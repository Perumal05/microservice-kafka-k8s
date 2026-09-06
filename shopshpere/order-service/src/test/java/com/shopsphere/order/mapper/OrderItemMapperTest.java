package com.shopsphere.order.mapper;

import com.shopsphere.order.client.dto.ProductClientResponse;
import com.shopsphere.order.dto.request.CreateOrderItemRequest;
import com.shopsphere.order.dto.response.OrderItemResponse;
import com.shopsphere.order.model.entity.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class OrderItemMapperTest {

    private OrderItemMapper orderItemMapper;

    @BeforeEach
    void setUp() {
        orderItemMapper = new OrderItemMapper();
    }

    @Test
    void toEntity_ShouldMapUsingAuthoritativeProductData() {
        CreateOrderItemRequest request = new CreateOrderItemRequest(10L, 2);
        ProductClientResponse product = new ProductClientResponse(10L, "SKU-100", "Wireless Mouse", new BigDecimal("25.00"), "USD", "ACTIVE");

        OrderItem item = orderItemMapper.toEntity(request, product);

        assertNotNull(item);
        assertEquals(10L, item.getProductId());
        assertEquals("SKU-100", item.getProductSku());
        assertEquals("Wireless Mouse", item.getProductName());
        assertEquals(2, item.getQuantity());
        assertEquals(new BigDecimal("25.00"), item.getUnitPrice());
        assertEquals(new BigDecimal("50.00"), item.getTotalPrice());
    }

    @Test
    void toEntity_ShouldIgnoreClientSuppliedPriceEntirely_UsingOnlyProductServiceData() {
        // The request only ever carries productId + quantity - there is no client-supplied
        // price field at all, so authoritative pricing always comes from ProductClientResponse.
        CreateOrderItemRequest request = new CreateOrderItemRequest(20L, 3);
        ProductClientResponse product = new ProductClientResponse(20L, "SKU-200", "Keyboard", new BigDecimal("100.00"), "USD", "ACTIVE");

        OrderItem item = orderItemMapper.toEntity(request, product);

        assertEquals(new BigDecimal("100.00"), item.getUnitPrice());
        assertEquals(new BigDecimal("300.00"), item.getTotalPrice());
    }

    @Test
    void toResponse_ShouldMapOrderItemToOrderItemResponse() {
        Instant now = Instant.now();
        OrderItem item = OrderItem.builder()
                .id(1L)
                .productId(10L)
                .productSku("SKU-100")
                .productName("Wireless Mouse")
                .quantity(2)
                .unitPrice(new BigDecimal("25.00"))
                .totalPrice(new BigDecimal("50.00"))
                .createdAt(now)
                .build();

        OrderItemResponse response = orderItemMapper.toResponse(item);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(10L, response.productId());
        assertEquals(new BigDecimal("50.00"), response.totalPrice());
    }
}
