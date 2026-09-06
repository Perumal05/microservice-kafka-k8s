package com.shopsphere.notification.service.sender;

import com.shopsphere.notification.exception.NotificationSendingException;
import com.shopsphere.notification.model.entity.Notification;
import com.shopsphere.notification.model.entity.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class PushNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationSender.class);

    @Override
    public NotificationChannel supportedChannel() {
        return NotificationChannel.PUSH;
    }

    @Override
    public void send(Notification notification, boolean simulateFailure) {
        log.info("[PUSH] Simulating PUSH notification to device token {} | Type: {}",
                notification.getRecipient(),
                notification.getType());

        if (simulateFailure) {
            log.warn("[PUSH] Simulated failure for device token: {}", notification.getRecipient());
            throw new NotificationSendingException(
                    "Simulated PUSH delivery failure for device token: " + notification.getRecipient());
        }

        log.info("[PUSH] Successfully simulated PUSH delivery to {}", notification.getRecipient());
    }
}
