package com.shopsphere.inventory.mapper;

import com.shopsphere.inventory.dto.response.InventoryReservationResponse;
import com.shopsphere.inventory.model.entity.InventoryReservation;
import com.shopsphere.inventory.model.entity.ReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ReservationMapperTest {

    private ReservationMapper reservationMapper;

    @BeforeEach
    void setUp() {
        reservationMapper = new ReservationMapper();
    }

    @Test
    void toResponse_ShouldMapInventoryReservationToResponse() {
        Instant now = Instant.now();
        InventoryReservation reservation = InventoryReservation.builder()
                .id(1L)
                .inventoryId(5L)
                .orderId(100L)
                .quantity(2)
                .status(ReservationStatus.RESERVED)
                .createdAt(now)
                .updatedAt(now)
                .build();

        InventoryReservationResponse response = reservationMapper.toResponse(reservation);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(5L, response.inventoryId());
        assertEquals(100L, response.orderId());
        assertEquals(2, response.quantity());
        assertEquals(ReservationStatus.RESERVED, response.status());
    }
}
