package com.shopsphere.order.service;

import com.shopsphere.order.client.ProductServiceClient;
import com.shopsphere.order.client.dto.ProductClientResponse;
import com.shopsphere.order.client.exception.RemoteResourceNotFoundException;
import com.shopsphere.order.dto.request.CreateOrderItemRequest;
import com.shopsphere.order.dto.request.CreateOrderRequest;
import com.shopsphere.order.dto.request.ShippingAddressRequest;
import com.shopsphere.order.dto.response.OrderResponse;
import com.shopsphere.order.dto.response.PageResponse;
import com.shopsphere.order.event.OrderCreatedEvent;
import com.shopsphere.order.event.producer.OrderEventProducer;
import com.shopsphere.order.exception.BadRequestException;
import com.shopsphere.order.exception.InvalidOrderStateException;
import com.shopsphere.order.exception.ResourceNotFoundException;
import com.shopsphere.order.mapper.OrderItemMapper;
import com.shopsphere.order.mapper.OrderMapper;
import com.shopsphere.order.model.entity.Order;
import com.shopsphere.order.model.entity.OrderStatus;
import com.shopsphere.order.model.entity.PaymentMethod;
import com.shopsphere.order.repository.OrderRepository;
import com.shopsphere.order.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * As of Stage 5, {@code OrderServiceImpl.createOrder()} only validates
 * products synchronously and publishes {@link OrderCreatedEvent} - it no
 * longer calls Inventory/Payment/Cart directly (see the class Javadoc on
 * {@code OrderServiceImpl}). Those REST clients are still exercised by
 * their own dedicated client tests; this test class covers order creation,
 * status transitions, and event publication only.
 */
class OrderServiceImplTest {

    private OrderRepository orderRepository;
    private OrderItemMapper orderItemMapper;
    private OrderMapper orderMapper;
    private OrderStatusTransitionValidator transitionValidator;
    private ProductServiceClient productServiceClient;
    private OrderEventProducer orderEventProducer;
    private OrderServiceImpl orderService;

    private final Map<Long, Order> db = new HashMap<>();
    private final AtomicLong idSeq = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        orderItemMapper = new OrderItemMapper();
        orderMapper = new OrderMapper(orderItemMapper);
        transitionValidator = new OrderStatusTransitionValidator();
        productServiceClient = mock(ProductServiceClient.class);
        orderEventProducer = mock(OrderEventProducer.class);

        orderService = new OrderServiceImpl(orderRepository, orderItemMapper, orderMapper, transitionValidator,
                productServiceClient, orderEventProducer);

