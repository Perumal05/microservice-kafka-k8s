package com.shopsphere.order.event.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.order.event.EventTypes;
import com.shopsphere.order.event.InventoryReservationFailedEvent;
import com.shopsphere.order.event.InventoryReservedEvent;
import com.shopsphere.order.event.KafkaTopics;
import com.shopsphere.order.exception.ResourceNotFoundException;
import com.shopsphere.order.model.entity.OrderStatus;
import com.shopsphere.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link KafkaTopics#INVENTORY_EVENTS}, published by Inventory
 * Service, and updates the corresponding order's status.
 * <p>
 * The topic carries two possible event types (InventoryReserved and
 * InventoryReservationFailed) - this listener inspects the {@code eventType}
 * field before deciding how to parse the payload, rather than assuming a
 * single fixed shape for every message on the topic.
 * <p>
 * Kept intentionally thin: parse, log, delegate to {@link OrderService}.
 * All actual status-transition business logic lives in
 * {@code OrderServiceImpl}/{@code OrderStatusTransitionValidator}.
 */
@Component
public class InventoryEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    public InventoryEventConsumer(ObjectMapper objectMapper, OrderService orderService) {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
    }

    @KafkaListener(topics = KafkaTopics.INVENTORY_EVENTS)
    public void onMessage(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String eventType = node.path("eventType").asText(null);

            if (EventTypes.INVENTORY_RESERVED.equals(eventType)) {
                handleReserved(objectMapper.treeToValue(node, InventoryReservedEvent.class));
            } else if (EventTypes.INVENTORY_RESERVATION_FAILED.equals(eventType)) {
                handleFailed(objectMapper.treeToValue(node, InventoryReservationFailedEvent.class));
            } else {
                log.warn("Ignoring unrecognized event on {}: eventType={}", KafkaTopics.INVENTORY_EVENTS, eventType);
            }
        } catch (Exception ex) {
            // Deserialization/processing failures are logged, not swallowed. No retry/DLQ
            // yet (planned for a later resilience stage) - see Stage 4/5 documentation for
            // the at-least-once delivery limitation this implies.
            log.error("Failed to process message from {}: {}", KafkaTopics.INVENTORY_EVENTS, ex.getMessage(), ex);
        }
    }

    private void handleReserved(InventoryReservedEvent event) {
        log.info("Received InventoryReserved eventId={} orderId={} orderNumber={} correlationId={}",
                event.eventId(), event.orderId(), event.orderNumber(), event.correlationId());
        try {
            orderService.updateOrderStatus(event.orderId(), OrderStatus.PAYMENT_PENDING);
            log.info("Order {} moved to PAYMENT_PENDING (correlationId={})", event.orderNumber(), event.correlationId());
        } catch (ResourceNotFoundException ex) {
            log.warn("InventoryReserved received for unknown orderId={}: {}", event.orderId(), ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Failed to apply InventoryReserved for orderId={}: {}", event.orderId(), ex.getMessage(), ex);
        }
    }

    private void handleFailed(InventoryReservationFailedEvent event) {
        log.info("Received InventoryReservationFailed eventId={} orderId={} orderNumber={} reason={} correlationId={}",
                event.eventId(), event.orderId(), event.orderNumber(), event.reason(), event.correlationId());
        try {
            orderService.updateOrderStatus(event.orderId(), OrderStatus.FAILED);
            log.info("Order {} marked FAILED due to inventory reservation failure (correlationId={})",
                    event.orderNumber(), event.correlationId());
        } catch (ResourceNotFoundException ex) {
            log.warn("InventoryReservationFailed received for unknown orderId={}: {}", event.orderId(), ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Failed to apply InventoryReservationFailed for orderId={}: {}", event.orderId(), ex.getMessage(), ex);
        }
    }
}
