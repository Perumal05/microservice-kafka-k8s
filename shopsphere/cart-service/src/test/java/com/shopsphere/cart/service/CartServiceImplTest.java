package com.shopsphere.cart.service;

import com.shopsphere.cart.dto.request.CreateCartItemRequest;
import com.shopsphere.cart.dto.request.UpdateCartItemRequest;
import com.shopsphere.cart.dto.response.CartResponse;
import com.shopsphere.cart.exception.BadRequestException;
import com.shopsphere.cart.exception.CartItemNotFoundException;
import com.shopsphere.cart.mapper.CartItemMapper;
import com.shopsphere.cart.mapper.CartMapper;
import com.shopsphere.cart.model.entity.Cart;
import com.shopsphere.cart.model.entity.CartItem;
import com.shopsphere.cart.model.entity.CartStatus;
import com.shopsphere.cart.repository.CartItemRepository;
import com.shopsphere.cart.repository.CartRepository;
import com.shopsphere.cart.service.impl.CartItemServiceImpl;
import com.shopsphere.cart.service.impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CartServiceImplTest {

    private CartRepository cartRepository;
    private CartItemRepository cartItemRepository;
    private CartMapper cartMapper;
    private CartServiceImpl cartService;
    private CartItemServiceImpl cartItemService;

    private final Instant NOW = Instant.now();

    @BeforeEach
    void setUp() {
        cartRepository = mock(CartRepository.class);
        cartItemRepository = mock(CartItemRepository.class);
        CartItemMapper cartItemMapper = new CartItemMapper();
        cartMapper = new CartMapper(cartItemMapper);
        cartService = new CartServiceImpl(cartRepository, cartItemRepository, cartMapper);
        cartItemService = new CartItemServiceImpl(cartRepository, cartItemRepository, cartMapper);
    }

    // Helper builders
    private Cart activeCart(Long cartId, Long userId) {
        return Cart.builder().id(cartId).userId(userId).status(CartStatus.ACTIVE)
                .createdAt(NOW).updatedAt(NOW).build();
    }

    private CartItem item(Long id, Long cartId, Long productId, int qty, String price) {
        return CartItem.builder().id(id).cartId(cartId).productId(productId)
                .quantity(qty).unitPrice(new BigDecimal(price))
                .createdAt(NOW).updatedAt(NOW).build();
    }

    // ── CartServiceImpl ─────────────────────────────────────────────

    @Test
    void getOrCreateCart_ExistingActiveCart_ShouldReturnExistingCart() {
        Cart cart = activeCart(1L, 10L);
        when(cartRepository.findByUserIdAndStatus(10L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of());

        CartResponse response = cartService.getOrCreateCart(10L);

        assertEquals(1L, response.cartId());
        assertEquals(10L, response.userId());
        verify(cartRepository, never()).save(any());
    }

    @Test
    void getOrCreateCart_NoActiveCart_ShouldCreateNewCart() {
        Cart newCart = activeCart(2L, 10L);
        when(cartRepository.findByUserIdAndStatus(10L, CartStatus.ACTIVE)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(newCart);
        when(cartItemRepository.findByCartId(2L)).thenReturn(List.of());

        CartResponse response = cartService.getOrCreateCart(10L);

        assertEquals(2L, response.cartId());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void clearCart_ActiveCartExists_ShouldDeleteItemsAndAbandonCart() {
        Cart cart = activeCart(1L, 10L);
        when(cartRepository.findByUserIdAndStatus(10L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));

        cartService.clearCart(10L);

        verify(cartItemRepository).deleteAllByCartId(1L);
        verify(cartRepository).save(argThat(c -> c.getStatus() == CartStatus.ABANDONED));
    }

    @Test
    void clearCart_NoActiveCart_ShouldDoNothing() {
        when(cartRepository.findByUserIdAndStatus(10L, CartStatus.ACTIVE)).thenReturn(Optional.empty());

        cartService.clearCart(10L);

        verify(cartItemRepository, never()).deleteAllByCartId(any());
        verify(cartRepository, never()).save(any());
    }

    // ── CartItemServiceImpl ─────────────────────────────────────────

    @Test
    void addItem_NewProduct_ShouldCreateCartItemAndReturnCartResponse() {
        Cart cart = activeCart(1L, 10L);
        CartItem saved = item(5L, 1L, 100L, 2, "50.00");

        when(cartRepository.findByUserIdAndStatus(10L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(1L, 100L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(saved);
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(saved));

        CreateCartItemRequest request = new CreateCartItemRequest(100L, 2, new BigDecimal("50.00"));
        CartResponse response = cartItemService.addItem(10L, request);

        assertNotNull(response);
        assertEquals(1, response.items().size());
        assertEquals(new BigDecimal("100.00"), response.totalAmount());
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void addItem_ExistingProduct_ShouldAccumulateQuantity() {
        Cart cart = activeCart(1L, 10L);
        CartItem existing = item(5L, 1L, 100L, 2, "50.00");
        CartItem updated = item(5L, 1L, 100L, 5, "50.00"); // 2 + 3

        when(cartRepository.findByUserIdAndStatus(10L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(1L, 100L)).thenReturn(Optional.of(existing));
        when(cartItemRepository.save(existing)).thenReturn(updated);
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(updated));

        CreateCartItemRequest request = new CreateCartItemRequest(100L, 3, new BigDecimal("50.00"));
        CartResponse response = cartItemService.addItem(10L, request);

        assertEquals(5, response.items().get(0).quantity());
        verify(cartItemRepository).save(existing);
    }

    @Test
    void updateItem_ValidOwnership_ShouldUpdateQuantity() {
        Cart cart = activeCart(1L, 10L);
        CartItem existingItem = item(5L, 1L, 100L, 2, "50.00");
        CartItem updatedItem = item(5L, 1L, 100L, 7, "50.00");

        when(cartRepository.findByUserIdAndStatus(10L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(5L)).thenReturn(Optional.of(existingItem));
        when(cartItemRepository.save(existingItem)).thenReturn(updatedItem);
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(updatedItem));

        UpdateCartItemRequest request = new UpdateCartItemRequest(7);
        CartResponse response = cartItemService.updateItem(10L, 5L, request);

        assertEquals(7, response.items().get(0).quantity());
    }

    @Test
    void updateItem_ItemNotFound_ShouldThrowCartItemNotFoundException() {
        Cart cart = activeCart(1L, 10L);
        when(cartRepository.findByUserIdAndStatus(10L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CartItemNotFoundException.class,
                () -> cartItemService.updateItem(10L, 99L, new UpdateCartItemRequest(3)));
    }

    @Test
    void updateItem_ItemBelongsToDifferentCart_ShouldThrowBadRequestException() {
        Cart cart = activeCart(1L, 10L);
        // This item belongs to cartId=999, not cartId=1
        CartItem foreignItem = item(5L, 999L, 100L, 2, "50.00");

        when(cartRepository.findByUserIdAndStatus(10L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(5L)).thenReturn(Optional.of(foreignItem));

        assertThrows(BadRequestException.class,
                () -> cartItemService.updateItem(10L, 5L, new UpdateCartItemRequest(5)));
    }

    @Test
    void removeItem_ValidOwnership_ShouldDeleteItemAndReturnUpdatedCart() {
        Cart cart = activeCart(1L, 10L);
        CartItem existingItem = item(5L, 1L, 100L, 2, "50.00");

        when(cartRepository.findByUserIdAndStatus(10L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(5L)).thenReturn(Optional.of(existingItem));
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of()); // empty after deletion

        CartResponse response = cartItemService.removeItem(10L, 5L);

        verify(cartItemRepository).delete(existingItem);
        assertTrue(response.items().isEmpty());
        assertEquals(BigDecimal.ZERO, response.totalAmount());
    }

    @Test
    void removeItem_ItemNotFound_ShouldThrowCartItemNotFoundException() {
        Cart cart = activeCart(1L, 10L);
        when(cartRepository.findByUserIdAndStatus(10L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CartItemNotFoundException.class,
                () -> cartItemService.removeItem(10L, 99L));
    }

    @Test
    void removeItem_ItemBelongsToDifferentCart_ShouldThrowBadRequestException() {
        Cart cart = activeCart(1L, 10L);
        CartItem foreignItem = item(5L, 999L, 100L, 2, "50.00");

        when(cartRepository.findByUserIdAndStatus(10L, CartStatus.ACTIVE)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findById(5L)).thenReturn(Optional.of(foreignItem));

        assertThrows(BadRequestException.class,
                () -> cartItemService.removeItem(10L, 5L));
    }
}
