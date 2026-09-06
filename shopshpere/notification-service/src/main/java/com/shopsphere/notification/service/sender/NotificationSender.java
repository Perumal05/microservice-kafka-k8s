package com.shopsphere.notification.service.sender;

import com.shopsphere.notification.model.entity.Notification;
import com.shopsphere.notification.model.entity.NotificationChannel;

/**
 * Strategy interface for simulated notification delivery.
 * Implement per channel (EMAIL, SMS, PUSH).
 * <p>
 * Future Kafka integration note: when a Kafka consumer is introduced,
 * it will call NotificationService which calls this sender — no changes needed here.
 */
public interface NotificationSender {

    /**
     * The channel this sender handles.
     */
    NotificationChannel supportedChannel();

    /**
     * Simulate sending the notification.
     *
     * @param notification the notification to send
     * @param simulateFailure if true, the send is intentionally failed for testing
     */
    void send(Notification notification, boolean simulateFailure);
}
