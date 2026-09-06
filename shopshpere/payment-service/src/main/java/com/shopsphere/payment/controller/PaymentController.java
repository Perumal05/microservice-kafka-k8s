package com.shopsphere.payment.controller;

import com.shopsphere.payment.dto.request.CreatePaymentRequest;
import com.shopsphere.payment.dto.request.ProcessPaymentRequest;
import com.shopsphere.payment.dto.response.PaymentResponse;
import com.shopsphere.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payment Controller", description = "APIs for managing payment processing and simulations")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @Operation(summary = "Initialize a new payment intent (PENDING status)")
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by internal ID")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable Long paymentId) {
        PaymentResponse response = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reference/{paymentReference}")
    @Operation(summary = "Get payment by payment reference number")
    public ResponseEntity<PaymentResponse> getPaymentByReference(@PathVariable String paymentReference) {
        PaymentResponse response = paymentService.getPaymentByReference(paymentReference);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get all payments associated with an order ID")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByOrderId(@PathVariable Long orderId) {
        List<PaymentResponse> responses = paymentService.getPaymentsByOrderId(orderId);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{paymentId}/process")
    @Operation(summary = "Process payment with simulated outcome (SUCCESS, FAILURE, TIMEOUT)")
    public ResponseEntity<PaymentResponse> processPayment(
            @PathVariable Long paymentId,
            @RequestBody(required = false) ProcessPaymentRequest request) {
        PaymentResponse response = paymentService.processPayment(paymentId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "Refund a previously successful payment")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable Long paymentId) {
        PaymentResponse response = paymentService.refundPayment(paymentId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{paymentId}/cancel")
    @Operation(summary = "Cancel a pending or failed payment intent")
    public ResponseEntity<PaymentResponse> cancelPayment(@PathVariable Long paymentId) {
        PaymentResponse response = paymentService.cancelPayment(paymentId);
        return ResponseEntity.ok(response);
    }
}
