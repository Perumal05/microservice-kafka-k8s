package com.shopsphere.cart.exception;

public class CartNotFoundException extends ResourceNotFoundException {
    public CartNotFoundException(Long userId) {
        super("No active cart found for user id: " + userId);
    }
}
