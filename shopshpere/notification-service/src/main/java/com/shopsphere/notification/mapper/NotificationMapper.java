package com.shopsphere.notification.mapper;

import com.shopsphere.notification.dto.request.CreateNotificationRequest;
import com.shopsphere.notification.dto.response.NotificationResponse;
import com.shopsphere.notification.model.entity.Notification;
import com.shopsphere.notification.model.entity.NotificationStatus;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public Notification toEntity(CreateNotificationRequest request) {
        if (request == null) return null;
        return Notification.builder()
                .userId(request.userId())
                .type(request.type())
                .channel(request.channel())
                .recipient(request.recipient())
                .subject(request.subject())
                .message(request.message())
                .status(NotificationStatus.PENDING)
                .build();
    }

    public NotificationResponse toResponse(Notification notification) {
        if (notification == null) return null;
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getType(),
                notification.getChannel(),
                notification.getRecipient(),
                notification.getSubject(),
                notification.getMessage(),
                notification.getStatus(),
                notification.getCreatedAt(),
                notification.getSentAt(),
                notification.getFailureReason()
        );
    }
}
