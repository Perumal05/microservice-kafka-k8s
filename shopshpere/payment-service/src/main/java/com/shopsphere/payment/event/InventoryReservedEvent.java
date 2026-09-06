package com.shopsphere.payment.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Payment Service's own copy of the InventoryReserved event contract,
 * published by Inventory Service to {@link KafkaTopics#INVENTORY_EVENTS}.
 * <p>
 * This is the trigger for payment processing in Stage 5 - Payment Service
 * does not consume OrderCreated directly. The event forwards the order's
 * payment context (userId/currency/totalAmount/paymentMethod) that
 * Inventory Service itself forwarded from OrderCreated, so Payment Service
 * has everything it needs without an extra lookup.
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
}
