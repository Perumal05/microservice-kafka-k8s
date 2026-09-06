package com.shopsphere.notification.dto.response;

import com.shopsphere.notification.model.entity.NotificationChannel;
import com.shopsphere.notification.model.entity.NotificationStatus;
import com.shopsphere.notification.model.entity.NotificationType;

import java.time.Instant;

public record NotificationResponse(
    Long id,
    Long userId,
    NotificationType type,
    NotificationChannel channel,
    String recipient,
    String subject,
    String message,
    NotificationStatus status,
    Instant createdAt,
    Instant sentAt,
    String failureReason
) {}
