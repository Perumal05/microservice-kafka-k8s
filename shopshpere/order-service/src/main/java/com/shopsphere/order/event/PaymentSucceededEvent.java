package com.shopsphere.order.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Order Service's own copy of the PaymentSucceeded event contract,
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
        Long paymentId,
        BigDecimal amount,
        String currency
) {
}
