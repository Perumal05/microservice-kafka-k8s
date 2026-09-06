package com.shopsphere.notification.event.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.notification.dto.request.CreateNotificationRequest;
import com.shopsphere.notification.event.EventTypes;
import com.shopsphere.notification.event.KafkaTopics;
import com.shopsphere.notification.event.OrderCreatedEvent;
import com.shopsphere.notification.model.entity.NotificationChannel;
import com.shopsphere.notification.model.entity.NotificationType;
import com.shopsphere.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link KafkaTopics#ORDER_EVENTS}, published by Order Service.
 * <p>
 * Stage 4 introduced this listener purely to demonstrate producer -&gt; broker
 * -&gt; consumer (log only). Stage 5 extends it to also simulate a real
 * notification via the existing {@link NotificationService}, using its
 * existing (already-simulated) sender pipeline - still no real email/SMS
 * integration, exactly as before.
 * <p>
 * NOTE: Notification Service does not know a user's real email/phone (that
 * belongs to User Service, and calling it synchronously from this listener
 * just to resolve a recipient was judged an unnecessary new dependency for
 * this stage). A clearly-synthetic placeholder recipient is used instead -
 * see {@link #syntheticRecipient(Long)}.
 */
@Component
public class OrderEventNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventNotificationConsumer.class);

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    public OrderEventNotificationConsumer(ObjectMapper objectMapper, NotificationService notificationService) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = KafkaTopics.ORDER_EVENTS)
    public void onMessage(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String eventType = node.path("eventType").asText(null);

            if (EventTypes.ORDER_CREATED.equals(eventType)) {
                handleOrderCreated(objectMapper.treeToValue(node, OrderCreatedEvent.class));
            } else {
                log.warn("Ignoring unrecognized event on {}: eventType={}", KafkaTopics.ORDER_EVENTS, eventType);
            }
        } catch (Exception ex) {
            log.error("Failed to process message from {}: {}", KafkaTopics.ORDER_EVENTS, ex.getMessage(), ex);
        }
    }

    private void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Order created notification | eventId={} orderId={} orderNumber={} userId={} correlationId={}",
                event.eventId(), event.orderId(), event.orderNumber(), event.userId(), event.correlationId());

        var request = new CreateNotificationRequest(
                event.userId(),
                NotificationType.ORDER_CREATED,
                NotificationChannel.EMAIL,
                syntheticRecipient(event.userId()),
                "Your ShopSphere order " + event.orderNumber() + " was received",
                "We've received your order " + event.orderNumber() + " and are getting it ready.",
                false
        );
        var created = notificationService.createNotification(request);
        notificationService.sendNotification(created.id(), false);
    }

    private String syntheticRecipient(Long userId) {
        return "user-" + userId + "@shopsphere.local";
    }
}
