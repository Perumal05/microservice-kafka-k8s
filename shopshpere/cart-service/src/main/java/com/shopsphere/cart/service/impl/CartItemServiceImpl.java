package com.shopsphere.cart.service.impl;

import com.shopsphere.cart.dto.request.CreateCartItemRequest;
import com.shopsphere.cart.dto.request.UpdateCartItemRequest;
import com.shopsphere.cart.dto.response.CartResponse;
import com.shopsphere.cart.exception.BadRequestException;
import com.shopsphere.cart.exception.CartItemNotFoundException;
import com.shopsphere.cart.mapper.CartMapper;
import com.shopsphere.cart.model.entity.Cart;
import com.shopsphere.cart.model.entity.CartItem;
import com.shopsphere.cart.model.entity.CartStatus;
import com.shopsphere.cart.repository.CartItemRepository;
import com.shopsphere.cart.repository.CartRepository;
import com.shopsphere.cart.service.CartItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CartItemServiceImpl implements CartItemService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;

    public CartItemServiceImpl(CartRepository cartRepository,
                               CartItemRepository cartItemRepository,
                               CartMapper cartMapper) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.cartMapper = cartMapper;
    }

    @Override
    public CartResponse addItem(Long userId, CreateCartItemRequest request) {
        Cart cart = getOrCreateActiveCart(userId);

        // If product already in cart, accumulate quantity
        CartItem item = cartItemRepository.findByCartIdAndProductId(cart.getId(), request.productId())
                .map(existing -> {
                    existing.setQuantity(existing.getQuantity() + request.quantity());
                    return cartItemRepository.save(existing);
                })
                .orElseGet(() -> cartItemRepository.save(
                        CartItem.builder()
                                .cartId(cart.getId())
                                .productId(request.productId())
                                .quantity(request.quantity())
                                .unitPrice(request.unitPrice())
                                .build()
                ));

        return buildCartResponse(cart);
    }

    @Override
    public CartResponse updateItem(Long userId, Long itemId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateActiveCart(userId);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new CartItemNotFoundException(itemId));

        // Ownership: item must belong to this user's active cart
        if (!item.getCartId().equals(cart.getId())) {
            throw new BadRequestException("Cart item with id: " + itemId + " does not belong to your cart");
        }

        item.setQuantity(request.quantity());
        cartItemRepository.save(item);

        return buildCartResponse(cart);
    }

    @Override
    public CartResponse removeItem(Long userId, Long itemId) {
        Cart cart = getOrCreateActiveCart(userId);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new CartItemNotFoundException(itemId));

        if (!item.getCartId().equals(cart.getId())) {
            throw new BadRequestException("Cart item with id: " + itemId + " does not belong to your cart");
        }

        cartItemRepository.delete(item);

        return buildCartResponse(cart);
    }

    private Cart getOrCreateActiveCart(Long userId) {
        return cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseGet(() -> cartRepository.save(
                        Cart.builder().userId(userId).status(CartStatus.ACTIVE).build()
                ));
    }

    private CartResponse buildCartResponse(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        return cartMapper.toResponse(cart, items);
    }
}
