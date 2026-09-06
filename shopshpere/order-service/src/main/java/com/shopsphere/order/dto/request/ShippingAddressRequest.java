package com.shopsphere.order.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ShippingAddressRequest(
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
    String country
) {}
