package com.shopsphere.cart.service.impl;

import com.shopsphere.cart.dto.response.CartResponse;
import com.shopsphere.cart.mapper.CartMapper;
import com.shopsphere.cart.model.entity.Cart;
import com.shopsphere.cart.model.entity.CartItem;
import com.shopsphere.cart.model.entity.CartStatus;
import com.shopsphere.cart.repository.CartItemRepository;
import com.shopsphere.cart.repository.CartRepository;
import com.shopsphere.cart.service.CartService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartMapper cartMapper;

    public CartServiceImpl(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           CartMapper cartMapper) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.cartMapper = cartMapper;
    }

    @Override
    public CartResponse getOrCreateCart(Long userId) {
        Cart cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseGet(() -> cartRepository.save(
                        Cart.builder().userId(userId).status(CartStatus.ACTIVE).build()
                ));
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        return cartMapper.toResponse(cart, items);
    }

    @Override
    public void clearCart(Long userId) {
        cartRepository.findByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .ifPresent(cart -> {
                    cartItemRepository.deleteAllByCartId(cart.getId());
                    cart.setStatus(CartStatus.ABANDONED);
                    cartRepository.save(cart);
                });
    }
}
