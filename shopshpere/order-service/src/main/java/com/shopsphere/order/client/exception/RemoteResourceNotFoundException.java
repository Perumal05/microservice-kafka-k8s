package com.shopsphere.order.client.exception;

/**
 * Thrown when a remote service responds with HTTP 404 for a requested
 * resource (e.g. a product ID that does not exist in the Product Service).
 */
public class RemoteResourceNotFoundException extends RemoteServiceException {
    public RemoteResourceNotFoundException(String serviceName, String message) {
        super(serviceName, 404, message, null);
    }
}
