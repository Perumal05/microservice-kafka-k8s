package com.shopsphere.order.service;

import com.shopsphere.order.exception.InvalidOrderStateException;
import com.shopsphere.order.model.entity.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStatusTransitionValidatorTest {

    private OrderStatusTransitionValidator validator;

    @BeforeEach
    void setUp() {
        validator = new OrderStatusTransitionValidator();
    }

    @Test
    void validateTransition_ValidTransitions_ShouldNotThrowException() {
        assertDoesNotThrow(() -> validator.validateTransition(OrderStatus.PENDING, OrderStatus.CONFIRMED));
        assertDoesNotThrow(() -> validator.validateTransition(OrderStatus.PENDING, OrderStatus.PAYMENT_PENDING));
        assertDoesNotThrow(() -> validator.validateTransition(OrderStatus.PENDING, OrderStatus.CANCELLED));
        assertDoesNotThrow(() -> validator.validateTransition(OrderStatus.PAYMENT_PENDING, OrderStatus.PAID));
        assertDoesNotThrow(() -> validator.validateTransition(OrderStatus.PAID, OrderStatus.PROCESSING));
        assertDoesNotThrow(() -> validator.validateTransition(OrderStatus.PROCESSING, OrderStatus.SHIPPED));
        assertDoesNotThrow(() -> validator.validateTransition(OrderStatus.SHIPPED, OrderStatus.DELIVERED));
    }

    @Test
    void validateTransition_InvalidTransitions_ShouldThrowInvalidOrderStateException() {
        assertThrows(InvalidOrderStateException.class, () -> validator.validateTransition(OrderStatus.PENDING, OrderStatus.DELIVERED));
        assertThrows(InvalidOrderStateException.class, () -> validator.validateTransition(OrderStatus.SHIPPED, OrderStatus.PROCESSING));
        assertThrows(InvalidOrderStateException.class, () -> validator.validateTransition(OrderStatus.DELIVERED, OrderStatus.PENDING));
        assertThrows(InvalidOrderStateException.class, () -> validator.validateTransition(OrderStatus.CANCELLED, OrderStatus.PAID));
    }

    @Test
    void validateCancellation_ValidCancellationStates_ShouldNotThrowException() {
        assertDoesNotThrow(() -> validator.validateCancellation(OrderStatus.PENDING));
        assertDoesNotThrow(() -> validator.validateCancellation(OrderStatus.PAYMENT_PENDING));
        assertDoesNotThrow(() -> validator.validateCancellation(OrderStatus.CONFIRMED));
        assertDoesNotThrow(() -> validator.validateCancellation(OrderStatus.PAID));
        assertDoesNotThrow(() -> validator.validateCancellation(OrderStatus.PROCESSING));
    }

    @Test
    void validateCancellation_InvalidCancellationStates_ShouldThrowException() {
        assertThrows(InvalidOrderStateException.class, () -> validator.validateCancellation(OrderStatus.SHIPPED));
        assertThrows(InvalidOrderStateException.class, () -> validator.validateCancellation(OrderStatus.DELIVERED));
        assertThrows(InvalidOrderStateException.class, () -> validator.validateCancellation(OrderStatus.CANCELLED));
        assertThrows(InvalidOrderStateException.class, () -> validator.validateCancellation(OrderStatus.FAILED));
    }
}
