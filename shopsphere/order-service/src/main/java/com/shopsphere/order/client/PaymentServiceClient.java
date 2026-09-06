package com.shopsphere.order.client;

import com.shopsphere.order.client.dto.PaymentResult;
import com.shopsphere.order.model.entity.PaymentMethod;

import java.math.BigDecimal;

/**
 * Outbound client boundary for the Payment Service.
 * <p>
 * The Payment Service uses a two-step create-then-process flow, and always
 * responds with HTTP 200 - the payment outcome (SUCCESS/FAILED) is carried
 * in the response body, not the HTTP status. Callers must inspect
 * {@link PaymentResult#isSuccessful()} rather than relying on the HTTP
 * status code to determine whether the payment succeeded.
 */
public interface PaymentServiceClient {

    /**
     * Initializes a payment intent (PENDING) for the given order.
     */
    PaymentResult createPayment(Long orderId, Long userId, BigDecimal amount, String currency, PaymentMethod paymentMethod);

    /**
     * Processes a previously created payment. The returned result's status
     * reflects SUCCESS or FAILED - both are normal (HTTP 200) outcomes.
     */
    PaymentResult processPayment(Long paymentId);
}
