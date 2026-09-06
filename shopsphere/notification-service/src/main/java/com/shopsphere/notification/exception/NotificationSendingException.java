package com.shopsphere.notification.exception;

public class NotificationSendingException extends RuntimeException {
    public NotificationSendingException(String message) {
        super(message);
    }

    public NotificationSendingException(String message, Throwable cause) {
        super(message, cause);
    }
}
