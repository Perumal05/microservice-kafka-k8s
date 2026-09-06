package com.shopsphere.inventory.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Published by Inventory Service to {@link KafkaTopics#INVENTORY_EVENTS}
 * once every item in an order has been successfully reserved.
 * <p>
 * Carries the order's payment context (userId/currency/totalAmount/
 * paymentMethod) forward from the consumed {@link OrderCreatedEvent} so
 * that Payment Service - which consumes this event, not OrderCreated
 * directly - has everything it needs to process payment without an
 * additional lookup.
 */
public record InventoryReservedEvent(
        String eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String correlationId,

        Long orderId,
        String orderNumber,
        Long userId,
        String currency,
        BigDecimal totalAmount,
        String paymentMethod,
        List<ReservationInfo> reservations
) {
    public record ReservationInfo(Long productId, Long reservationId, Integer quantity) {
    }

    public static InventoryReservedEvent of(String correlationId, Long orderId, String orderNumber, Long userId,
                                             String currency, BigDecimal totalAmount, String paymentMethod,
                                             List<ReservationInfo> reservations) {
        return new InventoryReservedEvent(
                UUID.randomUUID().toString(),
                EventTypes.INVENTORY_RESERVED,
                1,
                Instant.now(),
                correlationId,
                orderId,
                orderNumber,
                userId,
                currency,
                totalAmount,
                paymentMethod,
                reservations
        );
    }
}
