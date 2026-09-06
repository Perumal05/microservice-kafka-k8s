package com.shopsphere.cart.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record CartItemResponse(
    Long itemId,
    Long productId,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal totalPrice,
    Instant createdAt,
    Instant updatedAt
) {}
