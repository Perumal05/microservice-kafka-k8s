package com.shopsphere.inventory.service;

import com.shopsphere.inventory.dto.request.ConfirmReservationRequest;
import com.shopsphere.inventory.dto.request.ReleaseInventoryRequest;
import com.shopsphere.inventory.dto.request.ReserveInventoryRequest;
import com.shopsphere.inventory.dto.response.InventoryReservationResponse;

import java.util.List;

public interface ReservationService {
    InventoryReservationResponse reserveInventory(Long productId, ReserveInventoryRequest request);
    InventoryReservationResponse releaseReservation(Long reservationId, ReleaseInventoryRequest request);
    InventoryReservationResponse confirmReservation(Long reservationId, ConfirmReservationRequest request);
    List<InventoryReservationResponse> getReservationsByOrderId(Long orderId);
}
