package com.shopsphere.order.mapper;

import com.shopsphere.order.client.dto.ProductClientResponse;
import com.shopsphere.order.dto.request.CreateOrderItemRequest;
import com.shopsphere.order.dto.response.OrderItemResponse;
import com.shopsphere.order.model.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OrderItemMapper {

    /**
     * Builds an {@link OrderItem} from the client's requested quantity and the
     * authoritative product data resolved from the Product Service. Price, SKU,
     * and name are always taken from {@code product}, never from the client.
     */
    public OrderItem toEntity(CreateOrderItemRequest request, ProductClientResponse product) {
        if (request == null || product == null) {
            return null;
        }
        BigDecimal unitPrice = product.price();
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(request.quantity()));
        return OrderItem.builder()
                .productId(product.id())
                .productSku(product.sku())
                .productName(product.name())
                .quantity(request.quantity())
                .unitPrice(unitPrice)
                .totalPrice(totalPrice)
                .build();
    }

    public OrderItemResponse toResponse(OrderItem item) {
        if (item == null) {
            return null;
        }
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductSku(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice(),
                item.getCreatedAt()
        );
    }
}
