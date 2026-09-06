package com.shopsphere.notification.event;

import java.time.Instant;

/**
 * Notification Service's own copy of the PaymentFailed event contract,
 * published by Payment Service to {@link KafkaTopics#PAYMENT_EVENTS}.
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
}
