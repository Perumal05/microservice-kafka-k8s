package com.shopsphere.inventory.mapper;

import com.shopsphere.inventory.dto.response.InventoryReservationResponse;
import com.shopsphere.inventory.model.entity.InventoryReservation;
import org.springframework.stereotype.Component;

@Component
public class ReservationMapper {

    public InventoryReservationResponse toResponse(InventoryReservation reservation) {
        if (reservation == null) {
            return null;
        }
        return new InventoryReservationResponse(
                reservation.getId(),
                reservation.getInventoryId(),
                reservation.getOrderId(),
                reservation.getQuantity(),
                reservation.getStatus(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt()
        );
    }
}
