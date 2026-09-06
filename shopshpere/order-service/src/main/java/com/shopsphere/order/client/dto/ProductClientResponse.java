package com.shopsphere.order.client.dto;

import java.math.BigDecimal;

/**
 * Order Service's own view of the data it needs from the Product Service.
 * <p>
 * This is intentionally a separate type from any Product Service entity or
 * response DTO - each microservice owns its data contracts independently.
 * Only the fields Order Service actually needs are modeled here.
 */
public record ProductClientResponse(
    Long id,
    String sku,
    String name,
    BigDecimal price,
    String currency,
    String status
) {
    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}
