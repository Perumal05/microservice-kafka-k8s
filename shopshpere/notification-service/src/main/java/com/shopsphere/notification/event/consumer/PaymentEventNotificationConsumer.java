package com.shopsphere.notification.event.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.notification.dto.request.CreateNotificationRequest;
import com.shopsphere.notification.event.EventTypes;
import com.shopsphere.notification.event.KafkaTopics;
import com.shopsphere.notification.event.PaymentFailedEvent;
import com.shopsphere.notification.event.PaymentSucceededEvent;
import com.shopsphere.notification.model.entity.NotificationChannel;
import com.shopsphere.notification.model.entity.NotificationType;
import com.shopsphere.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link KafkaTopics#PAYMENT_EVENTS}, published by Payment
 * Service, and simulates a payment-result notification for both outcomes
 * via the existing {@link NotificationService}.
 */
@Component
public class PaymentEventNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventNotificationConsumer.class);

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    public PaymentEventNotificationConsumer(ObjectMapper objectMapper, NotificationService notificationService) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_EVENTS)
    public void onMessage(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String eventType = node.path("eventType").asText(null);

            if (EventTypes.PAYMENT_SUCCEEDED.equals(eventType)) {
                handleSucceeded(objectMapper.treeToValue(node, PaymentSucceededEvent.class));
            } else if (EventTypes.PAYMENT_FAILED.equals(eventType)) {
                handleFailed(objectMapper.treeToValue(node, PaymentFailedEvent.class));
            } else {
                log.warn("Ignoring unrecognized event on {}: eventType={}", KafkaTopics.PAYMENT_EVENTS, eventType);
            }
        } catch (Exception ex) {
            log.error("Failed to process message from {}: {}", KafkaTopics.PAYMENT_EVENTS, ex.getMessage(), ex);
        }
    }

    private void handleSucceeded(PaymentSucceededEvent event) {
        log.info("Payment successful notification | eventId={} orderId={} orderNumber={} paymentId={} userId={} correlationId={}",
                event.eventId(), event.orderId(), event.orderNumber(), event.paymentId(), event.userId(), event.correlationId());

        var request = new CreateNotificationRequest(
                event.userId(),
                NotificationType.PAYMENT_SUCCESS,
                NotificationChannel.EMAIL,
                syntheticRecipient(event.userId()),
                "Payment received for order " + event.orderNumber(),
                "Your payment of " + event.amount() + " " + event.currency() + " for order " + event.orderNumber() + " was successful.",
                false
        );
        var created = notificationService.createNotification(request);
        notificationService.sendNotification(created.id(), false);
    }

    private void handleFailed(PaymentFailedEvent event) {
        log.info("Payment failed notification | eventId={} orderId={} orderNumber={} paymentId={} userId={} reason={} correlationId={}",
                event.eventId(), event.orderId(), event.orderNumber(), event.paymentId(), event.userId(), event.reason(), event.correlationId());

        var request = new CreateNotificationRequest(
                event.userId(),
                NotificationType.PAYMENT_FAILED,
                NotificationChannel.EMAIL,
                syntheticRecipient(event.userId()),
                "Payment failed for order " + event.orderNumber(),
                "We could not process payment for order " + event.orderNumber() + ": " + event.reason(),
                false
        );
        var created = notificationService.createNotification(request);
        notificationService.sendNotification(created.id(), false);
    }

    private String syntheticRecipient(Long userId) {
        return "user-" + userId + "@shopsphere.local";
    }
}
