package com.shopsphere.inventory.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by Inventory Service to {@link KafkaTopics#INVENTORY_EVENTS}
 * when any item in an order could not be reserved (e.g. insufficient
 * stock). Per the existing business rule, a partial reservation is never
 * reported as success - if any item fails, the whole order's reservation is
 * considered failed and this event is published instead of
 * {@link InventoryReservedEvent}.
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
    public static InventoryReservationFailedEvent of(String correlationId, Long orderId, String orderNumber, String reason) {
        return new InventoryReservationFailedEvent(
                UUID.randomUUID().toString(),
                EventTypes.INVENTORY_RESERVATION_FAILED,
                1,
                Instant.now(),
                correlationId,
                orderId,
                orderNumber,
                reason
        );
    }
}
