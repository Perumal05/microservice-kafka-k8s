package com.shopsphere.order.model.entity;

/**
 * Payment method chosen at checkout time.
 * <p>
 * Mirrors the values accepted by the Payment Service's own
 * {@code PaymentMethod} enum. Kept as a separate type (rather than a shared
 * library) since each microservice owns its own domain model; the values
 * must simply stay in sync with the Payment Service contract.
 */
public enum PaymentMethod {
    CARD,
    UPI,
    NET_BANKING,
    WALLET
}
