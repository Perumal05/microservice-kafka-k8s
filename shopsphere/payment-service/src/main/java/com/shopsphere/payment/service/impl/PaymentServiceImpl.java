package com.shopsphere.payment.service.impl;

import com.shopsphere.payment.dto.request.CreatePaymentRequest;
import com.shopsphere.payment.dto.request.ProcessPaymentRequest;
import com.shopsphere.payment.dto.response.PaymentResponse;
import com.shopsphere.payment.exception.ResourceNotFoundException;
import com.shopsphere.payment.mapper.PaymentMapper;
import com.shopsphere.payment.model.entity.Payment;
import com.shopsphere.payment.model.entity.PaymentStatus;
import com.shopsphere.payment.model.entity.SimulationMode;
import com.shopsphere.payment.repository.PaymentRepository;
import com.shopsphere.payment.service.PaymentService;
import com.shopsphere.payment.service.PaymentStatusTransitionValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentStatusTransitionValidator transitionValidator;
    private final long simulationTimeoutMs;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            PaymentMapper paymentMapper,
            PaymentStatusTransitionValidator transitionValidator,
            @Value("${payment.simulation.timeout-ms:2000}") long simulationTimeoutMs) {
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
        this.transitionValidator = transitionValidator;
        this.simulationTimeoutMs = simulationTimeoutMs;
    }

    @Override
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        String paymentRef = generatePaymentReference();
        Payment payment = paymentMapper.toEntity(request, paymentRef);
        Payment savedPayment = paymentRepository.save(payment);
        return paymentMapper.toResponse(savedPayment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));
        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByReference(String paymentReference) {
        Payment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with reference: " + paymentReference));
        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId).stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Override
    public PaymentResponse processPayment(Long paymentId, ProcessPaymentRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        transitionValidator.validateTransition(payment.getStatus(), PaymentStatus.PROCESSING);
        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);

        SimulationMode mode = (request != null && request.simulationMode() != null)
                ? request.simulationMode()
                : SimulationMode.SUCCESS;

        switch (mode) {
            case SUCCESS -> {
                transitionValidator.validateTransition(PaymentStatus.PROCESSING, PaymentStatus.SUCCESS);
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setFailureReason(null);
            }
            case FAILURE -> {
                transitionValidator.validateTransition(PaymentStatus.PROCESSING, PaymentStatus.FAILED);
                payment.setStatus(PaymentStatus.FAILED);
                String reason = (request != null && request.failureReason() != null && !request.failureReason().isBlank())
                        ? request.failureReason()
                        : "Simulated payment processing failure";
                payment.setFailureReason(reason);
            }
            case TIMEOUT -> {
                try {
                    Thread.sleep(simulationTimeoutMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                transitionValidator.validateTransition(PaymentStatus.PROCESSING, PaymentStatus.FAILED);
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Simulated payment processing timeout");
            }
        }

        Payment updatedPayment = paymentRepository.save(payment);
        return paymentMapper.toResponse(updatedPayment);
    }

    @Override
    public PaymentResponse refundPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        transitionValidator.validateRefund(payment.getStatus());
        transitionValidator.validateTransition(payment.getStatus(), PaymentStatus.REFUNDED);

        payment.setStatus(PaymentStatus.REFUNDED);
        Payment refundedPayment = paymentRepository.save(payment);
        return paymentMapper.toResponse(refundedPayment);
    }

    @Override
    public PaymentResponse cancelPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));

        transitionValidator.validateTransition(payment.getStatus(), PaymentStatus.CANCELLED);

        payment.setStatus(PaymentStatus.CANCELLED);
        Payment cancelledPayment = paymentRepository.save(payment);
        return paymentMapper.toResponse(cancelledPayment);
    }

    private String generatePaymentReference() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "PAY-" + datePrefix + "-" + randomSuffix;
    }
}
