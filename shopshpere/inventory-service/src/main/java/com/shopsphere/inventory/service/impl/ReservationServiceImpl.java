package com.shopsphere.inventory.service.impl;

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
import com.shopsphere.inventory.service.ReservationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ReservationServiceImpl implements ReservationService {

    private final InventoryRepository inventoryRepository;
    private final InventoryReservationRepository reservationRepository;
    private final ReservationMapper reservationMapper;

    public ReservationServiceImpl(InventoryRepository inventoryRepository,
                                  InventoryReservationRepository reservationRepository,
                                  ReservationMapper reservationMapper) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.reservationMapper = reservationMapper;
    }

    @Override
    public InventoryReservationResponse reserveInventory(Long productId, ReserveInventoryRequest request) {
        // Obtain pessimistic write lock on product inventory row to eliminate race conditions
        Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product ID: " + productId));

        int salable = inventory.getSalableQuantity();
        if (salable < request.quantity()) {
            throw new InsufficientInventoryException(
                String.format("Insufficient inventory for product ID %d: requested %d, available salable %d",
                        productId, request.quantity(), salable)
            );
        }

        inventory.setReservedQuantity(inventory.getReservedQuantity() + request.quantity());
        inventoryRepository.save(inventory);

        InventoryReservation reservation = InventoryReservation.builder()
                .inventoryId(inventory.getId())
                .orderId(request.orderId())
                .quantity(request.quantity())
                .status(ReservationStatus.RESERVED)
                .build();

        InventoryReservation savedReservation = reservationRepository.save(reservation);
        return reservationMapper.toResponse(savedReservation);
    }

    @Override
    public InventoryReservationResponse releaseReservation(Long reservationId, ReleaseInventoryRequest request) {
        InventoryReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + reservationId));

        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            throw new InvalidReservationStateException(
                String.format("Cannot release reservation with status '%s'", reservation.getStatus())
            );
        }

        Inventory inventory = inventoryRepository.findByIdForUpdate(reservation.getInventoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + reservation.getInventoryId()));

        inventory.setReservedQuantity(Math.max(inventory.getReservedQuantity() - reservation.getQuantity(), 0));
        inventoryRepository.save(inventory);

        reservation.setStatus(ReservationStatus.RELEASED);
        InventoryReservation updatedReservation = reservationRepository.save(reservation);

        return reservationMapper.toResponse(updatedReservation);
    }

    @Override
    public InventoryReservationResponse confirmReservation(Long reservationId, ConfirmReservationRequest request) {
        InventoryReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + reservationId));

        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            throw new InvalidReservationStateException(
                String.format("Cannot confirm reservation with status '%s'", reservation.getStatus())
            );
        }

        Inventory inventory = inventoryRepository.findByIdForUpdate(reservation.getInventoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found with id: " + reservation.getInventoryId()));

        inventory.setReservedQuantity(Math.max(inventory.getReservedQuantity() - reservation.getQuantity(), 0));
        inventory.setAvailableQuantity(Math.max(inventory.getAvailableQuantity() - reservation.getQuantity(), 0));
        inventoryRepository.save(inventory);

        reservation.setStatus(ReservationStatus.CONFIRMED);
        InventoryReservation updatedReservation = reservationRepository.save(reservation);

        return reservationMapper.toResponse(updatedReservation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryReservationResponse> getReservationsByOrderId(Long orderId) {
        return reservationRepository.findByOrderId(orderId).stream()
                .map(reservationMapper::toResponse)
                .toList();
    }
}
