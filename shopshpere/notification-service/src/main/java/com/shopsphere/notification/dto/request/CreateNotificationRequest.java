package com.shopsphere.notification.dto.request;

import com.shopsphere.notification.model.entity.NotificationChannel;
import com.shopsphere.notification.model.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateNotificationRequest(
    @NotNull(message = "User ID is required")
    Long userId,

    @NotNull(message = "Notification type is required")
    NotificationType type,

    @NotNull(message = "Notification channel is required")
    NotificationChannel channel,

    @NotBlank(message = "Recipient is required")
    String recipient,

    String subject,

    @NotBlank(message = "Message is required")
    String message,

    Boolean simulateFailure
) {}
