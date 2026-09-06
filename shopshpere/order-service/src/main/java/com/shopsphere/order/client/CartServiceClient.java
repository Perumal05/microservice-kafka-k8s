package com.shopsphere.order.client;

/**
 * Outbound client boundary for the Cart Service.
 */
public interface CartServiceClient {

    /**
     * Clears the given user's cart. Called as a best-effort step after a
     * successful checkout; failures here must never invalidate an
     * otherwise-successful order (see {@code OrderServiceImpl#createOrder}).
     */
    void clearCart(Long userId);
}
