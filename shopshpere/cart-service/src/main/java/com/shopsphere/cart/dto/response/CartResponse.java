package com.shopsphere.cart.dto.response;

import com.shopsphere.cart.model.entity.CartStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CartResponse(
    Long cartId,
    Long userId,
    CartStatus status,
    List<CartItemResponse> items,
    BigDecimal totalAmount,
    Instant createdAt,
    Instant updatedAt
) {}
