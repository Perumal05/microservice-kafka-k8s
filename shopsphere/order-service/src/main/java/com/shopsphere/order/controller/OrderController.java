package com.shopsphere.order.controller;

import com.shopsphere.order.dto.request.CreateOrderRequest;
import com.shopsphere.order.dto.request.UpdateOrderStatusRequest;
import com.shopsphere.order.dto.response.ErrorResponse;
import com.shopsphere.order.dto.response.OrderResponse;
import com.shopsphere.order.dto.response.PageResponse;
import com.shopsphere.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order Controller", description = "APIs for managing customer orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(
            summary = "Create a new order (asynchronous checkout, Stage 5)",
            description = """
                    Validates each item against the Product Service (still synchronous REST),
                    then persists the order as PENDING and publishes an OrderCreated event to
                    Kafka. Inventory reservation and payment now happen asynchronously in
                    response to that event - this call returns as soon as the order is
                    persisted, while it is still PENDING.

                    Poll GET /api/orders/{id} to observe the order progress to
                    PAYMENT_PENDING (inventory reserved) and then PAID, or to FAILED if
                    inventory reservation or payment failed asynchronously.

                    This is NOT a distributed transaction, and the order persist + Kafka
                    publish are not atomic (see project documentation for the dual-write
                    limitation).
                    """,
            responses = {
                    @ApiResponse(responseCode = "201", description = "Order accepted and persisted as PENDING; processing continues asynchronously"),
                    @ApiResponse(responseCode = "400", description = "Invalid request body, or the Product Service rejected the request",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "A referenced product does not exist",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "502", description = "The Product Service returned an unexpected error",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "503", description = "The Product Service is unreachable",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "504", description = "The Product Service call timed out",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by internal ID")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long orderId) {
        OrderResponse response = orderService.getOrderById(orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/number/{orderNumber}")
    @Operation(summary = "Get order by human-readable order number")
    public ResponseEntity<OrderResponse> getOrderByOrderNumber(@PathVariable String orderNumber) {
        OrderResponse response = orderService.getOrderByOrderNumber(orderNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get paginated orders for a specific user")
    public ResponseEntity<PageResponse<OrderResponse>> getUserOrders(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        PageResponse<OrderResponse> response = orderService.getUserOrders(userId, page, size, sort);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{orderId}/status")
    @Operation(summary = "Update order status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderResponse response = orderService.updateOrderStatus(orderId, request.status());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long orderId) {
        OrderResponse response = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(response);
    }
}
