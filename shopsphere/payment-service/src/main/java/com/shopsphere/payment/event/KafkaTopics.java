package com.shopsphere.payment.event;

/**
 * Kafka topic names used across the ShopSphere event-driven workflow.
 * See order-service's copy of this class for the full topic-strategy note.
 */
public final class KafkaTopics {

    /** Consumed from Inventory Service. Carries: InventoryReserved, InventoryReservationFailed. */
    public static final String INVENTORY_EVENTS = "shopsphere.inventory.events";

    /** Produced by Payment Service. Carries: PaymentSucceeded, PaymentFailed. */
    public static final String PAYMENT_EVENTS = "shopsphere.payment.events";

    private KafkaTopics() {
    }
}
