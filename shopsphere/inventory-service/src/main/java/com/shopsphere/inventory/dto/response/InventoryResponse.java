package com.shopsphere.inventory.dto.response;

import java.time.Instant;

public record InventoryResponse(
    Long id,
    Long productId,
    String sku,
    Integer availableQuantity,
    Integer reservedQuantity,
    Integer reorderLevel,
    Integer salableQuantity,
    Instant createdAt,
    Instant updatedAt
) {}
