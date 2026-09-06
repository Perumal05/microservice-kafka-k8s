package com.shopsphere.payment.event.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.payment.dto.request.CreatePaymentRequest;
import com.shopsphere.payment.dto.response.PaymentResponse;
import com.shopsphere.payment.event.EventTypes;
import com.shopsphere.payment.event.InventoryReservedEvent;
import com.shopsphere.payment.event.KafkaTopics;
import com.shopsphere.payment.event.PaymentFailedEvent;
import com.shopsphere.payment.event.PaymentSucceededEvent;
import com.shopsphere.payment.event.producer.PaymentEventProducer;
import com.shopsphere.payment.model.entity.PaymentMethod;
import com.shopsphere.payment.model.entity.PaymentStatus;
import com.shopsphere.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Consumes {@link KafkaTopics#INVENTORY_EVENTS}, published by Inventory
 * Service, and processes payment for orders whose inventory has been
 * successfully reserved.
 * <p>
 * Per Stage 5 scope, this listener only acts on {@code InventoryReserved} -
 * {@code InventoryReservationFailed} messages on the same topic are ignored
 * here (Order Service reacts to those instead; Payment Service has nothing
 * to do for an order whose inventory reservation already failed).
 * <p>
 * Kept thin: parse -&gt; delegate to {@link PaymentService} -&gt; publish the
 * outcome via {@link PaymentEventProducer}. Payment processing rules
 * themselves are not duplicated here - they live in
 * {@code PaymentServiceImpl}, exactly as they did for the Stage 3 REST flow.
 */
@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final PaymentService paymentService;
    private final PaymentEventProducer paymentEventProducer;

    public PaymentEventConsumer(ObjectMapper objectMapper, PaymentService paymentService,
                                 PaymentEventProducer paymentEventProducer) {
        this.objectMapper = objectMapper;
        this.paymentService = paymentService;
        this.paymentEventProducer = paymentEventProducer;
    }

    @KafkaListener(topics = KafkaTopics.INVENTORY_EVENTS)
    public void onMessage(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String eventType = node.path("eventType").asText(null);

            if (EventTypes.INVENTORY_RESERVED.equals(eventType)) {
                handleInventoryReserved(objectMapper.treeToValue(node, InventoryReservedEvent.class));
            } else if (EventTypes.INVENTORY_RESERVATION_FAILED.equals(eventType)) {
                log.debug("Ignoring InventoryReservationFailed on {} - nothing for Payment Service to do", KafkaTopics.INVENTORY_EVENTS);
            } else {
                log.warn("Ignoring unrecognized event on {}: eventType={}", KafkaTopics.INVENTORY_EVENTS, eventType);
            }
        } catch (Exception ex) {
            // Logged, not swallowed. No retry/DLQ yet - see Stage 5 documentation for the
            // at-least-once delivery limitation and the minimal duplicate-processing guard
            // used below instead of a full idempotency framework.
            log.error("Failed to process message from {}: {}", KafkaTopics.INVENTORY_EVENTS, ex.getMessage(), ex);
        }
    }

    private void handleInventoryReserved(InventoryReservedEvent event) {
        log.info("Received InventoryReserved eventId={} orderId={} orderNumber={} correlationId={}",
                event.eventId(), event.orderId(), event.orderNumber(), event.correlationId());

        // Minimal duplicate-processing guard (NOT a full idempotency solution - see Stage 5
        // documentation). If a payment already exists for this order, this message has
        // already been handled (likely a Kafka redelivery) - skip rather than risk creating
        // a second payment for the same order.
        List<PaymentResponse> existing = paymentService.getPaymentsByOrderId(event.orderId());
        if (!existing.isEmpty()) {
            log.warn("Skipping InventoryReserved for orderId={}: {} payment(s) already exist (likely a redelivered message)",
                    event.orderId(), existing.size());
            return;
        }

        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(event.paymentMethod());
        } catch (IllegalArgumentException ex) {
            log.error("Unrecognized paymentMethod '{}' on orderId={} - cannot process payment", event.paymentMethod(), event.orderId());
            paymentEventProducer.publishPaymentFailed(PaymentFailedEvent.of(
                    event.correlationId(), event.orderId(), event.orderNumber(), event.userId(), null, "Unrecognized payment method"));
            return;
        }

        PaymentResponse payment = paymentService.createPayment(new CreatePaymentRequest(
                event.orderId(), event.userId(), event.totalAmount(), event.currency(), paymentMethod));

        // No simulation mode specified -> PaymentServiceImpl defaults to SUCCESS, matching
        // the same default behavior used by the Stage 3 REST flow (RestPaymentServiceClient).
        PaymentResponse processed = paymentService.processPayment(payment.id(), null);

        if (processed.status() == PaymentStatus.SUCCESS) {
            paymentEventProducer.publishPaymentSucceeded(PaymentSucceededEvent.of(
                    event.correlationId(), event.orderId(), event.orderNumber(), event.userId(),
                    processed.id(), processed.amount(), processed.currency()));
        } else {
            String reason = processed.failureReason() != null ? processed.failureReason() : "Payment was not successful";
            paymentEventProducer.publishPaymentFailed(PaymentFailedEvent.of(
                    event.correlationId(), event.orderId(), event.orderNumber(), event.userId(), processed.id(), reason));
        }
    }
}
