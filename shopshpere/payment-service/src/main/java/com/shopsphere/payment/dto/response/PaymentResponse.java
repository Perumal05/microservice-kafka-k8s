package com.shopsphere.payment.dto.response;

import com.shopsphere.payment.model.entity.PaymentMethod;
import com.shopsphere.payment.model.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
    Long id,
    String paymentReference,
    Long orderId,
    Long userId,
    BigDecimal amount,
    String currency,
    PaymentStatus status,
    PaymentMethod paymentMethod,
    String failureReason,
    Instant createdAt,
    Instant updatedAt
) {}
