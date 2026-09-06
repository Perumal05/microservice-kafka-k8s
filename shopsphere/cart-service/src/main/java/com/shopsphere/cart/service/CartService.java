package com.shopsphere.cart.service;

import com.shopsphere.cart.dto.response.CartResponse;

public interface CartService {
    CartResponse getOrCreateCart(Long userId);
    void clearCart(Long userId);
}
