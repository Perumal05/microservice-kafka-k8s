package com.shopsphere.payment.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Published by Payment Service to {@link KafkaTopics#PAYMENT_EVENTS} once a
 * payment has been processed successfully.
 * <p>
 * Deliberately excludes any sensitive payment information (no card number,
 * CVV, or authentication credentials - Payment Service's own domain model
 * doesn't store those either, per the existing Stage 3 design).
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
    public static PaymentSucceededEvent of(String correlationId, Long orderId, String orderNumber, Long userId,
                                            Long paymentId, BigDecimal amount, String currency) {
        return new PaymentSucceededEvent(
                UUID.randomUUID().toString(), EventTypes.PAYMENT_SUCCEEDED, 1, Instant.now(), correlationId,
                orderId, orderNumber, userId, paymentId, amount, currency);
    }
}
