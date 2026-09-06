package com.shopsphere.product.dto.request;

import com.shopsphere.product.model.entity.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Set;

public record UpdateProductRequest(
    @NotBlank(message = "Product name is required")
    String name,

    String description,

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    BigDecimal price,

    @NotBlank(message = "Currency is required")
    String currency,

    ProductStatus status,

    @NotEmpty(message = "Product must belong to at least one category")
    Set<Long> categoryIds
) {}
