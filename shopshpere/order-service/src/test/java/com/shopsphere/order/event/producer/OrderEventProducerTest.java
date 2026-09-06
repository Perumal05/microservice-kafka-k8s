package com.shopsphere.order.event.producer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shopsphere.order.event.KafkaTopics;
import com.shopsphere.order.event.OrderCreatedEvent;
import com.shopsphere.order.event.OrderCreatedEventItem;
import com.shopsphere.order.model.entity.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OrderEventProducerTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private OrderEventProducer producer;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        producer = new OrderEventProducer(kafkaTemplate, objectMapper);
        when(kafkaTemplate.send(any(String.class), any(String.class), any(String.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
    }

    @Test
    void publishOrderCreated_SendsToOrderEventsTopic_KeyedByOrderId() {
        OrderCreatedEvent event = OrderCreatedEvent.of("corr-1", 55L, "ORD-55", 100L, "USD",
                new BigDecimal("99.99"), PaymentMethod.CARD, List.of(new OrderCreatedEventItem(10L, "SKU-1", 1, new BigDecimal("99.99"))));

        producer.publishOrderCreated(event);

        verify(kafkaTemplate).send(eq(KafkaTopics.ORDER_EVENTS), eq("55"), any(String.class));
    }

    @Test
    void publishOrderCreated_SerializesEventAsJsonWithEventTypeAndCorrelationId() throws Exception {
        OrderCreatedEvent event = OrderCreatedEvent.of("corr-xyz", 7L, "ORD-7", 1L, "USD",
                BigDecimal.TEN, PaymentMethod.UPI, List.of());

        producer.publishOrderCreated(event);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.ORDER_EVENTS), eq("7"), payloadCaptor.capture());

        JsonNode json = objectMapper.readTree(payloadCaptor.getValue());
        assertEquals("OrderCreated", json.get("eventType").asText());
        assertEquals(1, json.get("eventVersion").asInt());
        assertEquals("corr-xyz", json.get("correlationId").asText());
        assertEquals(7, json.get("orderId").asLong());
    }

    @Test
    void publishOrderCreated_KafkaTemplateThrows_DoesNotPropagateException() {
        // The dual-write limitation means a publish failure must be logged, not thrown -
        // otherwise a Kafka outage would also break order creation, defeating the point of
        // decoupling via events.
        when(kafkaTemplate.send(any(String.class), any(String.class), any(String.class)))
                .thenThrow(new RuntimeException("broker unreachable"));

        OrderCreatedEvent event = OrderCreatedEvent.of("corr-1", 1L, "ORD-1", 1L, "USD", BigDecimal.ONE, PaymentMethod.CARD, List.of());

        assertDoesNotThrow(() -> producer.publishOrderCreated(event));
    }
}
