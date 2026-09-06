package com.shopsphere.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateInventoryRequest(
    @NotNull(message = "Available quantity is required")
    @Min(value = 0, message = "Available quantity cannot be negative")
    Integer availableQuantity,

    @Min(value = 0, message = "Reorder level cannot be negative")
    Integer reorderLevel
) {}
