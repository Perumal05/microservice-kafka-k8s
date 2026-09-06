package com.shopsphere.payment.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by Payment Service to {@link KafkaTopics#PAYMENT_EVENTS} when a
 * payment fails (business decline or simulated failure/timeout). Carries a
 * failure reason/code only - never sensitive payment information.
 */
public record PaymentFailedEvent(
        String eventId,
        String eventType,
        int eventVersion,
        Instant occurredAt,
        String correlationId,

        Long orderId,
        String orderNumber,
        Long userId,
        Long paymentId,
        String reason
) {
    public static PaymentFailedEvent of(String correlationId, Long orderId, String orderNumber, Long userId,
                                         Long paymentId, String reason) {
        return new PaymentFailedEvent(
                UUID.randomUUID().toString(), EventTypes.PAYMENT_FAILED, 1, Instant.now(), correlationId,
                orderId, orderNumber, userId, paymentId, reason);
    }
}
