package com.shopsphere.inventory.service;

import com.shopsphere.inventory.dto.request.ConfirmReservationRequest;
import com.shopsphere.inventory.dto.request.ReleaseInventoryRequest;
import com.shopsphere.inventory.dto.request.ReserveInventoryRequest;
import com.shopsphere.inventory.dto.response.InventoryReservationResponse;
import com.shopsphere.inventory.exception.InsufficientInventoryException;
import com.shopsphere.inventory.exception.InvalidReservationStateException;
import com.shopsphere.inventory.exception.ResourceNotFoundException;
import com.shopsphere.inventory.mapper.ReservationMapper;
import com.shopsphere.inventory.model.entity.Inventory;
import com.shopsphere.inventory.model.entity.InventoryReservation;
import com.shopsphere.inventory.model.entity.ReservationStatus;
import com.shopsphere.inventory.repository.InventoryRepository;
import com.shopsphere.inventory.repository.InventoryReservationRepository;
import com.shopsphere.inventory.service.impl.ReservationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReservationServiceImplTest {

    private InventoryRepository inventoryRepository;
    private InventoryReservationRepository reservationRepository;
    private ReservationMapper reservationMapper;
    private ReservationServiceImpl reservationService;

    @BeforeEach
    void setUp() {
        inventoryRepository = mock(InventoryRepository.class);
        reservationRepository = mock(InventoryReservationRepository.class);
        reservationMapper = new ReservationMapper();
        reservationService = new ReservationServiceImpl(inventoryRepository, reservationRepository, reservationMapper);
    }

    @Test
    void reserveInventory_Success() {
        Inventory inventory = Inventory.builder()
                .id(1L).productId(10L).sku("SKU-100").availableQuantity(50).reservedQuantity(10)
                .build();
        ReserveInventoryRequest request = new ReserveInventoryRequest(100L, 5);

        when(inventoryRepository.findByProductIdForUpdate(10L)).thenReturn(Optional.of(inventory));
        when(reservationRepository.save(any(InventoryReservation.class))).thenAnswer(inv -> {
            InventoryReservation res = inv.getArgument(0);
            res.setId(10L);
            res.setCreatedAt(Instant.now());
            res.setUpdatedAt(Instant.now());
            return res;
        });

        InventoryReservationResponse response = reservationService.reserveInventory(10L, request);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals(ReservationStatus.RESERVED, response.status());
        assertEquals(15, inventory.getReservedQuantity());
        verify(inventoryRepository).save(inventory);
    }

    @Test
    void reserveInventory_InsufficientStock_ThrowsInsufficientInventoryException() {
        Inventory inventory = Inventory.builder()
                .id(1L).productId(10L).sku("SKU-100").availableQuantity(10).reservedQuantity(8) // salable = 2
                .build();
        ReserveInventoryRequest request = new ReserveInventoryRequest(100L, 5);

        when(inventoryRepository.findByProductIdForUpdate(10L)).thenReturn(Optional.of(inventory));

        assertThrows(InsufficientInventoryException.class, () -> reservationService.reserveInventory(10L, request));
    }

    @Test
    void releaseReservation_Success() {
        InventoryReservation reservation = InventoryReservation.builder()
                .id(10L).inventoryId(1L).orderId(100L).quantity(5).status(ReservationStatus.RESERVED)
                .build();
        Inventory inventory = Inventory.builder()
                .id(1L).availableQuantity(50).reservedQuantity(15)
                .build();

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));
        when(inventoryRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(inventory));
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        InventoryReservationResponse response = reservationService.releaseReservation(10L, new ReleaseInventoryRequest("Cancelled order"));

        assertEquals(ReservationStatus.RELEASED, response.status());
        assertEquals(10, inventory.getReservedQuantity());
    }

    @Test
    void releaseReservation_AlreadyReleased_ThrowsInvalidReservationStateException() {
        InventoryReservation reservation = InventoryReservation.builder()
                .id(10L).status(ReservationStatus.RELEASED)
                .build();
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));

        assertThrows(InvalidReservationStateException.class, () -> reservationService.releaseReservation(10L, null));
    }

    @Test
    void confirmReservation_Success() {
        InventoryReservation reservation = InventoryReservation.builder()
                .id(10L).inventoryId(1L).orderId(100L).quantity(5).status(ReservationStatus.RESERVED)
                .build();
        Inventory inventory = Inventory.builder()
                .id(1L).availableQuantity(50).reservedQuantity(15)
                .build();

        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));
        when(inventoryRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(inventory));
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        InventoryReservationResponse response = reservationService.confirmReservation(10L, new ConfirmReservationRequest("Order shipped"));

        assertEquals(ReservationStatus.CONFIRMED, response.status());
        assertEquals(10, inventory.getReservedQuantity());
        assertEquals(45, inventory.getAvailableQuantity());
    }

    @Test
    void confirmReservation_AlreadyConfirmed_ThrowsInvalidReservationStateException() {
        InventoryReservation reservation = InventoryReservation.builder()
                .id(10L).status(ReservationStatus.CONFIRMED)
                .build();
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));

        assertThrows(InvalidReservationStateException.class, () -> reservationService.confirmReservation(10L, null));
    }

    @Test
    void getReservationsByOrderId_Success() {
        InventoryReservation reservation = InventoryReservation.builder()
                .id(10L).inventoryId(1L).orderId(100L).quantity(5).status(ReservationStatus.RESERVED)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        when(reservationRepository.findByOrderId(100L)).thenReturn(List.of(reservation));

        List<InventoryReservationResponse> responses = reservationService.getReservationsByOrderId(100L);

        assertEquals(1, responses.size());
        assertEquals(100L, responses.get(0).orderId());
    }
}
