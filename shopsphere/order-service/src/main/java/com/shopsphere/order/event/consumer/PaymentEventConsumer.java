package com.shopsphere.order.event.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.order.event.EventTypes;
import com.shopsphere.order.event.KafkaTopics;
import com.shopsphere.order.event.PaymentFailedEvent;
import com.shopsphere.order.event.PaymentSucceededEvent;
import com.shopsphere.order.exception.ResourceNotFoundException;
import com.shopsphere.order.model.entity.OrderStatus;
import com.shopsphere.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link KafkaTopics#PAYMENT_EVENTS}, published by Payment
 * Service, and updates the corresponding order's final status.
 * <p>
 * Kept separate from {@link InventoryEventConsumer} so each listener class
 * has a single, clear responsibility rather than one large listener
 * handling every event type in the system.
 */
@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    public PaymentEventConsumer(ObjectMapper objectMapper, OrderService orderService) {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_EVENTS)
    public void onMessage(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String eventType = node.path("eventType").asText(null);

            if (EventTypes.PAYMENT_SUCCEEDED.equals(eventType)) {
                handleSucceeded(objectMapper.treeToValue(node, PaymentSucceededEvent.class));
            } else if (EventTypes.PAYMENT_FAILED.equals(eventType)) {
                handleFailed(objectMapper.treeToValue(node, PaymentFailedEvent.class));
            } else {
                log.warn("Ignoring unrecognized event on {}: eventType={}", KafkaTopics.PAYMENT_EVENTS, eventType);
            }
        } catch (Exception ex) {
            log.error("Failed to process message from {}: {}", KafkaTopics.PAYMENT_EVENTS, ex.getMessage(), ex);
        }
    }

    private void handleSucceeded(PaymentSucceededEvent event) {
        log.info("Received PaymentSucceeded eventId={} orderId={} orderNumber={} paymentId={} correlationId={}",
                event.eventId(), event.orderId(), event.orderNumber(), event.paymentId(), event.correlationId());
        try {
            orderService.updateOrderStatus(event.orderId(), OrderStatus.PAID);
            log.info("Order {} marked PAID (correlationId={})", event.orderNumber(), event.correlationId());
        } catch (ResourceNotFoundException ex) {
            log.warn("PaymentSucceeded received for unknown orderId={}: {}", event.orderId(), ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Failed to apply PaymentSucceeded for orderId={}: {}", event.orderId(), ex.getMessage(), ex);
        }
    }

    private void handleFailed(PaymentFailedEvent event) {
        log.info("Received PaymentFailed eventId={} orderId={} orderNumber={} reason={} correlationId={}",
                event.eventId(), event.orderId(), event.orderNumber(), event.reason(), event.correlationId());
        try {
            orderService.updateOrderStatus(event.orderId(), OrderStatus.FAILED);
            log.info("Order {} marked FAILED due to payment failure (correlationId={})",
                    event.orderNumber(), event.correlationId());
        } catch (ResourceNotFoundException ex) {
            log.warn("PaymentFailed received for unknown orderId={}: {}", event.orderId(), ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Failed to apply PaymentFailed for orderId={}: {}", event.orderId(), ex.getMessage(), ex);
        }
    }
}
