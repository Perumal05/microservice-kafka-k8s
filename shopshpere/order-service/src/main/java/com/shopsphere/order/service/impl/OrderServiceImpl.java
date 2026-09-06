package com.shopsphere.order.service.impl;

import com.shopsphere.order.client.ProductServiceClient;
import com.shopsphere.order.client.dto.ProductClientResponse;
import com.shopsphere.order.dto.request.CreateOrderItemRequest;
import com.shopsphere.order.dto.request.CreateOrderRequest;
import com.shopsphere.order.dto.response.OrderResponse;
import com.shopsphere.order.dto.response.PageResponse;
import com.shopsphere.order.event.OrderCreatedEvent;
import com.shopsphere.order.event.OrderCreatedEventItem;
import com.shopsphere.order.event.producer.OrderEventProducer;
import com.shopsphere.order.exception.BadRequestException;
import com.shopsphere.order.exception.ResourceNotFoundException;
import com.shopsphere.order.filter.CorrelationIdHolder;
import com.shopsphere.order.mapper.OrderItemMapper;
import com.shopsphere.order.mapper.OrderMapper;
import com.shopsphere.order.model.entity.Order;
import com.shopsphere.order.model.entity.OrderItem;
import com.shopsphere.order.model.entity.OrderStatus;
import com.shopsphere.order.repository.OrderRepository;
import com.shopsphere.order.service.OrderService;
import com.shopsphere.order.service.OrderStatusTransitionValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Order Service business logic.
 *
 * <h2>Stage 5: event-driven order processing</h2>
 * As of Stage 5, {@link #createOrder(CreateOrderRequest)} no longer calls
 * Inventory Service or Payment Service synchronously over REST. Product
 * validation remains synchronous (Order Service still needs authoritative
 * price/SKU/name before it can persist an order at all), but once the order
 * is persisted as PENDING, an {@link OrderCreatedEvent} is published to
 * Kafka and the method returns immediately - the order is still PENDING at
 * that point. Inventory reservation and payment now happen asynchronously:
 *
 * <pre>
 * createOrder()
 *   -&gt; validate products (REST, still synchronous)
 *   -&gt; persist order (PENDING)
 *   -&gt; publish OrderCreatedEvent
 *   -&gt; return (order is still PENDING here)
 *
 * (asynchronously, via Kafka)
 * Inventory Service   -&gt; InventoryReserved / InventoryReservationFailed
 * Order Service consumes that -&gt; PAYMENT_PENDING / FAILED
 * Payment Service     -&gt; PaymentSucceeded / PaymentFailed
 * Order Service consumes that -&gt; PAID / FAILED
 * </pre>
 *
 * See {@link com.shopsphere.order.event.consumer.InventoryEventConsumer} and
 * {@link com.shopsphere.order.event.consumer.PaymentEventConsumer} for the
 * consumer side of this flow.
 * <p>
 * <b>Dual-write limitation (intentional, not yet solved):</b> persisting the
 * order and publishing {@code OrderCreatedEvent} are two separate systems
 * (the local database and Kafka) and are not atomic. If the process crashes
 * or Kafka is unreachable between the DB commit and the publish call, the
 * order exists as PENDING forever with no event ever emitted. This will be
 * solved with the Outbox pattern in a later stage - see
 * {@link OrderEventProducer} for where this gap lives in code.
 * <p>
 * The REST clients for Inventory/Payment/Cart (see {@code com.shopsphere.order.client})
 * are intentionally left in place and still covered by their own tests, even
 * though this class no longer calls them - removing tested, working code
 * that may still be useful for a future Saga/compensation stage was judged
 * out of scope for this change.
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final OrderItemMapper orderItemMapper;
    private final OrderMapper orderMapper;
    private final OrderStatusTransitionValidator transitionValidator;
    private final ProductServiceClient productServiceClient;
    private final OrderEventProducer orderEventProducer;

    public OrderServiceImpl(OrderRepository orderRepository,
                            OrderItemMapper orderItemMapper,
                            OrderMapper orderMapper,
                            OrderStatusTransitionValidator transitionValidator,
                            ProductServiceClient productServiceClient,
                            OrderEventProducer orderEventProducer) {
        this.orderRepository = orderRepository;
        this.orderItemMapper = orderItemMapper;
        this.orderMapper = orderMapper;
        this.transitionValidator = transitionValidator;
        this.productServiceClient = productServiceClient;
        this.orderEventProducer = orderEventProducer;
    }

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        // 1. Validate every requested product against the Product Service and resolve
        //    authoritative price/SKU/name. No order is persisted if this step fails.
        //    Product Service remains synchronous REST in Stage 5 - see class Javadoc.
        List<OrderItem> items = resolveOrderItems(request.items());

        BigDecimal subtotal = items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shippingAmount = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = subtotal.add(shippingAmount).add(taxAmount).subtract(discountAmount);

        // 2. Persist the order locally as PENDING. This is a plain local transaction.
        Order savedOrder = persistPendingOrder(request, items, subtotal, totalAmount);

        // 3. Publish OrderCreatedEvent. Inventory reservation and payment now happen
        //    asynchronously in response to this event - see class Javadoc for the full
        //    flow and the dual-write limitation this step carries.
        publishOrderCreated(savedOrder);

        // The order is still PENDING when this method returns - the caller (and the
        // frontend) must poll GET /api/orders/{id} to observe it progress to
        // PAYMENT_PENDING, then PAID or FAILED.
        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with order number: " + orderNumber));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getUserOrders(Long userId, int page, int size, String sort) {
        Pageable pageable = createPageable(page, size, sort);
        Page<OrderResponse> orderPage = orderRepository.findByUserId(userId, pageable)
                .map(orderMapper::toResponse);
        return PageResponse.from(orderPage);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus) {
        return orderMapper.toResponse(changeStatus(orderId, newStatus));
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        transitionValidator.validateCancellation(order.getStatus());

        order.setStatus(OrderStatus.CANCELLED);
        Order cancelledOrder = orderRepository.save(order);
        return orderMapper.toResponse(cancelledOrder);
    }

    // --- Order creation helpers ---------------------------------------------------

    private List<OrderItem> resolveOrderItems(List<CreateOrderItemRequest> itemRequests) {
        return itemRequests.stream().map(this::resolveOrderItem).toList();
    }

    private OrderItem resolveOrderItem(CreateOrderItemRequest itemRequest) {
        ProductClientResponse product = productServiceClient.getProduct(itemRequest.productId());
        if (!product.isActive()) {
            throw new BadRequestException(
                    "Product " + product.id() + " is not available for purchase (status=" + product.status() + ")");
        }
        return orderItemMapper.toEntity(itemRequest, product);
    }

    private Order persistPendingOrder(CreateOrderRequest request, List<OrderItem> items, BigDecimal subtotal, BigDecimal totalAmount) {
        String orderNumber = generateOrderNumber();
        Order order = orderMapper.toEntity(request, orderNumber, subtotal, totalAmount, null);
        Order savedOrder = orderRepository.save(order);
        final Long createdOrderId = savedOrder.getId();
        items.forEach(item -> item.setOrderId(createdOrderId));
        savedOrder.getItems().addAll(items);
        return orderRepository.save(savedOrder);
    }

    private void publishOrderCreated(Order order) {
        List<OrderCreatedEventItem> eventItems = order.getItems().stream()
                .map(item -> new OrderCreatedEventItem(item.getProductId(), item.getProductSku(), item.getQuantity(), item.getUnitPrice()))
                .toList();

        String correlationId = CorrelationIdHolder.get();
        OrderCreatedEvent event = OrderCreatedEvent.of(
                correlationId, order.getId(), order.getOrderNumber(), order.getUserId(),
                order.getCurrency(), order.getTotalAmount(), order.getPaymentMethod(), eventItems);

        orderEventProducer.publishOrderCreated(event);
    }

    // --- Status transition helpers --------------------------------------------------

    /**
     * Applies a validated status transition and persists it as its own local
     * transaction. Same-status transitions are a no-op (see
     * {@code OrderStatusTransitionValidator}), which provides a small amount of
     * protection against Kafka's at-least-once delivery causing an event to be
     * processed twice - not a full idempotency solution, just enough to avoid an
     * obvious duplicate-processing error. See Stage 5 documentation for the
     * broader idempotency limitation this implies.
     */
    private Order changeStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        transitionValidator.validateTransition(order.getStatus(), newStatus);
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    private String generateOrderNumber() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "ORD-" + datePrefix + "-" + randomSuffix;
    }

    private Pageable createPageable(int page, int size, String sort) {
        if (sort == null || sort.isBlank()) {
            return PageRequest.of(page, size, Sort.by("createdAt").descending());
        }
        String[] sortParams = sort.split(",");
        String property = sortParams[0].trim();
        Sort.Direction direction = (sortParams.length > 1 && sortParams[1].trim().equalsIgnoreCase("asc"))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return PageRequest.of(page, size, Sort.by(direction, property));
    }
}
