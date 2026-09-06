package com.shopsphere.order.exception;

/**
 * Thrown when a checkout fails after the order has already been persisted
 * as PENDING (i.e. inventory reservation or payment failed). By the time
 * this is thrown, any inventory reservations already taken have been
 * released and the order has been marked FAILED.
 * <p>
 * Mapped to HTTP 409 Conflict - the request was well-formed and the order
 * exists, but the checkout could not be completed.
 */
public class OrderCheckoutFailedException extends RuntimeException {

    private final Long orderId;
    private final String orderNumber;

    public OrderCheckoutFailedException(Long orderId, String orderNumber, String message) {
        super(message);
        this.orderId = orderId;
        this.orderNumber = orderNumber;
    }

    public OrderCheckoutFailedException(Long orderId, String orderNumber, String message, Throwable cause) {
        super(message, cause);
        this.orderId = orderId;
        this.orderNumber = orderNumber;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }
}
