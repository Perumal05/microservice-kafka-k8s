package com.shopsphere.order.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shopsphere.order.event.PaymentFailedEvent;
import com.shopsphere.order.event.PaymentSucceededEvent;
import com.shopsphere.order.model.entity.OrderStatus;
import com.shopsphere.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class PaymentEventConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private OrderService orderService;
    private PaymentEventConsumer consumer;

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        consumer = new PaymentEventConsumer(objectMapper, orderService);
    }

    @Test
    void onMessage_PaymentSucceeded_MarksOrderPaid() throws Exception {
        PaymentSucceededEvent event = new PaymentSucceededEvent(
                "evt-1", "PaymentSucceeded", 1, Instant.now(), "corr-1",
                55L, "ORD-55", 900L, new BigDecimal("99.99"), "USD");

        consumer.onMessage(objectMapper.writeValueAsString(event));

        verify(orderService).updateOrderStatus(55L, OrderStatus.PAID);
    }

    @Test
    void onMessage_PaymentFailed_MarksOrderFailed() throws Exception {
        PaymentFailedEvent event = new PaymentFailedEvent(
                "evt-2", "PaymentFailed", 1, Instant.now(), "corr-2",
                56L, "ORD-56", 901L, "Card declined");

        consumer.onMessage(objectMapper.writeValueAsString(event));

        verify(orderService).updateOrderStatus(56L, OrderStatus.FAILED);
    }

    @Test
    void onMessage_UnrecognizedEventType_IsIgnored() {
        assertDoesNotThrow(() -> consumer.onMessage("{\"eventType\":\"Unknown\"}"));
        verifyNoInteractions(orderService);
    }

    @Test
    void onMessage_MalformedJson_IsLoggedNotThrown() {
        assertDoesNotThrow(() -> consumer.onMessage("{not json"));
        verifyNoInteractions(orderService);
    }
}
