package com.shopsphere.order.service;

import com.shopsphere.order.dto.request.CreateOrderRequest;
import com.shopsphere.order.dto.response.OrderResponse;
import com.shopsphere.order.dto.response.PageResponse;
import com.shopsphere.order.model.entity.OrderStatus;

public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request);
    OrderResponse getOrderById(Long orderId);
    OrderResponse getOrderByOrderNumber(String orderNumber);
    PageResponse<OrderResponse> getUserOrders(Long userId, int page, int size, String sort);
    OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus);
    OrderResponse cancelOrder(Long orderId);
}
