package com.shopsphere.order.event;

import java.time.Instant;
import java.util.List;

/**
 * Order Service's own copy of the InventoryReserved event contract,
 * published by Inventory Service to {@link KafkaTopics#INVENTORY_EVENTS}.
 * <p>
 * This is intentionally a separate class from Inventory Service's producer-
 * side type - each service owns its own copy of a shared event shape so
 * that consumers never depend on the producer's Java package/classpath.
 * Only the fields Order Service actually needs to react are included.
 */
public record InventoryReservedEvent(
        String eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String correlationId,

        Long orderId,
        String orderNumber,
        List<ReservationInfo> reservations
) {
    public record ReservationInfo(Long productId, Long reservationId, Integer quantity) {
    }
}
