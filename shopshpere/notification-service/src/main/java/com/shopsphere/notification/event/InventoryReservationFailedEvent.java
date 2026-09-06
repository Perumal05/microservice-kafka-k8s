package com.shopsphere.notification.event;

import java.time.Instant;

/**
 * Notification Service's own copy of the InventoryReservationFailed event
 * contract, published by Inventory Service to {@link KafkaTopics#INVENTORY_EVENTS}.
 */
public record InventoryReservationFailedEvent(
        String eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String correlationId,

        Long orderId,
        String orderNumber,
        String reason
) {
}
