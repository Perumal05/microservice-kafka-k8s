package com.shopsphere.cart.event.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.cart.event.EventTypes;
import com.shopsphere.cart.event.KafkaTopics;
import com.shopsphere.cart.event.PaymentSucceededEvent;
import com.shopsphere.cart.service.CartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link KafkaTopics#PAYMENT_EVENTS}, published by Payment
 * Service, and clears the paying user's cart once payment succeeds.
 * <p>
 * This replaces Stage 3's "Order Service calls Cart Service's REST endpoint
 * as a best-effort step after payment succeeds" with the equivalent
 * asynchronous shape: Cart Service now reacts to {@code PaymentSucceeded}
 * itself, using its own {@link CartService} business logic directly - never
 * by calling its own REST endpoint from this listener.
 * <p>
 * {@code PaymentFailed} is intentionally ignored: an unsuccessful payment
 * must not clear the cart (the customer should still see their items to
 * retry checkout).
 * <p>
 * No explicit duplicate-processing guard is needed here, unlike
 * {@code InventoryEventConsumer}/{@code PaymentEventConsumer} elsewhere in
 * this system: {@link CartService#clearCart(Long)} only acts on a cart that
 * is still {@code ACTIVE} and is a no-op otherwise (see
 * {@code CartServiceImpl}), so a redelivered {@code PaymentSucceeded}
 * message for an already-cleared cart simply does nothing the second time.
 */
@Component
public class PaymentEventCartConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventCartConsumer.class);

    private final ObjectMapper objectMapper;
    private final CartService cartService;

    public PaymentEventCartConsumer(ObjectMapper objectMapper, CartService cartService) {
        this.objectMapper = objectMapper;
        this.cartService = cartService;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_EVENTS)
    public void onMessage(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            String eventType = node.path("eventType").asText(null);

            if (EventTypes.PAYMENT_SUCCEEDED.equals(eventType)) {
                handlePaymentSucceeded(objectMapper.treeToValue(node, PaymentSucceededEvent.class));
            } else if (EventTypes.PAYMENT_FAILED.equals(eventType)) {
                log.debug("Ignoring PaymentFailed on {} - a failed payment must not clear the cart", KafkaTopics.PAYMENT_EVENTS);
            } else {
                log.warn("Ignoring unrecognized event on {}: eventType={}", KafkaTopics.PAYMENT_EVENTS, eventType);
            }
        } catch (Exception ex) {
            // Logged, not swallowed. No retry/DLQ yet - see Stage 5 documentation. A missed
            // cart clear here is a minor, self-correcting inconvenience (the user's cart
            // just still shows items they already bought) rather than a business-critical
            // failure, so this is a reasonable place to accept the at-least-once/no-retry
            // limitation without extra safeguards.
            log.error("Failed to process message from {}: {}", KafkaTopics.PAYMENT_EVENTS, ex.getMessage(), ex);
        }
    }

    private void handlePaymentSucceeded(PaymentSucceededEvent event) {
        log.info("Received PaymentSucceeded eventId={} orderId={} orderNumber={} userId={} correlationId={}",
                event.eventId(), event.orderId(), event.orderNumber(), event.userId(), event.correlationId());

        if (event.userId() == null) {
            log.warn("PaymentSucceeded for orderId={} has no userId - cannot clear a cart", event.orderId());
            return;
        }

        cartService.clearCart(event.userId());
        log.info("Cart cleared for userId={} following successful payment for orderId={}", event.userId(), event.orderId());
    }
}
