package com.shopsphere.inventory.dto.response;

public record AvailabilityResponse(
    Long productId,
    String sku,
    Integer availableQuantity,
    Integer reservedQuantity,
    Integer salableQuantity,
    boolean isAvailable
) {}
