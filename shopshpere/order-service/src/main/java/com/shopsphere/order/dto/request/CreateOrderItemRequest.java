package com.shopsphere.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for specifying order items during order creation.
 * <p>
 * The client supplies only the product identity and the desired quantity.
 * Authoritative product data (SKU, name, current price) is resolved
 * server-side via the Product Service so that price/SKU/name can never be
 * supplied or manipulated by the client.
 */
public record CreateOrderItemRequest(
    @NotNull(message = "Product ID is required")
    Long productId,

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    Integer quantity
) {}
