package com.shopsphere.inventory.event.producer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shopsphere.inventory.event.InventoryReservationFailedEvent;
import com.shopsphere.inventory.event.InventoryReservedEvent;
import com.shopsphere.inventory.event.KafkaTopics;
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

class InventoryEventProducerTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private InventoryEventProducer producer;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        producer = new InventoryEventProducer(kafkaTemplate, objectMapper);
        when(kafkaTemplate.send(any(String.class), any(String.class), any(String.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
    }

    @Test
    void publishInventoryReserved_SendsToInventoryEventsTopic_KeyedByOrderId() throws Exception {
        InventoryReservedEvent event = InventoryReservedEvent.of("corr-1", 55L, "ORD-55", 100L, "USD",
                new BigDecimal("50.00"), "CARD", List.of(new InventoryReservedEvent.ReservationInfo(10L, 900L, 2)));

        producer.publishInventoryReserved(event);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.INVENTORY_EVENTS), eq("55"), payloadCaptor.capture());

        JsonNode json = objectMapper.readTree(payloadCaptor.getValue());
        assertEquals("InventoryReserved", json.get("eventType").asText());
        assertEquals(1, json.get("eventVersion").asInt());
        assertEquals("corr-1", json.get("correlationId").asText());
        assertTrue(json.get("reservations").isArray());
        assertEquals(1, json.get("reservations").size());
    }

    @Test
    void publishInventoryReservationFailed_SendsToInventoryEventsTopic_WithReason() throws Exception {
        InventoryReservationFailedEvent event = InventoryReservationFailedEvent.of(
                "corr-2", 56L, "ORD-56", "Insufficient inventory for product ID 20: requested 5, available salable 2");

        producer.publishInventoryReservationFailed(event);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(KafkaTopics.INVENTORY_EVENTS), eq("56"), payloadCaptor.capture());

        JsonNode json = objectMapper.readTree(payloadCaptor.getValue());
        assertEquals("InventoryReservationFailed", json.get("eventType").asText());
        assertTrue(json.get("reason").asText().contains("Insufficient inventory"));
    }

    @Test
    void publish_KafkaTemplateThrows_DoesNotPropagateException() {
        when(kafkaTemplate.send(any(String.class), any(String.class), any(String.class)))
                .thenThrow(new RuntimeException("broker unreachable"));

        InventoryReservedEvent event = InventoryReservedEvent.of("corr-1", 1L, "ORD-1", 1L, "USD",
                BigDecimal.ONE, "CARD", List.of());

        assertDoesNotThrow(() -> producer.publishInventoryReserved(event));
    }
}
