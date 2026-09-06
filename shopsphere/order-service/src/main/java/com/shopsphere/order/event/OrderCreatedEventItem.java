package com.shopsphere.order.event;

import java.math.BigDecimal;

/**
 * A single order line item as carried inside {@link OrderCreatedEvent}.
 * Deliberately separate from the {@code OrderItem} JPA entity.
 */
public record OrderCreatedEventItem(
        Long productId,
        String sku,
        Integer quantity,
        BigDecimal unitPrice
) {
}
