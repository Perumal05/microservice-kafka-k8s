package com.shopsphere.inventory.event;

/**
 * Kafka topic names used across the ShopSphere event-driven workflow.
 * See order-service's copy of this class for the full topic-strategy note.
 */
public final class KafkaTopics {

    /** Consumed from Order Service. Carries: OrderCreated. */
    public static final String ORDER_EVENTS = "shopsphere.order.events";

    /** Produced by Inventory Service. Carries: InventoryReserved, InventoryReservationFailed. */
    public static final String INVENTORY_EVENTS = "shopsphere.inventory.events";

    private KafkaTopics() {
    }
}
