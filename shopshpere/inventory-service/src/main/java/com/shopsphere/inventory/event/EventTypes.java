package com.shopsphere.inventory.event;

/**
 * String values used in the {@code eventType} field of every event JSON
 * payload. Consumers branch on these values, not on Java class names.
 */
public final class EventTypes {

    public static final String ORDER_CREATED = "OrderCreated";
    public static final String INVENTORY_RESERVED = "InventoryReserved";
    public static final String INVENTORY_RESERVATION_FAILED = "InventoryReservationFailed";

    private EventTypes() {
    }
}
