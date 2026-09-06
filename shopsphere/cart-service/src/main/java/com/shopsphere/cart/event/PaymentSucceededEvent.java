package com.shopsphere.cart.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Cart Service's own copy of the PaymentSucceeded event contract, published
 * by Payment Service to {@link KafkaTopics#PAYMENT_EVENTS}. Only
 * {@code userId} is actually used by this service - the rest of the fields
 * are kept so the record accurately mirrors the real payload shape (Jackson
 * ignores fields we don't map further, and Spring Boot's default
 * {@code ObjectMapper} tolerates unknown properties either way).
 */
public record PaymentSucceededEvent(
        String eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String correlationId,

        Long orderId,
        String orderNumber,
        Long userId,
        Long paymentId,
        BigDecimal amount,
        String currency
) {
}
