package com.shopsphere.order.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderItemResponse(
    Long id,
    Long productId,
    String productSku,
    String productName,
    Integer quantity,
    BigDecimal unitPrice,
    BigDecimal totalPrice,
    Instant createdAt
) {}
