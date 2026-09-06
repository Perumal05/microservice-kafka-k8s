package com.shopsphere.payment.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shopsphere.payment.dto.request.CreatePaymentRequest;
import com.shopsphere.payment.dto.response.PaymentResponse;
import com.shopsphere.payment.event.InventoryReservedEvent;
import com.shopsphere.payment.event.PaymentFailedEvent;
import com.shopsphere.payment.event.PaymentSucceededEvent;
import com.shopsphere.payment.event.producer.PaymentEventProducer;
import com.shopsphere.payment.model.entity.PaymentMethod;
import com.shopsphere.payment.model.entity.PaymentStatus;
import com.shopsphere.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PaymentEventConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private PaymentService paymentService;
    private PaymentEventProducer paymentEventProducer;
    private PaymentEventConsumer consumer;

    @BeforeEach
    void setUp() {
        paymentService = mock(PaymentService.class);
        paymentEventProducer = mock(PaymentEventProducer.class);
        consumer = new PaymentEventConsumer(objectMapper, paymentService, paymentEventProducer);

        when(paymentService.getPaymentsByOrderId(anyLong())).thenReturn(List.of());
    }

    private InventoryReservedEvent inventoryReservedEvent(Long orderId) {
        return InventoryReservedEvent.of("corr-1", orderId, "ORD-" + orderId, 100L, "USD",
                new BigDecimal("99.99"), "CARD", List.of(new InventoryReservedEvent.ReservationInfo(10L, 900L, 1)));
    }

    private PaymentResponse paymentResponse(Long id, PaymentStatus status, String failureReason) {
        return new PaymentResponse(id, "PAY-" + id, 55L, 100L, new BigDecimal("99.99"), "USD",
                status, PaymentMethod.CARD, failureReason, Instant.now(), Instant.now());
    }

    @Test
    void onMessage_InventoryReserved_PaymentSucceeds_PublishesPaymentSucceeded() throws Exception {
        when(paymentService.createPayment(any(CreatePaymentRequest.class)))
                .thenReturn(paymentResponse(900L, PaymentStatus.PENDING, null));
        when(paymentService.processPayment(eq(900L), isNull()))
                .thenReturn(paymentResponse(900L, PaymentStatus.SUCCESS, null));

        consumer.onMessage(objectMapper.writeValueAsString(inventoryReservedEvent(55L)));

        ArgumentCaptor<PaymentSucceededEvent> captor = ArgumentCaptor.forClass(PaymentSucceededEvent.class);
        verify(paymentEventProducer).publishPaymentSucceeded(captor.capture());
        assertEquals(55L, captor.getValue().orderId());
        assertEquals(900L, captor.getValue().paymentId());
        assertEquals(100L, captor.getValue().userId());
        verify(paymentEventProducer, never()).publishPaymentFailed(any());
    }

    @Test
    void onMessage_InventoryReserved_PaymentFails_PublishesPaymentFailed() throws Exception {
        when(paymentService.createPayment(any(CreatePaymentRequest.class)))
                .thenReturn(paymentResponse(901L, PaymentStatus.PENDING, null));
        when(paymentService.processPayment(eq(901L), isNull()))
                .thenReturn(paymentResponse(901L, PaymentStatus.FAILED, "Card declined"));

        consumer.onMessage(objectMapper.writeValueAsString(inventoryReservedEvent(56L)));

        ArgumentCaptor<PaymentFailedEvent> captor = ArgumentCaptor.forClass(PaymentFailedEvent.class);
        verify(paymentEventProducer).publishPaymentFailed(captor.capture());
        assertEquals(56L, captor.getValue().orderId());
        assertEquals("Card declined", captor.getValue().reason());
        verify(paymentEventProducer, never()).publishPaymentSucceeded(any());
    }

    @Test
    void onMessage_PaymentAlreadyExistsForOrder_SkipsReprocessing() throws Exception {
        when(paymentService.getPaymentsByOrderId(57L)).thenReturn(List.of(paymentResponse(902L, PaymentStatus.SUCCESS, null)));

        consumer.onMessage(objectMapper.writeValueAsString(inventoryReservedEvent(57L)));

        verify(paymentService, never()).createPayment(any());
        verifyNoInteractions(paymentEventProducer);
    }

    @Test
    void onMessage_InventoryReservationFailed_IsIgnored() {
        assertDoesNotThrow(() -> consumer.onMessage("{\"eventType\":\"InventoryReservationFailed\",\"orderId\":1}"));
        verifyNoInteractions(paymentService, paymentEventProducer);
    }

    @Test
    void onMessage_MalformedJson_IsLoggedNotThrown() {
        assertDoesNotThrow(() -> consumer.onMessage("{bad json"));
        verifyNoInteractions(paymentService, paymentEventProducer);
    }
}
