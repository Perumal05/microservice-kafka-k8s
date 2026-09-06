package com.shopsphere.inventory.event.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.inventory.event.InventoryReservationFailedEvent;
import com.shopsphere.inventory.event.InventoryReservedEvent;
import com.shopsphere.inventory.event.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes Inventory Service's domain events. Kept as a dedicated
 * component - never invoked directly from {@link com.shopsphere.inventory.event.consumer.InventoryEventConsumer}'s
 * business logic without going through this seam, mirroring the REST client
 * pattern already used elsewhere in the project.
 */
@Component
public class InventoryEventProducer {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public InventoryEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /** Keyed by orderId, like every other event in this workflow, so all events for one order stay in-order within a partition. */
    public void publishInventoryReserved(InventoryReservedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaTopics.INVENTORY_EVENTS, String.valueOf(event.orderId()), payload);
            log.info("Published InventoryReserved eventId={} orderId={} orderNumber={} correlationId={}",
                    event.eventId(), event.orderId(), event.orderNumber(), event.correlationId());
        } catch (Exception ex) {
            // Dual-write limitation (same as OrderEventProducer): the reservation rows are
            // already committed in the Inventory database by the time this runs. If the
            // publish fails, Payment Service never learns the reservation succeeded and the
            // order stalls in PAYMENT_PENDING/PENDING. Not solved until the Outbox stage.
            log.error("Failed to publish InventoryReserved for orderId={}: {}", event.orderId(), ex.getMessage(), ex);
        }
    }

    public void publishInventoryReservationFailed(InventoryReservationFailedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(KafkaTopics.INVENTORY_EVENTS, String.valueOf(event.orderId()), payload);
            log.info("Published InventoryReservationFailed eventId={} orderId={} orderNumber={} reason={} correlationId={}",
                    event.eventId(), event.orderId(), event.orderNumber(), event.reason(), event.correlationId());
        } catch (Exception ex) {
            log.error("Failed to publish InventoryReservationFailed for orderId={}: {}", event.orderId(), ex.getMessage(), ex);
        }
    }
}
