package com.shopsphere.payment.event.producer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shopsphere.payment.event.KafkaTopics;
import com.shopsphere.payment.event.PaymentFailedEvent;
import com.shopsphere.payment.event.PaymentSucceededEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PaymentEventProducerTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private PaymentEventProducer producer;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        producer = new PaymentEventProducer(kafkaTemplate, objectMapper);
        when(kafkaTemplate.send(any(String.class), any(String.class), any(String.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
    }

    @Test
    void publishPaymentSucceeded_SendsToPaymentEventsTopic_KeyedByOrderId() throws Exception {
        PaymentSucceededEvent event = PaymentSucceededEvent.of("corr-1", 55L, "ORD-55", 100L, 900L, new BigDecimal("99.99"), "USD");

        producer.publishPaymentSucceeded(event);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.PAYMENT_EVENTS), eq("55"), payloadCaptor.capture());

        JsonNode json = objectMapper.readTree(payloadCaptor.getValue());
        assertEquals("PaymentSucceeded", json.get("eventType").asText());
        assertEquals(1, json.get("eventVersion").asInt());
        assertEquals("corr-1", json.get("correlationId").asText());
        assertFalse(json.has("cardNumber"));
        assertFalse(json.has("cvv"));
    }

    @Test
    void publishPaymentFailed_SendsToPaymentEventsTopic_WithReason() throws Exception {
        PaymentFailedEvent event = PaymentFailedEvent.of("corr-2", 56L, "ORD-56", 100L, 901L, "Card declined");

        producer.publishPaymentFailed(event);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.PAYMENT_EVENTS), eq("56"), payloadCaptor.capture());

        JsonNode json = objectMapper.readTree(payloadCaptor.getValue());
        assertEquals("PaymentFailed", json.get("eventType").asText());
        assertEquals("Card declined", json.get("reason").asText());
    }

    @Test
    void publish_KafkaTemplateThrows_DoesNotPropagateException() {
        when(kafkaTemplate.send(any(String.class), any(String.class), any(String.class)))
                .thenThrow(new RuntimeException("broker unreachable"));

        PaymentSucceededEvent event = PaymentSucceededEvent.of("corr-1", 1L, "ORD-1", 1L, 1L, BigDecimal.ONE, "USD");

        assertDoesNotThrow(() -> producer.publishPaymentSucceeded(event));
    }
}
