package com.shopsphere.cart.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
    name = "cart_items",
    indexes = {
        @Index(name = "idx_cart_items_cart_id", columnList = "cart_id"),
        @Index(name = "idx_cart_items_product_id", columnList = "product_id")
    }
)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cart_id", nullable = false)
    private Long cartId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public CartItem() {
    }

    public CartItem(Long id, Long cartId, Long productId, Integer quantity, BigDecimal unitPrice, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.cartId = cartId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) this.createdAt = now;
        if (this.updatedAt == null) this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCartId() { return cartId; }
    public void setCartId(Long cartId) { this.cartId = cartId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CartItem cartItem = (CartItem) o;
        return id != null && Objects.equals(id, cartItem.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }

    public static CartItemBuilder builder() { return new CartItemBuilder(); }

    public static class CartItemBuilder {
        private Long id;
        private Long cartId;
        private Long productId;
        private Integer quantity;
        private BigDecimal unitPrice;
        private Instant createdAt;
        private Instant updatedAt;

        public CartItemBuilder id(Long id) { this.id = id; return this; }
        public CartItemBuilder cartId(Long cartId) { this.cartId = cartId; return this; }
        public CartItemBuilder productId(Long productId) { this.productId = productId; return this; }
        public CartItemBuilder quantity(Integer quantity) { this.quantity = quantity; return this; }
        public CartItemBuilder unitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; return this; }
        public CartItemBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public CartItemBuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public CartItem build() { return new CartItem(id, cartId, productId, quantity, unitPrice, createdAt, updatedAt); }
    }
}
