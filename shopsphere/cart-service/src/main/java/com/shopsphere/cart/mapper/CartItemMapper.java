package com.shopsphere.cart.mapper;

import com.shopsphere.cart.dto.response.CartItemResponse;
import com.shopsphere.cart.model.entity.CartItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CartItemMapper {

    public CartItemResponse toResponse(CartItem item) {
        if (item == null) return null;
        BigDecimal totalPrice = item.getUnitPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemResponse(
                item.getId(),
                item.getProductId(),
                item.getQuantity(),
                item.getUnitPrice(),
                totalPrice,
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
