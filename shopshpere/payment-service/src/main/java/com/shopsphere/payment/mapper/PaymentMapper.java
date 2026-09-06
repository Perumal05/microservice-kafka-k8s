package com.shopsphere.payment.mapper;

import com.shopsphere.payment.dto.request.CreatePaymentRequest;
import com.shopsphere.payment.dto.response.PaymentResponse;
import com.shopsphere.payment.model.entity.Payment;
import com.shopsphere.payment.model.entity.PaymentStatus;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public Payment toEntity(CreatePaymentRequest request, String paymentReference) {
        if (request == null) {
            return null;
        }
        return Payment.builder()
                .paymentReference(paymentReference)
                .orderId(request.orderId())
                .userId(request.userId())
                .amount(request.amount())
                .currency(request.currency())
                .paymentMethod(request.paymentMethod())
                .status(PaymentStatus.PENDING)
                .build();
    }

    public PaymentResponse toResponse(Payment payment) {
        if (payment == null) {
            return null;
        }
        return new PaymentResponse(
                payment.getId(),
                payment.getPaymentReference(),
                payment.getOrderId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getPaymentMethod(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
