package com.shopsphere.order.client.dto;

/**
 * Order Service's own view of an Inventory Service reservation result.
 */
public record InventoryReservationResult(
    Long reservationId,
    Long inventoryId,
    Long orderId,
    Integer quantity,
    String status
) {}
