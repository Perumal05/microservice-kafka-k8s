package com.shopsphere.order.mapper;

import com.shopsphere.order.dto.request.CreateOrderRequest;
import com.shopsphere.order.dto.response.OrderItemResponse;
import com.shopsphere.order.dto.response.OrderResponse;
import com.shopsphere.order.dto.response.ShippingAddressResponse;
import com.shopsphere.order.model.entity.Order;
import com.shopsphere.order.model.entity.OrderItem;
import com.shopsphere.order.model.entity.OrderStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class OrderMapper {

    private final OrderItemMapper orderItemMapper;

    public OrderMapper(OrderItemMapper orderItemMapper) {
        this.orderItemMapper = orderItemMapper;
    }

    public Order toEntity(CreateOrderRequest request, String orderNumber, BigDecimal subtotal, BigDecimal totalAmount, List<OrderItem> items) {
        if (request == null) {
            return null;
        }
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .userId(request.userId())
                .status(OrderStatus.PENDING)
                .currency(request.currency())
                .paymentMethod(request.paymentMethod())
                .subtotal(subtotal)
                .shippingAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(totalAmount)
                .shippingAddressLine1(request.shippingAddress().addressLine1())
                .shippingAddressLine2(request.shippingAddress().addressLine2())
                .shippingCity(request.shippingAddress().city())
                .shippingState(request.shippingAddress().state())
                .shippingPostalCode(request.shippingAddress().postalCode())
                .shippingCountry(request.shippingAddress().country())
                .items(items)
                .build();

        return order;
    }

    public OrderResponse toResponse(Order order) {
        if (order == null) {
            return null;
        }
        ShippingAddressResponse shippingAddress = new ShippingAddressResponse(
                order.getShippingAddressLine1(),
                order.getShippingAddressLine2(),
                order.getShippingCity(),
                order.getShippingState(),
                order.getShippingPostalCode(),
                order.getShippingCountry()
        );

        List<OrderItemResponse> itemResponses = order.getItems() != null
                ? order.getItems().stream().map(orderItemMapper::toResponse).toList()
                : List.of();

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getUserId(),
                order.getStatus(),
                order.getCurrency(),
                order.getPaymentMethod(),
                order.getSubtotal(),
                order.getShippingAmount(),
                order.getTaxAmount(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                shippingAddress,
                itemResponses,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
