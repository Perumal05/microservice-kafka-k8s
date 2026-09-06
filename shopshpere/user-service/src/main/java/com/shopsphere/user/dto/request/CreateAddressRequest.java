package com.shopsphere.user.dto.request;

import com.shopsphere.user.model.entity.AddressType;
import jakarta.validation.constraints.NotBlank;

public record CreateAddressRequest(
    @NotBlank(message = "Address line 1 is required")
    String addressLine1,

    String addressLine2,

    @NotBlank(message = "City is required")
    String city,

    @NotBlank(message = "State is required")
    String state,

    @NotBlank(message = "Postal code is required")
    String postalCode,

    @NotBlank(message = "Country is required")
    String country,

    AddressType type,

    Boolean isDefault
) {}
