package com.shopsphere.order.client;

import com.shopsphere.order.client.dto.InventoryReservationResult;

/**
 * Outbound client boundary for the Inventory Service.
 */
public interface InventoryServiceClient {

    /**
     * Reserves {@code quantity} units of {@code productId} for {@code orderId}.
     *
     * @throws com.shopsphere.order.client.exception.RemoteServiceBadRequestException if stock is insufficient
     * @throws com.shopsphere.order.client.exception.RemoteResourceNotFoundException if the product has no inventory record
     * @throws com.shopsphere.order.client.exception.RemoteServiceException for any other remote failure
     */
    InventoryReservationResult reserve(Long productId, Long orderId, Integer quantity);

    /**
     * Releases a previously created reservation back to salable stock.
     * Used as compensation when a later step in checkout fails.
     */
    void release(Long reservationId, String reason);
}
