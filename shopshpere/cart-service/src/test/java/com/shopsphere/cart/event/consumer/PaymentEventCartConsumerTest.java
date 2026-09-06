package com.shopsphere.cart.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shopsphere.cart.event.PaymentSucceededEvent;
import com.shopsphere.cart.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PaymentEventCartConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private CartService cartService;
    private PaymentEventCartConsumer consumer;

    @BeforeEach
    void setUp() {
        cartService = mock(CartService.class);
        consumer = new PaymentEventCartConsumer(objectMapper, cartService);
    }

    @Test
    void onMessage_PaymentSucceeded_ClearsCartForThatUser() throws Exception {
        PaymentSucceededEvent event = new PaymentSucceededEvent(
                "evt-1", "PaymentSucceeded", 1, Instant.now(), "corr-1",
                55L, "ORD-55", 100L, 900L, new BigDecimal("99.99"), "USD");

        consumer.onMessage(objectMapper.writeValueAsString(event));

        verify(cartService).clearCart(eq(100L));
    }

    @Test
    void onMessage_PaymentFailed_DoesNotClearCart() {
        String payload = "{\"eventType\":\"PaymentFailed\",\"orderId\":56,\"userId\":100}";

        consumer.onMessage(payload);

        verifyNoInteractions(cartService);
    }

    @Test
    void onMessage_PaymentSucceededWithoutUserId_DoesNotThrowAndDoesNotClearCart() throws Exception {
        String payload = "{\"eventId\":\"e1\",\"eventType\":\"PaymentSucceeded\",\"eventVersion\":1,"
                + "\"occurredAt\":\"2026-01-01T00:00:00Z\",\"correlationId\":\"corr-1\","
                + "\"orderId\":57,\"orderNumber\":\"ORD-57\",\"userId\":null,\"paymentId\":901,"
                + "\"amount\":10.00,\"currency\":\"USD\"}";

        assertDoesNotThrow(() -> consumer.onMessage(payload));
        verifyNoInteractions(cartService);
    }

    @Test
    void onMessage_UnrecognizedEventType_IsIgnored() {
        assertDoesNotThrow(() -> consumer.onMessage("{\"eventType\":\"Unknown\"}"));
        verifyNoInteractions(cartService);
    }

    @Test
    void onMessage_MalformedJson_IsLoggedNotThrown() {
        assertDoesNotThrow(() -> consumer.onMessage("not json"));
        verifyNoInteractions(cartService);
    }

    @Test
    void onMessage_CartServiceThrows_DoesNotPropagateException() {
        doThrow(new RuntimeException("db unavailable")).when(cartService).clearCart(100L);
        String payload = "{\"eventId\":\"e1\",\"eventType\":\"PaymentSucceeded\",\"eventVersion\":1,"
                + "\"occurredAt\":\"2026-01-01T00:00:00Z\",\"correlationId\":\"corr-1\","
                + "\"orderId\":58,\"orderNumber\":\"ORD-58\",\"userId\":100,\"paymentId\":902,"
                + "\"amount\":10.00,\"currency\":\"USD\"}";

        assertDoesNotThrow(() -> consumer.onMessage(payload));
    }
}
