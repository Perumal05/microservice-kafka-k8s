package com.shopsphere.payment.service;

import com.shopsphere.payment.dto.request.CreatePaymentRequest;
import com.shopsphere.payment.dto.request.ProcessPaymentRequest;
import com.shopsphere.payment.dto.response.PaymentResponse;
import com.shopsphere.payment.exception.InvalidPaymentStateException;
import com.shopsphere.payment.exception.ResourceNotFoundException;
import com.shopsphere.payment.mapper.PaymentMapper;
import com.shopsphere.payment.model.entity.Payment;
import com.shopsphere.payment.model.entity.PaymentMethod;
import com.shopsphere.payment.model.entity.PaymentStatus;
import com.shopsphere.payment.model.entity.SimulationMode;
import com.shopsphere.payment.repository.PaymentRepository;
import com.shopsphere.payment.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentServiceImplTest {

    private PaymentRepository paymentRepository;
    private PaymentMapper paymentMapper;
    private PaymentStatusTransitionValidator transitionValidator;
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentRepository = mock(PaymentRepository.class);
        paymentMapper = new PaymentMapper();
        transitionValidator = new PaymentStatusTransitionValidator();
        paymentService = new PaymentServiceImpl(paymentRepository, paymentMapper, transitionValidator, 50L);
    }

    @Test
    void createPayment_Success() {
        CreatePaymentRequest request = new CreatePaymentRequest(100L, 50L, new BigDecimal("250.00"), "USD", PaymentMethod.CARD);

        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(1L);
            p.setCreatedAt(Instant.now());
            p.setUpdatedAt(Instant.now());
            return p;
        });

        PaymentResponse response = paymentService.createPayment(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(PaymentStatus.PENDING, response.status());
        assertTrue(response.paymentReference().startsWith("PAY-"));
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void getPaymentById_Success() {
        Payment payment = Payment.builder()
                .id(1L).paymentReference("PAY-123").orderId(100L).userId(50L).amount(new BigDecimal("100.00"))
                .status(PaymentStatus.PENDING).paymentMethod(PaymentMethod.CARD)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPaymentById(1L);

        assertNotNull(response);
        assertEquals("PAY-123", response.paymentReference());
    }

    @Test
    void getPaymentById_NotFound_ThrowsResourceNotFoundException() {
        when(paymentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.getPaymentById(99L));
    }

    @Test
    void processPayment_SimulateSuccess_ShouldTransitionToSuccess() {
        Payment payment = Payment.builder()
                .id(1L).status(PaymentStatus.PENDING).paymentMethod(PaymentMethod.CARD)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        ProcessPaymentRequest request = new ProcessPaymentRequest(SimulationMode.SUCCESS, null);
        PaymentResponse response = paymentService.processPayment(1L, request);

        assertEquals(PaymentStatus.SUCCESS, response.status());
        assertNull(response.failureReason());
    }

    @Test
    void processPayment_SimulateFailure_ShouldTransitionToFailed() {
        Payment payment = Payment.builder()
                .id(1L).status(PaymentStatus.PENDING).paymentMethod(PaymentMethod.CARD)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        ProcessPaymentRequest request = new ProcessPaymentRequest(SimulationMode.FAILURE, "Insufficient card funds");
        PaymentResponse response = paymentService.processPayment(1L, request);

        assertEquals(PaymentStatus.FAILED, response.status());
        assertEquals("Insufficient card funds", response.failureReason());
    }

    @Test
    void processPayment_SimulateTimeout_ShouldTransitionToFailedWithTimeoutReason() {
        Payment payment = Payment.builder()
                .id(1L).status(PaymentStatus.PENDING).paymentMethod(PaymentMethod.CARD)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        ProcessPaymentRequest request = new ProcessPaymentRequest(SimulationMode.TIMEOUT, null);
        PaymentResponse response = paymentService.processPayment(1L, request);

        assertEquals(PaymentStatus.FAILED, response.status());
        assertEquals("Simulated payment processing timeout", response.failureReason());
    }

    @Test
    void refundPayment_SuccessPayment_ShouldTransitionToRefunded() {
        Payment payment = Payment.builder()
                .id(1L).status(PaymentStatus.SUCCESS).paymentMethod(PaymentMethod.CARD)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.refundPayment(1L);

        assertEquals(PaymentStatus.REFUNDED, response.status());
    }

    @Test
    void refundPayment_FailedPayment_ShouldThrowInvalidPaymentStateException() {
        Payment payment = Payment.builder()
                .id(1L).status(PaymentStatus.FAILED).paymentMethod(PaymentMethod.CARD)
                .build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        assertThrows(InvalidPaymentStateException.class, () -> paymentService.refundPayment(1L));
    }

    @Test
    void cancelPayment_PendingPayment_ShouldTransitionToCancelled() {
        Payment payment = Payment.builder()
                .id(1L).status(PaymentStatus.PENDING).paymentMethod(PaymentMethod.CARD)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResponse response = paymentService.cancelPayment(1L);

        assertEquals(PaymentStatus.CANCELLED, response.status());
    }
}
