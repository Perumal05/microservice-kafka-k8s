package com.shopsphere.payment.service;

import com.shopsphere.payment.exception.InvalidPaymentStateException;
import com.shopsphere.payment.model.entity.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentStatusTransitionValidatorTest {

    private PaymentStatusTransitionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PaymentStatusTransitionValidator();
    }

    @Test
    void validateTransition_ValidTransitions_ShouldNotThrowException() {
        assertDoesNotThrow(() -> validator.validateTransition(PaymentStatus.PENDING, PaymentStatus.PROCESSING));
        assertDoesNotThrow(() -> validator.validateTransition(PaymentStatus.PENDING, PaymentStatus.CANCELLED));
        assertDoesNotThrow(() -> validator.validateTransition(PaymentStatus.PROCESSING, PaymentStatus.SUCCESS));
        assertDoesNotThrow(() -> validator.validateTransition(PaymentStatus.PROCESSING, PaymentStatus.FAILED));
        assertDoesNotThrow(() -> validator.validateTransition(PaymentStatus.SUCCESS, PaymentStatus.REFUNDED));
        assertDoesNotThrow(() -> validator.validateTransition(PaymentStatus.FAILED, PaymentStatus.CANCELLED));
    }

    @Test
    void validateTransition_InvalidTransitions_ShouldThrowInvalidPaymentStateException() {
        assertThrows(InvalidPaymentStateException.class, () -> validator.validateTransition(PaymentStatus.PENDING, PaymentStatus.SUCCESS));
        assertThrows(InvalidPaymentStateException.class, () -> validator.validateTransition(PaymentStatus.SUCCESS, PaymentStatus.PENDING));
        assertThrows(InvalidPaymentStateException.class, () -> validator.validateTransition(PaymentStatus.REFUNDED, PaymentStatus.SUCCESS));
        assertThrows(InvalidPaymentStateException.class, () -> validator.validateTransition(PaymentStatus.CANCELLED, PaymentStatus.PROCESSING));
    }

    @Test
    void validateRefund_SuccessPayment_ShouldNotThrowException() {
        assertDoesNotThrow(() -> validator.validateRefund(PaymentStatus.SUCCESS));
    }

    @Test
    void validateRefund_NonSuccessPayment_ShouldThrowInvalidPaymentStateException() {
        assertThrows(InvalidPaymentStateException.class, () -> validator.validateRefund(PaymentStatus.PENDING));
        assertThrows(InvalidPaymentStateException.class, () -> validator.validateRefund(PaymentStatus.FAILED));
        assertThrows(InvalidPaymentStateException.class, () -> validator.validateRefund(PaymentStatus.CANCELLED));
        assertThrows(InvalidPaymentStateException.class, () -> validator.validateRefund(PaymentStatus.REFUNDED));
    }
}
