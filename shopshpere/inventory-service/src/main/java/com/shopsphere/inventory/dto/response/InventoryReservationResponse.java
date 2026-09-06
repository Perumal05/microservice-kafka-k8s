package com.shopsphere.inventory.dto.response;

import com.shopsphere.inventory.model.entity.ReservationStatus;

import java.time.Instant;

public record InventoryReservationResponse(
    Long id,
    Long inventoryId,
    Long orderId,
    Integer quantity,
    ReservationStatus status,
    Instant createdAt,
    Instant updatedAt
) {}
