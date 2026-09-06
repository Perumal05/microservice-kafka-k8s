package com.shopsphere.order.event.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.order.event.KafkaTopics;
import com.shopsphere.order.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes domain events for Order Service. Kept as a dedicated component
 * (never injected into {@code OrderController} directly) so that Kafka
 * publishing stays behind a clean seam, exactly like the REST clients in
 * {@code com.shopsphere.order.client} do for outbound HTTP calls.
 * <p>
 * Events are serialized to a plain JSON string (not Spring Kafka's
 * {@code JsonSerializer} with type headers) specifically so that consumers
 * never need to know the producer's Java package or class name - they only
 * need to agree on the JSON shape and the {@code eventType} field.
 */
@Component
public class OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrderEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publishes an OrderCreated event keyed by the order ID, so that every
     * event for a given order is routed to the same Kafka partition and
     * therefore stays in order relative to other events for that same order.
     * Ordering is only guaranteed within a partition, never globally across
     * partitions/topics.
     */
    public void publishOrderCreated(OrderCreatedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            String key = String.valueOf(event.orderId());
            kafkaTemplate.send(KafkaTopics.ORDER_EVENTS, key, payload);
            log.info("Published OrderCreated eventId={} orderId={} orderNumber={} correlationId={}",
                    event.eventId(), event.orderId(), event.orderNumber(), event.correlationId());
        } catch (Exception ex) {
            // NOTE (dual-write limitation): the order row was already committed to the
            // Order Service database before this method runs. If publishing to Kafka
            // fails here, the order exists in the database but no OrderCreated event
            // was ever emitted, so Inventory/Notification never learn about it. This is
            // the classic "dual write" problem: a DB commit and a Kafka publish are two
            // separate systems and cannot be made atomic without the Outbox pattern,
            // which is intentionally NOT implemented yet (planned for a later stage).
            log.error("Failed to publish OrderCreated for orderId={} orderNumber={}: {}",
                    event.orderId(), event.orderNumber(), ex.getMessage(), ex);
        }
    }
}
