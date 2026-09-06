package com.shopsphere.inventory.event.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.shopsphere.inventory.dto.request.ReleaseInventoryRequest;
import com.shopsphere.inventory.dto.request.ReserveInventoryRequest;
import com.shopsphere.inventory.dto.response.InventoryReservationResponse;
import com.shopsphere.inventory.event.InventoryReservationFailedEvent;
import com.shopsphere.inventory.event.InventoryReservedEvent;
import com.shopsphere.inventory.event.OrderCreatedEvent;
import com.shopsphere.inventory.event.producer.InventoryEventProducer;
import com.shopsphere.inventory.exception.InsufficientInventoryException;
import com.shopsphere.inventory.model.entity.ReservationStatus;
import com.shopsphere.inventory.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class InventoryEventConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private ReservationService reservationService;
    private InventoryEventProducer inventoryEventProducer;
    private InventoryEventConsumer consumer;

    @BeforeEach
    void setUp() {
        reservationService = mock(ReservationService.class);
        inventoryEventProducer = mock(InventoryEventProducer.class);
        consumer = new InventoryEventConsumer(objectMapper, reservationService, inventoryEventProducer);

        when(reservationService.getReservationsByOrderId(anyLong())).thenReturn(List.of());
    }

    private OrderCreatedEvent orderCreatedEvent(Long orderId, List<OrderCreatedEvent.Item> items) {
        return new OrderCreatedEvent("evt-1", "OrderCreated", 1, Instant.now(), "corr-1",
                orderId, "ORD-" + orderId, 100L, "USD", new BigDecimal("100.00"), "CARD", items);
    }

    @Test
    void onMessage_AllItemsReservable_PublishesInventoryReserved() throws Exception {
        when(reservationService.reserveInventory(eq(10L), any(ReserveInventoryRequest.class)))
                .thenReturn(new InventoryReservationResponse(901L, 1L, 55L, 2, ReservationStatus.RESERVED, Instant.now(), Instant.now()));
        when(reservationService.reserveInventory(eq(20L), any(ReserveInventoryRequest.class)))
                .thenReturn(new InventoryReservationResponse(902L, 2L, 55L, 1, ReservationStatus.RESERVED, Instant.now(), Instant.now()));

        OrderCreatedEvent event = orderCreatedEvent(55L, List.of(
                new OrderCreatedEvent.Item(10L, "SKU-1", 2, new BigDecimal("10.00")),
                new OrderCreatedEvent.Item(20L, "SKU-2", 1, new BigDecimal("20.00"))));

        consumer.onMessage(objectMapper.writeValueAsString(event));

        ArgumentCaptor<InventoryReservedEvent> captor = ArgumentCaptor.forClass(InventoryReservedEvent.class);
        verify(inventoryEventProducer).publishInventoryReserved(captor.capture());
        assertEquals(55L, captor.getValue().orderId());
        assertEquals(2, captor.getValue().reservations().size());
        verify(inventoryEventProducer, never()).publishInventoryReservationFailed(any());
        verify(reservationService, never()).releaseReservation(anyLong(), any());
    }

    @Test
    void onMessage_OneItemInsufficientStock_ReleasesEarlierReservationsAndPublishesFailed() throws Exception {
        when(reservationService.reserveInventory(eq(10L), any(ReserveInventoryRequest.class)))
                .thenReturn(new InventoryReservationResponse(901L, 1L, 55L, 2, ReservationStatus.RESERVED, Instant.now(), Instant.now()));
        when(reservationService.reserveInventory(eq(20L), any(ReserveInventoryRequest.class)))
                .thenThrow(new InsufficientInventoryException("Insufficient inventory for product ID 20: requested 1, available salable 0"));

        OrderCreatedEvent event = orderCreatedEvent(55L, List.of(
                new OrderCreatedEvent.Item(10L, "SKU-1", 2, new BigDecimal("10.00")),
                new OrderCreatedEvent.Item(20L, "SKU-2", 1, new BigDecimal("20.00"))));

        consumer.onMessage(objectMapper.writeValueAsString(event));

        // Item A was already reserved before item B failed - it must be released, not left
        // dangling (the existing Stage 3 "no partial reservations" business rule).
        verify(reservationService).releaseReservation(eq(901L), any(ReleaseInventoryRequest.class));

        ArgumentCaptor<InventoryReservationFailedEvent> captor = ArgumentCaptor.forClass(InventoryReservationFailedEvent.class);
        verify(inventoryEventProducer).publishInventoryReservationFailed(captor.capture());
        assertEquals(55L, captor.getValue().orderId());
        assertTrue(captor.getValue().reason().contains("Insufficient inventory"));
        verify(inventoryEventProducer, never()).publishInventoryReserved(any());
    }

    @Test
    void onMessage_ReservationsAlreadyExistForOrder_SkipsReprocessing() throws Exception {
        // Minimal duplicate-processing guard: simulates a Kafka redelivery of the same
        // OrderCreated message after it was already handled once.
        when(reservationService.getReservationsByOrderId(55L)).thenReturn(
                List.of(new InventoryReservationResponse(901L, 1L, 55L, 2, ReservationStatus.RESERVED, Instant.now(), Instant.now())));

        OrderCreatedEvent event = orderCreatedEvent(55L, List.of(new OrderCreatedEvent.Item(10L, "SKU-1", 2, new BigDecimal("10.00"))));

        consumer.onMessage(objectMapper.writeValueAsString(event));

        verify(reservationService, never()).reserveInventory(anyLong(), any());
        verifyNoInteractions(inventoryEventProducer);
    }

    @Test
    void onMessage_UnrecognizedEventType_IsIgnored() {
        assertDoesNotThrow(() -> consumer.onMessage("{\"eventType\":\"SomeOtherEvent\"}"));
        verifyNoInteractions(reservationService, inventoryEventProducer);
    }

    @Test
    void onMessage_MalformedJson_IsLoggedNotThrown() {
        assertDoesNotThrow(() -> consumer.onMessage("{malformed"));
        verifyNoInteractions(reservationService, inventoryEventProducer);
    }
}
