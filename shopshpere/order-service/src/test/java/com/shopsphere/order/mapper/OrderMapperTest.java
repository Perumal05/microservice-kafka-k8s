package com.shopsphere.order.mapper;

import com.shopsphere.order.client.dto.ProductClientResponse;
import com.shopsphere.order.dto.request.CreateOrderItemRequest;
import com.shopsphere.order.dto.request.CreateOrderRequest;
import com.shopsphere.order.dto.request.ShippingAddressRequest;
import com.shopsphere.order.dto.response.OrderResponse;
import com.shopsphere.order.model.entity.Order;
import com.shopsphere.order.model.entity.OrderItem;
import com.shopsphere.order.model.entity.OrderStatus;
import com.shopsphere.order.model.entity.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderMapperTest {

    private OrderItemMapper orderItemMapper;
    private OrderMapper orderMapper;

    @BeforeEach
    void setUp() {
        orderItemMapper = new OrderItemMapper();
        orderMapper = new OrderMapper(orderItemMapper);
    }

    @Test
    void toEntity_ShouldMapCreateOrderRequestToOrder() {
        ShippingAddressRequest addressReq = new ShippingAddressRequest(
                "123 Main St", "Apt 4B", "Chennai", "Tamil Nadu", "600001", "India");
        CreateOrderItemRequest itemReq = new CreateOrderItemRequest(10L, 1);
        CreateOrderRequest request = new CreateOrderRequest(
                100L, "USD", PaymentMethod.CARD, List.of(itemReq), addressReq);

        ProductClientResponse product = new ProductClientResponse(10L, "SKU-100", "Laptop", new BigDecimal("750.00"), "USD", "ACTIVE");
        OrderItem item = orderItemMapper.toEntity(itemReq, product);
        Order order = orderMapper.toEntity(request, "ORD-20260809-123456", new BigDecimal("750.00"), new BigDecimal("750.00"), List.of(item));

        assertNotNull(order);
        assertEquals("ORD-20260809-123456", order.getOrderNumber());
        assertEquals(100L, order.getUserId());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(PaymentMethod.CARD, order.getPaymentMethod());
        assertEquals(new BigDecimal("750.00"), order.getSubtotal());
        assertEquals(BigDecimal.ZERO, order.getShippingAmount());
        assertEquals(new BigDecimal("750.00"), order.getTotalAmount());
        assertEquals("123 Main St", order.getShippingAddressLine1());
        assertEquals("Chennai", order.getShippingCity());
    }

    @Test
    void toResponse_ShouldMapOrderToOrderResponse() {
        Instant now = Instant.now();
        OrderItem item = OrderItem.builder()
                .id(1L)
                .productId(10L)
                .productSku("SKU-100")
                .productName("Laptop")
                .quantity(1)
                .unitPrice(new BigDecimal("750.00"))
                .totalPrice(new BigDecimal("750.00"))
                .build();

        Order order = Order.builder()
                .id(5L)
                .orderNumber("ORD-20260809-123456")
                .userId(100L)
                .status(OrderStatus.PENDING)
                .currency("USD")
                .paymentMethod(PaymentMethod.CARD)
                .subtotal(new BigDecimal("750.00"))
                .shippingAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(new BigDecimal("750.00"))
                .shippingAddressLine1("123 Main St")
                .shippingCity("Chennai")
                .shippingState("TN")
                .shippingPostalCode("600001")
                .shippingCountry("India")
                .createdAt(now)
                .updatedAt(now)
                .items(List.of(item))
                .build();

        OrderResponse response = orderMapper.toResponse(order);

        assertNotNull(response);
        assertEquals("ORD-20260809-123456", response.orderNumber());
        assertEquals(100L, response.userId());
        assertEquals(PaymentMethod.CARD, response.paymentMethod());
        assertEquals("123 Main St", response.shippingAddress().addressLine1());
        assertEquals(1, response.items().size());
    }
}
