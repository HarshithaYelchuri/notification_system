package com.notification.notification.service;

import com.notification.common.dto.PageResponse;
import com.notification.notification.dto.NotificationStatusResponse;
import com.notification.notification.dto.SendNotificationRequest;
import com.notification.notification.dto.SendNotificationResponse;

public interface NotificationService {

    SendNotificationResponse sendNotification(SendNotificationRequest request);

    NotificationStatusResponse getNotificationStatus(String id);

    NotificationStatusResponse getNotificationStatusByCorrelationId(String correlationId);

    PageResponse<NotificationStatusResponse> searchNotifications(int page, int size);
}
