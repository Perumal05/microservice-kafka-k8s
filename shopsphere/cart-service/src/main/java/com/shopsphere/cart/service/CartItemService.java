package com.shopsphere.cart.service;

import com.shopsphere.cart.dto.request.CreateCartItemRequest;
import com.shopsphere.cart.dto.request.UpdateCartItemRequest;
import com.shopsphere.cart.dto.response.CartResponse;

public interface CartItemService {
    CartResponse addItem(Long userId, CreateCartItemRequest request);
    CartResponse updateItem(Long userId, Long itemId, UpdateCartItemRequest request);
    CartResponse removeItem(Long userId, Long itemId);
}
