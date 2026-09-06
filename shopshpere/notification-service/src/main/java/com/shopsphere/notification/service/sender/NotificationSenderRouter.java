package com.shopsphere.notification.service.sender;

import com.shopsphere.notification.model.entity.Notification;

/**
 * Routes a notification to the appropriate channel-specific {@link NotificationSender} strategy.
 * <p>
 * This interface decouples the service layer from the concrete router implementation,
 * making it easy to mock in unit tests and to swap routing logic without touching business code.
 */
public interface NotificationSenderRouter {
    void route(Notification notification, boolean simulateFailure);
}
