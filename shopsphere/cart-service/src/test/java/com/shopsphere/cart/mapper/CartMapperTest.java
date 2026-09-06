package com.shopsphere.cart.mapper;

import com.shopsphere.cart.dto.response.CartItemResponse;
import com.shopsphere.cart.dto.response.CartResponse;
import com.shopsphere.cart.model.entity.Cart;
import com.shopsphere.cart.model.entity.CartItem;
import com.shopsphere.cart.model.entity.CartStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CartMapperTest {

    private CartItemMapper cartItemMapper;
    private CartMapper cartMapper;

    @BeforeEach
    void setUp() {
        cartItemMapper = new CartItemMapper();
        cartMapper = new CartMapper(cartItemMapper);
    }

    // ── CartItemMapper ──────────────────────────────────────────────

    @Test
    void cartItemMapper_toResponse_ShouldMapFieldsAndComputeTotalPrice() {
        Instant now = Instant.now();
        CartItem item = CartItem.builder()
                .id(1L).cartId(10L).productId(5L)
                .quantity(3).unitPrice(new BigDecimal("100.00"))
                .createdAt(now).updatedAt(now).build();

        CartItemResponse response = cartItemMapper.toResponse(item);

        assertNotNull(response);
        assertEquals(1L, response.itemId());
        assertEquals(5L, response.productId());
        assertEquals(3, response.quantity());
        assertEquals(new BigDecimal("100.00"), response.unitPrice());
        assertEquals(new BigDecimal("300.00"), response.totalPrice());
    }

    @Test
    void cartItemMapper_toResponse_NullItem_ShouldReturnNull() {
        assertNull(cartItemMapper.toResponse(null));
    }

    // ── CartMapper ──────────────────────────────────────────────────

    @Test
    void cartMapper_toResponse_ShouldAggregateTotalAmountAndMapItems() {
        Instant now = Instant.now();
        Cart cart = Cart.builder()
                .id(10L).userId(1L).status(CartStatus.ACTIVE)
                .createdAt(now).updatedAt(now).build();

        CartItem item1 = CartItem.builder()
                .id(1L).cartId(10L).productId(5L)
                .quantity(2).unitPrice(new BigDecimal("50.00"))
                .createdAt(now).updatedAt(now).build();

        CartItem item2 = CartItem.builder()
                .id(2L).cartId(10L).productId(6L)
                .quantity(1).unitPrice(new BigDecimal("75.00"))
                .createdAt(now).updatedAt(now).build();

        CartResponse response = cartMapper.toResponse(cart, List.of(item1, item2));

        assertNotNull(response);
        assertEquals(10L, response.cartId());
        assertEquals(1L, response.userId());
        assertEquals(CartStatus.ACTIVE, response.status());
        assertEquals(2, response.items().size());
        // totalAmount = (2*50) + (1*75) = 175
        assertEquals(new BigDecimal("175.00"), response.totalAmount());
    }

    @Test
    void cartMapper_toResponse_EmptyItems_ShouldReturnZeroTotal() {
        Instant now = Instant.now();
        Cart cart = Cart.builder()
                .id(10L).userId(1L).status(CartStatus.ACTIVE)
                .createdAt(now).updatedAt(now).build();

        CartResponse response = cartMapper.toResponse(cart, List.of());

        assertNotNull(response);
        assertTrue(response.items().isEmpty());
        assertEquals(BigDecimal.ZERO, response.totalAmount());
    }

    @Test
    void cartMapper_toResponse_NullCart_ShouldReturnNull() {
        assertNull(cartMapper.toResponse(null, List.of()));
    }
}
