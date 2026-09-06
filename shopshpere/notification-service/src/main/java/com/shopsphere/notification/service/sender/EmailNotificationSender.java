package com.shopsphere.notification.service.sender;

import com.shopsphere.notification.exception.NotificationSendingException;
import com.shopsphere.notification.model.entity.Notification;
import com.shopsphere.notification.model.entity.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationSender.class);

    @Override
    public NotificationChannel supportedChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(Notification notification, boolean simulateFailure) {
        log.info("[EMAIL] Simulating EMAIL notification to {} | Subject: '{}' | Type: {}",
                notification.getRecipient(),
                notification.getSubject(),
                notification.getType());

        if (simulateFailure) {
            log.warn("[EMAIL] Simulated failure for recipient: {}", notification.getRecipient());
            throw new NotificationSendingException(
                    "Simulated EMAIL delivery failure for recipient: " + notification.getRecipient());
        }

        log.info("[EMAIL] Successfully simulated EMAIL delivery to {}", notification.getRecipient());
    }
}
