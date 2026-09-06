package com.shopsphere.user.dto.response;

import com.shopsphere.user.model.entity.AddressType;

import java.time.Instant;

public record AddressResponse(
    Long id,
    Long userId,
    String addressLine1,
    String addressLine2,
    String city,
    String state,
    String postalCode,
    String country,
    AddressType type,
    Boolean isDefault,
    Instant createdAt,
    Instant updatedAt
) {}
