package com.shopsphere.cart.exception;

public class CartItemNotFoundException extends ResourceNotFoundException {
    public CartItemNotFoundException(Long itemId) {
        super("Cart item not found with id: " + itemId);
    }
}
