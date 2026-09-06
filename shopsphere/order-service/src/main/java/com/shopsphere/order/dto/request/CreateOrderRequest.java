package com.shopsphere.order.dto.request;

import com.shopsphere.order.model.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest(
    @NotNull(message = "User ID is required")
    Long userId,

    @NotBlank(message = "Currency is required")
    String currency,

    @NotNull(message = "Payment method is required")
    PaymentMethod paymentMethod,

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    List<CreateOrderItemRequest> items,

    @NotNull(message = "Shipping address is required")
    @Valid
    ShippingAddressRequest shippingAddress
) {}
