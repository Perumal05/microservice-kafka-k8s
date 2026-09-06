package com.shopsphere.order.dto.response;

public record ShippingAddressResponse(
    String addressLine1,
    String addressLine2,
    String city,
    String state,
    String postalCode,
    String country
) {}
