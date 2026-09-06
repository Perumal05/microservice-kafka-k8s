package com.shopsphere.order.event;

import com.shopsphere.order.model.entity.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderCreatedEventTest {

    @Test
    void of_PopulatesAllMetadataFields() {
        List<OrderCreatedEventItem> items = List.of(new OrderCreatedEventItem(10L, "SKU-1", 2, new BigDecimal("25.00")));

        OrderCreatedEvent event = OrderCreatedEvent.of(
                "corr-123", 55L, "ORD-20260101-ABC123", 100L, "USD",
                new BigDecimal("50.00"), PaymentMethod.CARD, items);

        assertNotNull(event.eventId());
        assertEquals(EventTypes.ORDER_CREATED, event.eventType());
        assertEquals(1, event.eventVersion());
        assertNotNull(event.occurredAt());
        assertEquals("corr-123", event.correlationId());
        assertEquals(55L, event.orderId());
        assertEquals("ORD-20260101-ABC123", event.orderNumber());
        assertEquals(100L, event.userId());
        assertEquals("USD", event.currency());
        assertEquals(new BigDecimal("50.00"), event.totalAmount());
        assertEquals(PaymentMethod.CARD, event.paymentMethod());
        assertEquals(1, event.items().size());
    }

    @Test
    void of_GeneratesUniqueEventIdPerCall() {
        List<OrderCreatedEventItem> items = List.of();
        OrderCreatedEvent first = OrderCreatedEvent.of("corr-1", 1L, "ORD-1", 1L, "USD", BigDecimal.ONE, PaymentMethod.CARD, items);
        OrderCreatedEvent second = OrderCreatedEvent.of("corr-1", 1L, "ORD-1", 1L, "USD", BigDecimal.ONE, PaymentMethod.CARD, items);

        // eventId must never be reused, even for the same order - orderId is not a
        // substitute for eventId (this matters once idempotency is implemented later).
        assertNotEquals(first.eventId(), second.eventId());
    }

    @Test
    void of_PreservesNullCorrelationIdRatherThanFabricatingOne() {
        OrderCreatedEvent event = OrderCreatedEvent.of(null, 1L, "ORD-1", 1L, "USD", BigDecimal.ONE, PaymentMethod.CARD, List.of());
        assertNull(event.correlationId());
    }
}
