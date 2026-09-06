package com.shopsphere.user.model.entity;

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
    name = "addresses",
    indexes = {
        @Index(name = "idx_addresses_user_id", columnList = "user_id"),
        @Index(name = "idx_addresses_user_default", columnList = "user_id, is_default")
    }
)
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "address_line1", nullable = false)
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(name = "postal_code", nullable = false)
    private String postalCode;

    @Column(nullable = false)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AddressType type;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Address() {
    }

    public Address(Long id, Long userId, String addressLine1, String addressLine2, String city, String state, String postalCode, String country, AddressType type, Boolean isDefault, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
        this.type = type;
        this.isDefault = isDefault;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.isDefault == null) {
            this.isDefault = false;
        }
        if (this.type == null) {
            this.type = AddressType.HOME;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public AddressType getType() {
        return type;
    }

    public void setType(AddressType type) {
        this.type = type;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return id != null && Objects.equals(id, address.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public static AddressBuilder builder() {
        return new AddressBuilder();
    }

    public static class AddressBuilder {
        private Long id;
        private Long userId;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        private AddressType type;
        private Boolean isDefault;
        private Instant createdAt;
        private Instant updatedAt;

        public AddressBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public AddressBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public AddressBuilder addressLine1(String addressLine1) {
            this.addressLine1 = addressLine1;
            return this;
        }

        public AddressBuilder addressLine2(String addressLine2) {
            this.addressLine2 = addressLine2;
            return this;
        }

        public AddressBuilder city(String city) {
            this.city = city;
            return this;
        }

        public AddressBuilder state(String state) {
            this.state = state;
            return this;
        }

        public AddressBuilder postalCode(String postalCode) {
            this.postalCode = postalCode;
            return this;
        }

        public AddressBuilder country(String country) {
            this.country = country;
            return this;
        }

        public AddressBuilder type(AddressType type) {
            this.type = type;
            return this;
        }

        public AddressBuilder isDefault(Boolean isDefault) {
            this.isDefault = isDefault;
            return this;
        }

        public AddressBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public AddressBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Address build() {
            return new Address(id, userId, addressLine1, addressLine2, city, state, postalCode, country, type, isDefault, createdAt, updatedAt);
        }
    }
}