        db.clear();
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(idSeq.getAndIncrement());
                order.setCreatedAt(Instant.now());
            }
            order.setUpdatedAt(Instant.now());
            db.put(order.getId(), order);
            return order;
        });
        when(orderRepository.findById(anyLong())).thenAnswer(invocation ->
                Optional.ofNullable(db.get((Long) invocation.getArgument(0))));
    }

    private CreateOrderRequest twoItemRequest() {
        ShippingAddressRequest addressReq = new ShippingAddressRequest("123 Main St", null, "Chennai", "TN", "600001", "India");
        CreateOrderItemRequest item1 = new CreateOrderItemRequest(10L, 2);
        CreateOrderItemRequest item2 = new CreateOrderItemRequest(20L, 1);
        return new CreateOrderRequest(100L, "USD", PaymentMethod.CARD, List.of(item1, item2), addressReq);
    }

    private ProductClientResponse activeProduct(Long id, String sku, BigDecimal price) {
        return new ProductClientResponse(id, sku, "Product-" + id, price, "USD", "ACTIVE");
    }

    // --- createOrder: product validation + persistence + event publication ---------------

    @Test
    void createOrder_ValidProducts_PersistsPendingOrderAndPublishesOrderCreated() {
        when(productServiceClient.getProduct(10L)).thenReturn(activeProduct(10L, "SKU-1", new BigDecimal("500.00")));
        when(productServiceClient.getProduct(20L)).thenReturn(activeProduct(20L, "SKU-2", new BigDecimal("50.00")));

        OrderResponse response = orderService.createOrder(twoItemRequest());

        // The order is PENDING when createOrder() returns - inventory reservation and
        // payment now happen asynchronously in response to the published event.
        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals(new BigDecimal("1050.00"), response.subtotal());
        assertEquals(2, response.items().size());

        ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(orderEventProducer, times(1)).publishOrderCreated(captor.capture());
        OrderCreatedEvent published = captor.getValue();
        assertEquals(response.id(), published.orderId());
        assertEquals(response.orderNumber(), published.orderNumber());
        assertEquals(100L, published.userId());
        assertEquals(2, published.items().size());
    }

    @Test
    void createOrder_EventMetadata_IsPopulatedCorrectly() {
        when(productServiceClient.getProduct(10L)).thenReturn(activeProduct(10L, "SKU-1", new BigDecimal("500.00")));
        when(productServiceClient.getProduct(20L)).thenReturn(activeProduct(20L, "SKU-2", new BigDecimal("50.00")));

        orderService.createOrder(twoItemRequest());

        ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(orderEventProducer).publishOrderCreated(captor.capture());
        OrderCreatedEvent event = captor.getValue();

        assertNotNull(event.eventId());
        assertEquals("OrderCreated", event.eventType());
        assertEquals(1, event.eventVersion());
        assertNotNull(event.occurredAt());
    }

    @Test
    void createOrder_ProductNotFound_OrderNotCreated_EventNotPublished() {
        when(productServiceClient.getProduct(10L)).thenThrow(new RemoteResourceNotFoundException("Product Service", "Product 10 not found"));

        assertThrows(RemoteResourceNotFoundException.class, () -> orderService.createOrder(twoItemRequest()));

        verify(orderRepository, never()).save(any(Order.class));
        verify(orderEventProducer, never()).publishOrderCreated(any());
    }

    @Test
    void createOrder_InactiveProduct_OrderNotCreated_EventNotPublished() {
        ProductClientResponse inactive = new ProductClientResponse(10L, "SKU-1", "Old Product", new BigDecimal("10.00"), "USD", "DISCONTINUED");
        when(productServiceClient.getProduct(10L)).thenReturn(inactive);
        when(productServiceClient.getProduct(20L)).thenReturn(activeProduct(20L, "SKU-2", new BigDecimal("50.00")));

        assertThrows(BadRequestException.class, () -> orderService.createOrder(twoItemRequest()));

        verify(orderRepository, never()).save(any(Order.class));
        verify(orderEventProducer, never()).publishOrderCreated(any());
    }

    // --- Existing (non-checkout) behavior ---------------------------------------------

    @Test
    void getOrderById_Success() {
        Order order = Order.builder()
                .id(1L).orderNumber("ORD-123").userId(100L).status(OrderStatus.PENDING)
                .currency("USD").paymentMethod(PaymentMethod.CARD)
                .subtotal(new BigDecimal("100.00")).totalAmount(new BigDecimal("100.00"))
                .shippingAddressLine1("Line 1").shippingCity("City").shippingState("State").shippingPostalCode("123").shippingCountry("Country")
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(1L);

        assertNotNull(response);
        assertEquals("ORD-123", response.orderNumber());
    }

    @Test
    void getOrderById_NotFound_ThrowsResourceNotFoundException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrderById(99L));
    }

    @Test
    void getUserOrders_Paginated_Success() {
        Order order = Order.builder()
                .id(1L).orderNumber("ORD-123").userId(100L).status(OrderStatus.PENDING)
                .currency("USD").paymentMethod(PaymentMethod.CARD)
                .subtotal(new BigDecimal("100.00")).totalAmount(new BigDecimal("100.00"))
                .shippingAddressLine1("Line 1").shippingCity("City").shippingState("State").shippingPostalCode("123").shippingCountry("Country")
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
        Page<Order> page = new PageImpl<>(List.of(order));

        when(orderRepository.findByUserId(eq(100L), any(Pageable.class))).thenReturn(page);

        PageResponse<OrderResponse> response = orderService.getUserOrders(100L, 0, 10, "createdAt,desc");

        assertEquals(1, response.totalElements());
        assertEquals("ORD-123", response.content().get(0).orderNumber());
    }

    // --- Status transitions (used by the Stage 5 Kafka event consumers) ------------------

    @Test
    void updateOrderStatus_PendingToPaymentPending_Success() {
        Order order = Order.builder().id(1L).status(OrderStatus.PENDING).currency("USD").paymentMethod(PaymentMethod.CARD)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        OrderResponse response = orderService.updateOrderStatus(1L, OrderStatus.PAYMENT_PENDING);

        assertEquals(OrderStatus.PAYMENT_PENDING, response.status());
    }

    @Test
    void updateOrderStatus_PaymentPendingToPaid_Success() {
        Order order = Order.builder().id(1L).status(OrderStatus.PAYMENT_PENDING).currency("USD").paymentMethod(PaymentMethod.CARD)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        OrderResponse response = orderService.updateOrderStatus(1L, OrderStatus.PAID);

        assertEquals(OrderStatus.PAID, response.status());
    }

    @Test
    void updateOrderStatus_PendingToFailed_Success() {
        Order order = Order.builder().id(1L).status(OrderStatus.PENDING).currency("USD").paymentMethod(PaymentMethod.CARD)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        OrderResponse response = orderService.updateOrderStatus(1L, OrderStatus.FAILED);

        assertEquals(OrderStatus.FAILED, response.status());
    }

    @Test
    void updateOrderStatus_InvalidTransition_ThrowsException() {
        Order order = Order.builder().id(1L).status(OrderStatus.SHIPPED).currency("USD").paymentMethod(PaymentMethod.CARD)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStateException.class, () -> orderService.updateOrderStatus(1L, OrderStatus.PENDING));
    }

    @Test
    void updateOrderStatus_SameStatus_IsNoOp() {
        Order order = Order.builder().id(1L).status(OrderStatus.PAID).currency("USD").paymentMethod(PaymentMethod.CARD)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        // Redelivering the same PaymentSucceeded event twice should not throw - this is the
        // minimal duplicate-processing safeguard described in OrderServiceImpl's Javadoc.
        OrderResponse response = orderService.updateOrderStatus(1L, OrderStatus.PAID);

        assertEquals(OrderStatus.PAID, response.status());
    }

    @Test
    void cancelOrder_PendingOrder_Success() {
        Order order = Order.builder().id(1L).status(OrderStatus.PENDING).currency("USD").paymentMethod(PaymentMethod.CARD)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        OrderResponse response = orderService.cancelOrder(1L);

        assertEquals(OrderStatus.CANCELLED, response.status());
    }

    @Test
    void cancelOrder_ShippedOrder_ThrowsException() {
        Order order = Order.builder().id(1L).status(OrderStatus.SHIPPED).currency("USD").paymentMethod(PaymentMethod.CARD)
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(InvalidOrderStateException.class, () -> orderService.cancelOrder(1L));
    }
}
