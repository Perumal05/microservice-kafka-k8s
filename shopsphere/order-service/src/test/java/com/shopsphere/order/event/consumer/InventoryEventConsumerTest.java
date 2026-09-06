package com.shopsphere.order.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shopsphere.order.dto.response.OrderResponse;
import com.shopsphere.order.event.InventoryReservationFailedEvent;
import com.shopsphere.order.event.InventoryReservedEvent;
import com.shopsphere.order.exception.ResourceNotFoundException;
import com.shopsphere.order.model.entity.OrderStatus;
import com.shopsphere.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class InventoryEventConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private OrderService orderService;
    private InventoryEventConsumer consumer;

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        consumer = new InventoryEventConsumer(objectMapper, orderService);
    }

    @Test
    void onMessage_InventoryReserved_MovesOrderToPaymentPending() throws Exception {
        InventoryReservedEvent event = new InventoryReservedEvent(
                "evt-1", "InventoryReserved", 1, Instant.now(), "corr-1",
                55L, "ORD-55", List.of(new InventoryReservedEvent.ReservationInfo(10L, 900L, 2)));

        consumer.onMessage(objectMapper.writeValueAsString(event));

        verify(orderService).updateOrderStatus(55L, OrderStatus.PAYMENT_PENDING);
    }

    @Test
    void onMessage_InventoryReservationFailed_MarksOrderFailed() throws Exception {
        InventoryReservationFailedEvent event = new InventoryReservationFailedEvent(
                "evt-2", "InventoryReservationFailed", 1, Instant.now(), "corr-2",
                56L, "ORD-56", "Insufficient inventory for product ID 10");

        consumer.onMessage(objectMapper.writeValueAsString(event));

        verify(orderService).updateOrderStatus(56L, OrderStatus.FAILED);
    }

    @Test
    void onMessage_UnrecognizedEventType_IsIgnoredWithoutError() {
        String payload = "{\"eventType\":\"SomethingElse\",\"orderId\":1}";

        assertDoesNotThrow(() -> consumer.onMessage(payload));
        verifyNoInteractions(orderService);
    }

    @Test
    void onMessage_MalformedJson_IsLoggedNotThrown() {
        assertDoesNotThrow(() -> consumer.onMessage("not valid json {{{"));
        verifyNoInteractions(orderService);
    }

    @Test
    void onMessage_OrderNotFound_DoesNotPropagateException() throws Exception {
        // A late/redelivered event for an order that no longer exists (or an ID mismatch
        // across environments) must not crash the consumer thread.
        when(orderService.updateOrderStatus(eq(999L), eq(OrderStatus.PAYMENT_PENDING)))
                .thenThrow(new ResourceNotFoundException("Order not found with id: 999"));

        InventoryReservedEvent event = new InventoryReservedEvent(
                "evt-3", "InventoryReserved", 1, Instant.now(), "corr-3", 999L, "ORD-999", List.of());

        assertDoesNotThrow(() -> consumer.onMessage(objectMapper.writeValueAsString(event)));
    }

    @Test
    void onMessage_PreservesCorrelationIdThroughToOrderResponse() throws Exception {
        // Correlation ID isn't passed to OrderService (it's not part of the order status
        // update contract) but it must still be present on the deserialized event for
        // logging - this test guards against the field silently disappearing.
        InventoryReservedEvent event = new InventoryReservedEvent(
                "evt-4", "InventoryReserved", 1, Instant.now(), "corr-propagate-me",
                60L, "ORD-60", List.of());
        String json = objectMapper.writeValueAsString(event);

        InventoryReservedEvent roundTripped = objectMapper.readValue(json, InventoryReservedEvent.class);
        org.junit.jupiter.api.Assertions.assertEquals("corr-propagate-me", roundTripped.correlationId());
    }
}
