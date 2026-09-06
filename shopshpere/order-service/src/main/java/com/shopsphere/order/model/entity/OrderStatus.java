package com.shopsphere.order.model.entity;

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PAYMENT_PENDING,
    PAID,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    FAILED
}
