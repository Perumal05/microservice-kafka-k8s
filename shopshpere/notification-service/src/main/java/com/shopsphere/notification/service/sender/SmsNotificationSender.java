package com.shopsphere.notification.service.sender;

import com.shopsphere.notification.exception.NotificationSendingException;
import com.shopsphere.notification.model.entity.Notification;
import com.shopsphere.notification.model.entity.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SmsNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationSender.class);

    @Override
    public NotificationChannel supportedChannel() {
        return NotificationChannel.SMS;
    }

    @Override
    public void send(Notification notification, boolean simulateFailure) {
        log.info("[SMS] Simulating SMS notification to {} | Type: {}",
                notification.getRecipient(),
                notification.getType());

        if (simulateFailure) {
            log.warn("[SMS] Simulated failure for recipient: {}", notification.getRecipient());
            throw new NotificationSendingException(
                    "Simulated SMS delivery failure for recipient: " + notification.getRecipient());
        }

        log.info("[SMS] Successfully simulated SMS delivery to {}", notification.getRecipient());
    }
}
