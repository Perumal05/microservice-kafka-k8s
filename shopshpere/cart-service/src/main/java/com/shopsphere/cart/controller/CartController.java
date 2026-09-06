package com.shopsphere.cart.controller;

import com.shopsphere.cart.dto.request.CreateCartItemRequest;
import com.shopsphere.cart.dto.request.UpdateCartItemRequest;
import com.shopsphere.cart.dto.response.CartResponse;
import com.shopsphere.cart.exception.MissingHeaderException;
import com.shopsphere.cart.service.CartItemService;
import com.shopsphere.cart.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final CartService cartService;
    private final CartItemService cartItemService;

    public CartController(CartService cartService, CartItemService cartItemService) {
        this.cartService = cartService;
        this.cartItemService = cartItemService;
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @RequestHeader(value = USER_ID_HEADER, required = false) Long userId) {
        validateUserId(userId);
        return ResponseEntity.ok(cartService.getOrCreateCart(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @RequestHeader(value = USER_ID_HEADER, required = false) Long userId,
            @Valid @RequestBody CreateCartItemRequest request) {
        validateUserId(userId);
        return ResponseEntity.ok(cartItemService.addItem(userId, request));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateItem(
            @RequestHeader(value = USER_ID_HEADER, required = false) Long userId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        validateUserId(userId);
        return ResponseEntity.ok(cartItemService.updateItem(userId, itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> removeItem(
            @RequestHeader(value = USER_ID_HEADER, required = false) Long userId,
            @PathVariable Long itemId) {
        validateUserId(userId);
        return ResponseEntity.ok(cartItemService.removeItem(userId, itemId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            @RequestHeader(value = USER_ID_HEADER, required = false) Long userId) {
        validateUserId(userId);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
    }

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new MissingHeaderException(USER_ID_HEADER);
        }
    }
}
