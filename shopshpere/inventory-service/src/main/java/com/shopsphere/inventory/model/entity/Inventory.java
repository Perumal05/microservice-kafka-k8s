package com.shopsphere.inventory.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    name = "inventory",
    indexes = {
        @Index(name = "idx_inventory_product_id", columnList = "product_id"),
        @Index(name = "idx_inventory_sku", columnList = "sku")
    }
)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Column(nullable = false, length = 100)
    private String sku;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    @Column(name = "reorder_level", nullable = false)
    private Integer reorderLevel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Inventory() {
    }

    public Inventory(Long id, Long productId, String sku, Integer availableQuantity, Integer reservedQuantity, Integer reorderLevel, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.productId = productId;
        this.sku = sku;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
        this.reorderLevel = reorderLevel;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.updatedAt == null) this.updatedAt = now;
        if (this.availableQuantity == null) this.availableQuantity = 0;
        if (this.reservedQuantity == null) this.reservedQuantity = 0;
        if (this.reorderLevel == null) this.reorderLevel = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public Integer getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; }
    public Integer getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(Integer reservedQuantity) { this.reservedQuantity = reservedQuantity; }
    public Integer getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(Integer reorderLevel) { this.reorderLevel = reorderLevel; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Integer getSalableQuantity() {
        int salable = (availableQuantity != null ? availableQuantity : 0) - (reservedQuantity != null ? reservedQuantity : 0);
        return Math.max(salable, 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Inventory inventory = (Inventory) o;
        return id != null && Objects.equals(id, inventory.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    public static InventoryBuilder builder() { return new InventoryBuilder(); }

    public static class InventoryBuilder {
        private Long id;
        private Long productId;
        private String sku;
        private Integer availableQuantity;
        private Integer reservedQuantity;
        private Integer reorderLevel;
        private Instant createdAt;
        private Instant updatedAt;

        public InventoryBuilder id(Long id) { this.id = id; return this; }
        public InventoryBuilder productId(Long productId) { this.productId = productId; return this; }
        public InventoryBuilder sku(String sku) { this.sku = sku; return this; }
        public InventoryBuilder availableQuantity(Integer availableQuantity) { this.availableQuantity = availableQuantity; return this; }
        public InventoryBuilder reservedQuantity(Integer reservedQuantity) { this.reservedQuantity = reservedQuantity; return this; }
        public InventoryBuilder reorderLevel(Integer reorderLevel) { this.reorderLevel = reorderLevel; return this; }
        public InventoryBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public InventoryBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        public Inventory build() {
            return new Inventory(id, productId, sku, availableQuantity, reservedQuantity, reorderLevel, createdAt, updatedAt);
        }
    }
}
