package com.shopsphere.product.dto.response;

import com.shopsphere.product.model.entity.CategoryStatus;

import java.time.Instant;

public record CategoryResponse(
    Long id,
    String name,
    String description,
    CategoryStatus status,
    Instant createdAt,
    Instant updatedAt
) {}
