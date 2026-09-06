package com.shopsphere.inventory.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
    name = "inventory_reservations",
    indexes = {
        @Index(name = "idx_reservations_inventory_id", columnList = "inventory_id"),
        @Index(name = "idx_reservations_order_id", columnList = "order_id"),
        @Index(name = "idx_reservations_status", columnList = "status")
    }
)
public class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_id", nullable = false)
    private Long inventoryId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public InventoryReservation() {
    }

    public InventoryReservation(Long id, Long inventoryId, Long orderId, Integer quantity, ReservationStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.inventoryId = inventoryId;
        this.orderId = orderId;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.updatedAt == null) this.updatedAt = now;
        if (this.status == null) this.status = ReservationStatus.RESERVED;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getInventoryId() { return inventoryId; }
    public void setInventoryId(Long inventoryId) { this.inventoryId = inventoryId; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryReservation that = (InventoryReservation) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    public static InventoryReservationBuilder builder() { return new InventoryReservationBuilder(); }

    public static class InventoryReservationBuilder {
        private Long id;
        private Long inventoryId;
        private Long orderId;
        private Integer quantity;
        private ReservationStatus status;
        private Instant createdAt;
        private Instant updatedAt;

        public InventoryReservationBuilder id(Long id) { this.id = id; return this; }
        public InventoryReservationBuilder inventoryId(Long inventoryId) { this.inventoryId = inventoryId; return this; }
        public InventoryReservationBuilder orderId(Long orderId) { this.orderId = orderId; return this; }
        public InventoryReservationBuilder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public InventoryReservationBuilder status(ReservationStatus status) { this.status = status; return this; }
        public InventoryReservationBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public InventoryReservationBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public InventoryReservation build() {
            return new InventoryReservation(id, inventoryId, orderId, quantity, status, createdAt, updatedAt);
        }
    }
}
