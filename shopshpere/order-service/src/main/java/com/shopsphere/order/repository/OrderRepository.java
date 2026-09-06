package com.shopsphere.order.repository;

import com.shopsphere.order.model.entity.Order;
import com.shopsphere.order.model.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    boolean existsByOrderNumber(String orderNumber);
    Page<Order> findByUserId(Long userId, Pageable pageable);
    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);
}
