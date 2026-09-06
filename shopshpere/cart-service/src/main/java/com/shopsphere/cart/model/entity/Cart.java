package com.shopsphere.cart.model.entity;

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
    name = "carts",
    indexes = {
        @Index(name = "idx_carts_user_id", columnList = "user_id"),
        @Index(name = "idx_carts_status", columnList = "status")
    }
)
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CartStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Cart() {
    }

    public Cart(Long id, Long userId, CartStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.updatedAt == null) this.updatedAt = now;
        if (this.status == null) this.status = CartStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public CartStatus getStatus() { return status; }
    public void setStatus(CartStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cart cart = (Cart) o;
        return id != null && Objects.equals(id, cart.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    public static CartBuilder builder() { return new CartBuilder(); }

    public static class CartBuilder {
        private Long id;
        private Long userId;
        private CartStatus status;
        private Instant createdAt;
        private Instant updatedAt;

        public CartBuilder id(Long id) { this.id = id; return this; }
        public CartBuilder userId(Long userId) { this.userId = userId; return this; }
        public CartBuilder status(CartStatus status) { this.status = status; return this; }
        public CartBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public CartBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Cart build() { return new Cart(id, userId, status, createdAt, updatedAt); }
    }
}
