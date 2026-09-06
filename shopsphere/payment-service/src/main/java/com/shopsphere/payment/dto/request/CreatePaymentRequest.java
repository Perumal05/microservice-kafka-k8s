package com.shopsphere.payment.dto.request;

import com.shopsphere.payment.model.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreatePaymentRequest(
    @NotNull(message = "Order ID is required")
    Long orderId,

    @NotNull(message = "User ID is required")
    Long userId,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    BigDecimal amount,

    @NotBlank(message = "Currency is required")
    String currency,

    @NotNull(message = "Payment method is required")
    PaymentMethod paymentMethod
) {}
