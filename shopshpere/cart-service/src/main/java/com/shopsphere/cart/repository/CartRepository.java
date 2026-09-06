package com.shopsphere.cart.repository;

import com.shopsphere.cart.model.entity.Cart;
import com.shopsphere.cart.model.entity.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserIdAndStatus(Long userId, CartStatus status);
    boolean existsByUserIdAndStatus(Long userId, CartStatus status);
}
