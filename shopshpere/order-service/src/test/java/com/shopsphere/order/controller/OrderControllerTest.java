package com.shopsphere.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsphere.order.client.ProductServiceClient;
import com.shopsphere.order.client.dto.ProductClientResponse;
import com.shopsphere.order.client.exception.RemoteResourceNotFoundException;
import com.shopsphere.order.dto.request.CreateOrderItemRequest;
import com.shopsphere.order.dto.request.CreateOrderRequest;
import com.shopsphere.order.dto.request.ShippingAddressRequest;
import com.shopsphere.order.dto.request.UpdateOrderStatusRequest;
import com.shopsphere.order.event.producer.OrderEventProducer;
import com.shopsphere.order.model.entity.OrderStatus;
import com.shopsphere.order.model.entity.PaymentMethod;
import com.shopsphere.order.repository.OrderItemRepository;
import com.shopsphere.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * As of Stage 5, {@code POST /api/orders} only performs synchronous product
 * validation and persists the order as PENDING before returning - it no
 * longer waits for inventory reservation or payment (those now happen
 * asynchronously via Kafka; see {@code InventoryEventConsumer}/
 * {@code PaymentEventConsumer}). {@link OrderEventProducer} is mocked here
 * so this controller test exercises real HTTP + DB wiring without requiring
 * a running Kafka broker.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @MockBean
    private ProductServiceClient productServiceClient;

    @MockBean
    private OrderEventProducer orderEventProducer;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();

        when(productServiceClient.getProduct(10L))
                .thenReturn(new ProductClientResponse(10L, "SKU-LAPTOP", "Gaming Laptop", new BigDecimal("1200.00"), "USD", "ACTIVE"));
    }

    private CreateOrderRequest singleItemRequest() {
        ShippingAddressRequest addressReq = new ShippingAddressRequest(
                "123 Main St", "Suite 100", "Chennai", "Tamil Nadu", "600001", "India");
        CreateOrderItemRequest item1 = new CreateOrderItemRequest(10L, 1);
        return new CreateOrderRequest(100L, "USD", PaymentMethod.CARD, List.of(item1), addressReq);
    }

    @Test
    void createOrder_ValidRequest_ShouldReturn201CreatedWithPendingStatusAndPublishEvent() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(singleItemRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.orderNumber", startsWith("ORD-")))
                .andExpect(jsonPath("$.status", is("PENDING")))
                .andExpect(jsonPath("$.subtotal", is(1200.00)))
                .andExpect(jsonPath("$.totalAmount", is(1200.00)))
                .andExpect(jsonPath("$.items", hasSize(1)));

        verify(orderEventProducer, times(1)).publishOrderCreated(any());
    }

    @Test
    void createOrder_ValidationFailure_ShouldReturn400BadRequest() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(null, "", null, List.of(), null);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("VALIDATION_ERROR")));

        verify(orderEventProducer, times(0)).publishOrderCreated(any());
    }

    @Test
    void createOrder_ProductNotFound_ShouldReturn404AndNotPersistOrderOrPublishEvent() throws Exception {
        when(productServiceClient.getProduct(10L))
                .thenThrow(new RemoteResourceNotFoundException("Product Service", "Product not found with id: 10"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(singleItemRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("REMOTE_RESOURCE_NOT_FOUND")));

        assertOrderCountIs(0);
        verify(orderEventProducer, times(0)).publishOrderCreated(any());
    }

    @Test
    void getOrderById_ShouldReturnOrder() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(singleItemRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        Long orderId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/orders/{orderId}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(orderId.intValue())))
                .andExpect(jsonPath("$.subtotal", is(1200.00)));
    }

    @Test
    void updateOrderStatus_PendingToPaymentPending_ShouldReturn200OK() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(singleItemRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        Long orderId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        // This simulates what InventoryEventConsumer does when it receives InventoryReserved.
        UpdateOrderStatusRequest updateReq = new UpdateOrderStatusRequest(OrderStatus.PAYMENT_PENDING);

        mockMvc.perform(patch("/api/orders/{orderId}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAYMENT_PENDING")));
    }

    @Test
    void cancelOrder_PendingOrder_ShouldReturn200OK() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(singleItemRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        Long orderId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/orders/{orderId}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    private void assertOrderCountIs(long expected) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, orderRepository.count());
    }
}
