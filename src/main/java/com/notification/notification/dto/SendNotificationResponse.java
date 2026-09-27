package com.notification.notification.dto;

import com.notification.notification.domain.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationResponse {

    private String correlationId;
    private String notificationId;
    private NotificationStatus status;
    private LocalDateTime timestamp;
}
