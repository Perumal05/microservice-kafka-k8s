package com.shopsphere.order.client.exception;

/**
 * Thrown when a request to a remote service exceeds the configured
 * connect/read timeout. Note that we deliberately do NOT retry automatically
 * when this occurs (see stage 3 design notes) since a blind retry after a
 * timeout could cause duplicate side effects (e.g. duplicate payments).
 */
public class RemoteServiceTimeoutException extends RemoteServiceException {
    public RemoteServiceTimeoutException(String serviceName, String message, Throwable cause) {
        super(serviceName, message, cause);
    }
}
