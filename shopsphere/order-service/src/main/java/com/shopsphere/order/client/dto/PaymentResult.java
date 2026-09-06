package com.shopsphere.order.client.dto;

import java.math.BigDecimal;

/**
 * Order Service's own view of a Payment Service payment result.
 */
public record PaymentResult(
    Long id,
    String paymentReference,
    Long orderId,
    Long userId,
    BigDecimal amount,
    String currency,
    String status,
    String failureReason
) {
    public boolean isSuccessful() {
        return "SUCCESS".equals(status);
    }
}
