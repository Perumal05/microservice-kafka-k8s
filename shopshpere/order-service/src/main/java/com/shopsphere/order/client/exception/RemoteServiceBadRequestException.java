package com.shopsphere.order.client.exception;

/**
 * Thrown when a remote service rejects a request as invalid (HTTP 400, or
 * other 4xx business-rule rejections such as insufficient inventory).
 */
public class RemoteServiceBadRequestException extends RemoteServiceException {
    public RemoteServiceBadRequestException(String serviceName, Integer statusCode, String message) {
        super(serviceName, statusCode, message, null);
    }
}
