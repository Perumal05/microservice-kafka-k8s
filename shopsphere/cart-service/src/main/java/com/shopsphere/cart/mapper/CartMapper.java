package com.shopsphere.cart.mapper;

import com.shopsphere.cart.dto.response.CartItemResponse;
import com.shopsphere.cart.dto.response.CartResponse;
import com.shopsphere.cart.model.entity.Cart;
import com.shopsphere.cart.model.entity.CartItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CartMapper {

    private final CartItemMapper cartItemMapper;

    public CartMapper(CartItemMapper cartItemMapper) {
        this.cartItemMapper = cartItemMapper;
    }

    public CartResponse toResponse(Cart cart, List<CartItem> items) {
        if (cart == null) return null;
        List<CartItemResponse> itemResponses = items.stream()
                .map(cartItemMapper::toResponse)
                .toList();
        BigDecimal totalAmount = itemResponses.stream()
                .map(CartItemResponse::totalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(
                cart.getId(),
                cart.getUserId(),
                cart.getStatus(),
                itemResponses,
                totalAmount,
                cart.getCreatedAt(),
                cart.getUpdatedAt()
        );
    }
}
