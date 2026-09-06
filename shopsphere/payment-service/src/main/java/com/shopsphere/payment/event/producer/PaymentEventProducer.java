package com.shopsphere.payment.event.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.payment.event.KafkaTopics;
import com.shopsphere.payment.event.PaymentFailedEvent;
import com.shopsphere.payment.event.PaymentSucceededEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes Payment Service's domain events. Kept as a dedicated component,
 * never invoked directly from a controller or Kafka listener's business
 * logic without going through this seam.
 */
@Component
public class PaymentEventProducer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public PaymentEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /** Keyed by orderId, consistent with every other event in this workflow. */
    public void publishPaymentSucceeded(PaymentSucceededEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaTopics.PAYMENT_EVENTS, String.valueOf(event.orderId()), payload);
            log.info("Published PaymentSucceeded eventId={} orderId={} orderNumber={} paymentId={} correlationId={}",
                    event.eventId(), event.orderId(), event.orderNumber(), event.paymentId(), event.correlationId());
        } catch (Exception ex) {
            // Dual-write limitation (same note as OrderEventProducer/InventoryEventProducer):
            // the payment row is already committed as SUCCESS in the Payment database by the
            // time this runs. If publishing fails here, Order Service never learns the
            // payment succeeded and the order stalls in PAYMENT_PENDING. Not solved until
            // the Outbox stage.
            log.error("Failed to publish PaymentSucceeded for orderId={}: {}", event.orderId(), ex.getMessage(), ex);
        }
    }

    public void publishPaymentFailed(PaymentFailedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaTopics.PAYMENT_EVENTS, String.valueOf(event.orderId()), payload);
            log.info("Published PaymentFailed eventId={} orderId={} orderNumber={} paymentId={} reason={} correlationId={}",
                    event.eventId(), event.orderId(), event.orderNumber(), event.paymentId(), event.reason(), event.correlationId());
        } catch (Exception ex) {
            log.error("Failed to publish PaymentFailed for orderId={}: {}", event.orderId(), ex.getMessage(), ex);
        }
    }
}
