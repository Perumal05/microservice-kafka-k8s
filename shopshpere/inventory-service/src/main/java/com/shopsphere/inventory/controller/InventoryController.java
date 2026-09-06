package com.shopsphere.inventory.controller;

import com.shopsphere.inventory.dto.request.ConfirmReservationRequest;
import com.shopsphere.inventory.dto.request.CreateInventoryRequest;
import com.shopsphere.inventory.dto.request.ReleaseInventoryRequest;
import com.shopsphere.inventory.dto.request.ReserveInventoryRequest;
import com.shopsphere.inventory.dto.request.UpdateInventoryRequest;
import com.shopsphere.inventory.dto.response.AvailabilityResponse;
import com.shopsphere.inventory.dto.response.InventoryReservationResponse;
import com.shopsphere.inventory.dto.response.InventoryResponse;
import com.shopsphere.inventory.service.InventoryService;
import com.shopsphere.inventory.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory Controller", description = "APIs for stock management and inventory reservations")
public class InventoryController {

    private final InventoryService inventoryService;
    private final ReservationService reservationService;

    public InventoryController(InventoryService inventoryService, ReservationService reservationService) {
        this.inventoryService = inventoryService;
        this.reservationService = reservationService;
    }

    @PostMapping
    @Operation(summary = "Initialize product inventory record")
    public ResponseEntity<InventoryResponse> createInventory(@Valid @RequestBody CreateInventoryRequest request) {
        InventoryResponse response = inventoryService.createInventory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Get inventory record for product ID")
    public ResponseEntity<InventoryResponse> getInventoryByProductId(@PathVariable Long productId) {
        InventoryResponse response = inventoryService.getInventoryByProductId(productId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Update available stock quantity and reorder level")
    public ResponseEntity<InventoryResponse> updateInventory(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateInventoryRequest request) {
        InventoryResponse response = inventoryService.updateInventory(productId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{productId}/reserve")
    @Operation(summary = "Reserve inventory quantity for an order")
    public ResponseEntity<InventoryReservationResponse> reserveInventory(
            @PathVariable Long productId,
            @Valid @RequestBody ReserveInventoryRequest request) {
        InventoryReservationResponse response = reservationService.reserveInventory(productId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reservations/{reservationId}/release")
    @Operation(summary = "Release a pending reservation back to salable stock")
    public ResponseEntity<InventoryReservationResponse> releaseReservation(
            @PathVariable Long reservationId,
            @RequestBody(required = false) ReleaseInventoryRequest request) {
        InventoryReservationResponse response = reservationService.releaseReservation(reservationId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reservations/{reservationId}/confirm")
    @Operation(summary = "Confirm a reservation (permanently consume stock)")
    public ResponseEntity<InventoryReservationResponse> confirmReservation(
            @PathVariable Long reservationId,
            @RequestBody(required = false) ConfirmReservationRequest request) {
        InventoryReservationResponse response = reservationService.confirmReservation(reservationId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{productId}/availability")
    @Operation(summary = "Check product stock availability")
    public ResponseEntity<AvailabilityResponse> checkAvailability(@PathVariable Long productId) {
        AvailabilityResponse response = inventoryService.checkAvailability(productId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reservations/order/{orderId}")
    @Operation(summary = "Get all inventory reservations associated with an order ID")
    public ResponseEntity<List<InventoryReservationResponse>> getReservationsByOrderId(@PathVariable Long orderId) {
        List<InventoryReservationResponse> responses = reservationService.getReservationsByOrderId(orderId);
        return ResponseEntity.ok(responses);
    }
}
