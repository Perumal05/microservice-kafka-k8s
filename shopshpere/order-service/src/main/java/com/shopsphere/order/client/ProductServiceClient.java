package com.shopsphere.order.client;

import com.shopsphere.order.client.dto.ProductClientResponse;

/**
 * Outbound client boundary for the Product Service.
 * <p>
 * Kept as an interface (with an HTTP-backed implementation) so that
 * business logic in {@code OrderService} never depends on RestClient
 * directly, and so that an alternative implementation (e.g. an
 * event-driven/cached one backed by Kafka) can be substituted later
 * without changing any calling code.
 */
public interface ProductServiceClient {

    /**
     * Retrieves authoritative product data for the given product ID.
     *
     * @throws com.shopsphere.order.client.exception.RemoteResourceNotFoundException if the product does not exist
     * @throws com.shopsphere.order.client.exception.RemoteServiceException for any other remote failure
     */
    ProductClientResponse getProduct(Long productId);
}
