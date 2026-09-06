package com.shopsphere.payment.service;

import com.shopsphere.payment.exception.InvalidPaymentStateException;
import com.shopsphere.payment.model.entity.PaymentStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class PaymentStatusTransitionValidator {

    private static final Map<PaymentStatus, Set<PaymentStatus>> VALID_TRANSITIONS = Map.of(
        PaymentStatus.PENDING, EnumSet.of(PaymentStatus.PROCESSING, PaymentStatus.CANCELLED),
        PaymentStatus.PROCESSING, EnumSet.of(PaymentStatus.SUCCESS, PaymentStatus.FAILED),
        PaymentStatus.SUCCESS, EnumSet.of(PaymentStatus.REFUNDED),
        PaymentStatus.FAILED, EnumSet.of(PaymentStatus.CANCELLED),
        PaymentStatus.REFUNDED, EnumSet.noneOf(PaymentStatus.class),
        PaymentStatus.CANCELLED, EnumSet.noneOf(PaymentStatus.class)
    );

    public void validateTransition(PaymentStatus currentStatus, PaymentStatus targetStatus) {
        if (currentStatus == targetStatus) {
            return;
        }

        Set<PaymentStatus> allowedTargets = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowedTargets.contains(targetStatus)) {
            throw new InvalidPaymentStateException(
                String.format("Cannot transition payment status from '%s' to '%s'", currentStatus, targetStatus)
            );
        }
    }

    public void validateRefund(PaymentStatus currentStatus) {
        if (currentStatus != PaymentStatus.SUCCESS) {
            throw new InvalidPaymentStateException(
                String.format("Cannot refund payment with status '%s'. Only SUCCESSFUL payments can be refunded.", currentStatus)
            );
        }
    }
}
