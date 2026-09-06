package com.shopsphere.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateInventoryRequest(
    @NotNull(message = "Product ID is required")
    Long productId,

    @NotBlank(message = "SKU is required")
    String sku,

    @NotNull(message = "Available quantity is required")
    @Min(value = 0, message = "Available quantity cannot be negative")
    Integer availableQuantity,

    @Min(value = 0, message = "Reorder level cannot be negative")
    Integer reorderLevel
) {}
