package com.shopsphere.notification.event;

/**
 * Kafka topic names used across the ShopSphere event-driven workflow.
 * Notification Service is a pure consumer of all three - it publishes
 * nothing of its own in this stage.
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
