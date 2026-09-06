package com.shopsphere.notification.service;

import com.shopsphere.notification.dto.request.CreateNotificationRequest;
import com.shopsphere.notification.dto.response.NotificationResponse;
import com.shopsphere.notification.dto.response.PageResponse;

public interface NotificationService {

    NotificationResponse createNotification(CreateNotificationRequest request);

    NotificationResponse getNotificationById(Long notificationId);

    PageResponse<NotificationResponse> getNotificationsByUserId(Long userId, int page, int size);

    NotificationResponse sendNotification(Long notificationId, boolean simulateFailure);

    NotificationResponse cancelNotification(Long notificationId);
}
