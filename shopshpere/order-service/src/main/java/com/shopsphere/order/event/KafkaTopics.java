package com.shopsphere.order.event;

/**
 * Kafka topic names used across the ShopSphere event-driven workflow.
 * <p>
 * Topic strategy (Stage 4/5): one topic per domain/service boundary, not one
 * topic per event type. Each topic may carry more than one event type
 * (distinguished by the {@code eventType} field in the JSON payload) -
 * consumers branch on that field rather than relying on topic == event type.
 */
public final class KafkaTopics {

    /** Produced by Order Service. Carries: OrderCreated. */
    public static final String ORDER_EVENTS = "shopsphere.order.events";

    /** Produced by Inventory Service. Carries: InventoryReserved, InventoryReservationFailed. */
    public static final String INVENTORY_EVENTS = "shopsphere.inventory.events";

    /** Produced by Payment Service. Carries: PaymentSucceeded, PaymentFailed. */
    public static final String PAYMENT_EVENTS = "shopsphere.payment.events";

    private KafkaTopics() {
    }
}
