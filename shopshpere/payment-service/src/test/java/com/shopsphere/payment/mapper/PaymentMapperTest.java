package com.shopsphere.payment.mapper;

import com.shopsphere.payment.dto.request.CreatePaymentRequest;
import com.shopsphere.payment.dto.response.PaymentResponse;
import com.shopsphere.payment.model.entity.Payment;
import com.shopsphere.payment.model.entity.PaymentMethod;
import com.shopsphere.payment.model.entity.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PaymentMapperTest {

    private PaymentMapper paymentMapper;

    @BeforeEach
    void setUp() {
        paymentMapper = new PaymentMapper();
    }

    @Test
    void toEntity_ShouldMapCreatePaymentRequestToPayment() {
        CreatePaymentRequest request = new CreatePaymentRequest(
                100L, 50L, new BigDecimal("150.00"), "USD", PaymentMethod.CARD);

        Payment payment = paymentMapper.toEntity(request, "PAY-20260809-123456");

        assertNotNull(payment);
        assertEquals("PAY-20260809-123456", payment.getPaymentReference());
        assertEquals(100L, payment.getOrderId());
        assertEquals(50L, payment.getUserId());
        assertEquals(new BigDecimal("150.00"), payment.getAmount());
        assertEquals("USD", payment.getCurrency());
        assertEquals(PaymentMethod.CARD, payment.getPaymentMethod());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
    }

    @Test
    void toResponse_ShouldMapPaymentToPaymentResponse() {
        Instant now = Instant.now();
        Payment payment = Payment.builder()
                .id(1L)
                .paymentReference("PAY-20260809-123456")
                .orderId(100L)
                .userId(50L)
                .amount(new BigDecimal("150.00"))
                .currency("USD")
                .paymentMethod(PaymentMethod.UPI)
                .status(PaymentStatus.SUCCESS)
                .createdAt(now)
                .updatedAt(now)
                .build();

        PaymentResponse response = paymentMapper.toResponse(payment);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("PAY-20260809-123456", response.paymentReference());
        assertEquals(PaymentStatus.SUCCESS, response.status());
        assertEquals(PaymentMethod.UPI, response.paymentMethod());
    }
}
