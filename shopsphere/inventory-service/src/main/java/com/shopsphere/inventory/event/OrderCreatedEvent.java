package com.shopsphere.inventory.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Inventory Service's own copy of the OrderCreated event contract,
 * published by Order Service to {@link KafkaTopics#ORDER_EVENTS}.
 * <p>
 * This is a deliberately separate class from Order Service's producer-side
 * type - Inventory Service does not depend on Order Service's Java package.
 * It carries userId/currency/totalAmount/paymentMethod even though
 * Inventory Service itself doesn't need them for reservation, because they
 * must be forwarded into {@link InventoryReservedEvent} for Payment Service
 * to consume downstream.
 */
public record OrderCreatedEvent(
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
        List<Item> items
) {
    public record Item(Long productId, String sku, Integer quantity, BigDecimal unitPrice) {
    }
}
