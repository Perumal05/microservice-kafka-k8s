package com.shopsphere.product.dto.response;

import com.shopsphere.product.model.entity.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

public record ProductResponse(
    Long id,
    String sku,
    String name,
    String description,
    BigDecimal price,
    String currency,
    ProductStatus status,
    Instant createdAt,
    Instant updatedAt,
    Set<CategoryResponse> categories
) {}
