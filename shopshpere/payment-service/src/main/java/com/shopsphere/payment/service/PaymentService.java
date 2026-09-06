package com.shopsphere.payment.service;

import com.shopsphere.payment.dto.request.CreatePaymentRequest;
import com.shopsphere.payment.dto.request.ProcessPaymentRequest;
import com.shopsphere.payment.dto.response.PaymentResponse;

import java.util.List;

public interface PaymentService {
    PaymentResponse createPayment(CreatePaymentRequest request);
    PaymentResponse getPaymentById(Long paymentId);
    PaymentResponse getPaymentByReference(String paymentReference);
    List<PaymentResponse> getPaymentsByOrderId(Long orderId);
    PaymentResponse processPayment(Long paymentId, ProcessPaymentRequest request);
    PaymentResponse refundPayment(Long paymentId);
    PaymentResponse cancelPayment(Long paymentId);
}
