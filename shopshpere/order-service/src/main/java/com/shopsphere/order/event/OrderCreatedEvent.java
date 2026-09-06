package com.shopsphere.order.event;

import com.shopsphere.order.model.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Published by Order Service to {@link KafkaTopics#ORDER_EVENTS} once an
 * order has been successfully validated and persisted locally as PENDING.
 * <p>
 * This is a dedicated event contract - it is NOT the {@code Order} JPA
 * entity and NOT the {@code CreateOrderRequest} REST DTO. Only the fields
 * downstream consumers (Inventory Service, Notification Service) actually
 * need are included.
 */
public record OrderCreatedEvent(
        // --- event metadata ---
        String eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String correlationId,

        // --- order information ---
        Long orderId,
        String orderNumber,
        Long userId,
        String currency,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        List<OrderCreatedEventItem> items
) {
    public static OrderCreatedEvent of(String correlationId, Long orderId, String orderNumber, Long userId,
                                        String currency, BigDecimal totalAmount, PaymentMethod paymentMethod,
                                        List<OrderCreatedEventItem> items) {
        return new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                EventTypes.ORDER_CREATED,
                1,
                Instant.now(),
                correlationId,
                orderId,
                orderNumber,
                userId,
                currency,
                totalAmount,
                paymentMethod,
                items
        );
    }
}
