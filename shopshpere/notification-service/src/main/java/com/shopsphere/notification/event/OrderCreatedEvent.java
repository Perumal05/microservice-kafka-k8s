package com.shopsphere.notification.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Notification Service's own copy of the OrderCreated event contract,
 * published by Order Service to {@link KafkaTopics#ORDER_EVENTS}.
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
