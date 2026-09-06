package com.shopsphere.product.dto.request;

import com.shopsphere.product.model.entity.CategoryStatus;
import jakarta.validation.constraints.NotBlank;

public record UpdateCategoryRequest(
    @NotBlank(message = "Category name is required")
    String name,

    String description,

    CategoryStatus status
) {}
