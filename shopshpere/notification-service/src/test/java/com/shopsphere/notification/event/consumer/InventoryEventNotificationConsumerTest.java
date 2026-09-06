package com.shopsphere.notification.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * This consumer is intentionally log-only (see its class Javadoc for why no
 * NotificationType fits an inventory-specific failure yet), so there is no
 * service dependency to verify interactions against - these tests confirm
 * the listener handles every payload shape without throwing.
 */
class InventoryEventNotificationConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final InventoryEventNotificationConsumer consumer = new InventoryEventNotificationConsumer(objectMapper);

    @Test
    void onMessage_InventoryReservationFailed_DoesNotThrow() {
        String payload = "{\"eventId\":\"e1\",\"eventType\":\"InventoryReservationFailed\",\"eventVersion\":1,"
                + "\"occurredAt\":\"2026-01-01T00:00:00Z\",\"correlationId\":\"corr-1\","
                + "\"orderId\":55,\"orderNumber\":\"ORD-55\",\"reason\":\"Insufficient inventory\"}";

        assertDoesNotThrow(() -> consumer.onMessage(payload));
    }

    @Test
    void onMessage_InventoryReserved_IsIgnoredWithoutThrowing() {
        String payload = "{\"eventType\":\"InventoryReserved\",\"orderId\":55}";
        assertDoesNotThrow(() -> consumer.onMessage(payload));
    }

    @Test
    void onMessage_UnrecognizedEventType_DoesNotThrow() {
        assertDoesNotThrow(() -> consumer.onMessage("{\"eventType\":\"Unknown\"}"));
    }

    @Test
    void onMessage_MalformedJson_IsLoggedNotThrown() {
        assertDoesNotThrow(() -> consumer.onMessage("not json"));
    }
}
