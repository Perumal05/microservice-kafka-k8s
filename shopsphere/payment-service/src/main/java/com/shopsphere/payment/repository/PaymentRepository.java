package com.shopsphere.payment.repository;

import com.shopsphere.payment.model.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentReference(String paymentReference);
    List<Payment> findByOrderId(Long orderId);
    List<Payment> findByUserId(Long userId);
    boolean existsByPaymentReference(String paymentReference);
}
