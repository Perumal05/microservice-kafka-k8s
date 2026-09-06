package com.shopsphere.order.service;

import com.shopsphere.order.exception.InvalidOrderStateException;
import com.shopsphere.order.model.entity.OrderStatus;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class OrderStatusTransitionValidator {

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
        OrderStatus.PENDING, EnumSet.of(OrderStatus.PAYMENT_PENDING, OrderStatus.CONFIRMED, OrderStatus.CANCELLED, OrderStatus.FAILED),
        OrderStatus.PAYMENT_PENDING, EnumSet.of(OrderStatus.PAID, OrderStatus.CANCELLED, OrderStatus.FAILED),
        OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
        OrderStatus.PAID, EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
        OrderStatus.PROCESSING, EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
        OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED),
        OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class),
        OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class),
        OrderStatus.FAILED, EnumSet.noneOf(OrderStatus.class)
    );

    public void validateTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
        if (currentStatus == targetStatus) {
            return; // No-op transition
        }

        Set<OrderStatus> allowedTargets = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowedTargets.contains(targetStatus)) {
            throw new InvalidOrderStateException(
                String.format("Cannot transition order status from '%s' to '%s'", currentStatus, targetStatus)
            );
        }
    }

    public void validateCancellation(OrderStatus currentStatus) {
        if (currentStatus == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException("Order is already cancelled");
        }
        if (currentStatus == OrderStatus.SHIPPED || currentStatus == OrderStatus.DELIVERED) {
            throw new InvalidOrderStateException(
                String.format("Cannot cancel order in status '%s'", currentStatus)
            );
        }
        if (currentStatus == OrderStatus.FAILED) {
            throw new InvalidOrderStateException("Cannot cancel a failed order");
        }
        validateTransition(currentStatus, OrderStatus.CANCELLED);
    }
}
