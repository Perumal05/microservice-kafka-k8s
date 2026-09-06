package com.shopsphere.order.dto.response;

import com.shopsphere.order.model.entity.OrderStatus;
import com.shopsphere.order.model.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
    Long id,
    String orderNumber,
    Long userId,
    OrderStatus status,
    String currency,
    PaymentMethod paymentMethod,
    BigDecimal subtotal,
    BigDecimal shippingAmount,
    BigDecimal taxAmount,
    BigDecimal discountAmount,
    BigDecimal totalAmount,
    ShippingAddressResponse shippingAddress,
    List<OrderItemResponse> items,
    Instant createdAt,
    Instant updatedAt
) {}
