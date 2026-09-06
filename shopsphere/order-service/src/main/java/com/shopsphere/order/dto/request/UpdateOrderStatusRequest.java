package com.shopsphere.order.dto.request;

import com.shopsphere.order.model.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
    @NotNull(message = "Order status is required")
    OrderStatus status
) {}
