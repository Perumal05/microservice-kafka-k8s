package com.shopsphere.cart.event;

/**
 * Kafka topic names used across the ShopSphere event-driven workflow.
 * Cart Service is a pure consumer here - it publishes no events of its own
 * in this stage.
 */
public final class KafkaTopics {

    /** Produced by Payment Service. Carries: PaymentSucceeded, PaymentFailed. */
    public static final String PAYMENT_EVENTS = "shopsphere.payment.events";

    private KafkaTopics() {
    }
}
