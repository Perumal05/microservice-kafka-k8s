package com.shopsphere.notification.event.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.notification.event.EventTypes;
import com.shopsphere.notification.event.InventoryReservationFailedEvent;
import com.shopsphere.notification.event.KafkaTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link KafkaTopics#INVENTORY_EVENTS}, published by Inventory
 * Service, and logs a simulated "inventory failure notification" for
 * {@code InventoryReservationFailed}. {@code InventoryReserved} is ignored -
 * it's an intermediate step, not something the customer needs to be
 * notified about.
 * <p>
 * Unlike {@link OrderEventNotificationConsumer} and
 * {@link PaymentEventNotificationConsumer}, this listener does NOT create a
 * persisted {@code Notification} via {@link com.shopsphere.notification.service.NotificationService}:
 * the existing {@code NotificationType} enum has no value representing an
 * inventory-specific failure, and introducing one purely for this log line
 * was judged out of scope for Stage 5's minimal-footprint principle. This
 * is logged only, matching Stage 4's original "log, don't persist" approach.
 */
@Component
public class InventoryEventNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventNotificationConsumer.class);

    private final ObjectMapper objectMapper;

    public InventoryEventNotificationConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.INVENTORY_EVENTS)
    public void onMessage(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String eventType = node.path("eventType").asText(null);

            if (EventTypes.INVENTORY_RESERVATION_FAILED.equals(eventType)) {
                handleReservationFailed(objectMapper.treeToValue(node, InventoryReservationFailedEvent.class));
            } else if (EventTypes.INVENTORY_RESERVED.equals(eventType)) {
                log.debug("Ignoring InventoryReserved on {} - not a customer-facing notification", KafkaTopics.INVENTORY_EVENTS);
            } else {
                log.warn("Ignoring unrecognized event on {}: eventType={}", KafkaTopics.INVENTORY_EVENTS, eventType);
            }
        } catch (Exception ex) {
            log.error("Failed to process message from {}: {}", KafkaTopics.INVENTORY_EVENTS, ex.getMessage(), ex);
        }
    }

    private void handleReservationFailed(InventoryReservationFailedEvent event) {
        log.info("Inventory failure notification | eventId={} orderId={} orderNumber={} reason={} correlationId={}",
                event.eventId(), event.orderId(), event.orderNumber(), event.reason(), event.correlationId());
    }
}
