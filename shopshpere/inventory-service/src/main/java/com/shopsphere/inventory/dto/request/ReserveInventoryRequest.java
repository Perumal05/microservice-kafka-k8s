package com.shopsphere.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReserveInventoryRequest(
    @NotNull(message = "Order ID is required")
    Long orderId,

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Reservation quantity must be at least 1")
    Integer quantity
) {}
