package com.shopsphere.notification.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Notification Service's own copy of the PaymentSucceeded event contract,
 * published by Payment Service to {@link KafkaTopics#PAYMENT_EVENTS}.
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
