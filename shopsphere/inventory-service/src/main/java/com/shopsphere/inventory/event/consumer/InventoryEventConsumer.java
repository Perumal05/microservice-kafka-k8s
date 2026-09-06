package com.shopsphere.inventory.event.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.inventory.dto.request.ReleaseInventoryRequest;
import com.shopsphere.inventory.dto.request.ReserveInventoryRequest;
import com.shopsphere.inventory.dto.response.InventoryReservationResponse;
import com.shopsphere.inventory.event.EventTypes;
import com.shopsphere.inventory.event.InventoryReservationFailedEvent;
import com.shopsphere.inventory.event.InventoryReservedEvent;
import com.shopsphere.inventory.event.KafkaTopics;
import com.shopsphere.inventory.event.OrderCreatedEvent;
import com.shopsphere.inventory.event.producer.InventoryEventProducer;
import com.shopsphere.inventory.exception.InsufficientInventoryException;
import com.shopsphere.inventory.exception.ResourceNotFoundException;
import com.shopsphere.inventory.service.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Consumes {@link KafkaTopics#ORDER_EVENTS}, published by Order Service, and
 * performs the actual inventory reservation for the order using Inventory
 * Service's own business logic and database - never by calling Inventory
 * Service's own REST endpoint from this listener.
 * <p>
 * Kept thin: parse -&gt; delegate to {@link ReservationService} -&gt; publish the
 * outcome via {@link InventoryEventProducer}. Reservation rules themselves
 * (pessimistic locking, salable-quantity check, etc.) are not duplicated
 * here - they live in {@code ReservationServiceImpl}, exactly as they did
 * for the Stage 3 REST flow.
 */
@Component
public class InventoryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final ReservationService reservationService;
    private final InventoryEventProducer inventoryEventProducer;

    public InventoryEventConsumer(ObjectMapper objectMapper, ReservationService reservationService,
                                   InventoryEventProducer inventoryEventProducer) {
        this.objectMapper = objectMapper;
        this.reservationService = reservationService;
        this.inventoryEventProducer = inventoryEventProducer;
    }

    @KafkaListener(topics = KafkaTopics.ORDER_EVENTS)
    public void onMessage(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String eventType = node.path("eventType").asText(null);

            if (EventTypes.ORDER_CREATED.equals(eventType)) {
                handleOrderCreated(objectMapper.treeToValue(node, OrderCreatedEvent.class));
            } else {
                log.warn("Ignoring unrecognized event on {}: eventType={}", KafkaTopics.ORDER_EVENTS, eventType);
            }
        } catch (Exception ex) {
            // Deserialization/processing failures are logged, not swallowed. No retry/DLQ
            // yet (planned for a later resilience stage) - Kafka's at-least-once delivery
            // means this message may be redelivered; see the minimal duplicate-processing
            // guard in handleOrderCreated() below.
            log.error("Failed to process message from {}: {}", KafkaTopics.ORDER_EVENTS, ex.getMessage(), ex);
        }
    }

    private void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreated eventId={} orderId={} orderNumber={} correlationId={}",
                event.eventId(), event.orderId(), event.orderNumber(), event.correlationId());

        // Minimal duplicate-processing guard (NOT a full idempotency solution - see Stage 5
        // documentation). Kafka consumers are at-least-once, so this same OrderCreated
        // message could be redelivered (e.g. after a consumer restart before offset commit).
        // If reservations already exist for this order, we've already handled it - skip
        // reprocessing rather than reserving stock twice for the same order.
        List<InventoryReservationResponse> existing = reservationService.getReservationsByOrderId(event.orderId());
        if (!existing.isEmpty()) {
            log.warn("Skipping OrderCreated for orderId={}: {} reservation(s) already exist (likely a redelivered message)",
                    event.orderId(), existing.size());
            return;
        }

        List<InventoryReservedEvent.ReservationInfo> reservations = new ArrayList<>();
        try {
            for (OrderCreatedEvent.Item item : event.items()) {
                InventoryReservationResponse reservation = reservationService.reserveInventory(
                        item.productId(), new ReserveInventoryRequest(event.orderId(), item.quantity()));
                reservations.add(new InventoryReservedEvent.ReservationInfo(
                        item.productId(), reservation.id(), item.quantity()));
            }
        } catch (InsufficientInventoryException | ResourceNotFoundException ex) {
            log.warn("Inventory reservation failed for orderId={} orderNumber={}: {}",
                    event.orderId(), event.orderNumber(), ex.getMessage());
            releaseAll(reservations);
            inventoryEventProducer.publishInventoryReservationFailed(
                    InventoryReservationFailedEvent.of(event.correlationId(), event.orderId(), event.orderNumber(), ex.getMessage()));
            return;
        }

        InventoryReservedEvent reservedEvent = InventoryReservedEvent.of(
                event.correlationId(), event.orderId(), event.orderNumber(), event.userId(),
                event.currency(), event.totalAmount(), event.paymentMethod(), reservations);
        inventoryEventProducer.publishInventoryReserved(reservedEvent);
    }

    /**
     * Releases every reservation already taken for this order before reporting overall
     * failure - the existing Stage 3 business rule (no partial reservations left behind
     * for a failed order) applies identically here, just triggered by a Kafka event
     * instead of a REST call.
     */
    private void releaseAll(List<InventoryReservedEvent.ReservationInfo> reservations) {
        for (InventoryReservedEvent.ReservationInfo reservation : reservations) {
            try {
                reservationService.releaseReservation(reservation.reservationId(), new ReleaseInventoryRequest("Reservation failed for another item in the same order"));
            } catch (RuntimeException ex) {
                log.error("Compensation failed: could not release reservationId={}: {}",
                        reservation.reservationId(), ex.getMessage(), ex);
            }
        }
    }
}
