package com.notification.notification.dto;

import com.notification.notification.domain.NotificationRecord;
import com.notification.notification.domain.NotificationStatus;
import com.notification.notification.domain.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatusResponse {

    private String notificationId;
    private String correlationId;
    private String tenantId;
    private String templateId;
    private NotificationType notificationType;
    private NotificationStatus status;
    private String recipient;
    private int retryCount;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static NotificationStatusResponse fromEntity(NotificationRecord entity) {
        return NotificationStatusResponse.builder()
                .notificationId(entity.getId())
                .correlationId(entity.getCorrelationId())
                .tenantId(entity.getTenantId())
                .templateId(entity.getTemplateId())
                .notificationType(entity.getNotificationType())
                .status(entity.getStatus())
                .recipient(entity.getRecipient())
                .retryCount(entity.getRetryCount())
                .errorMessage(entity.getErrorMessage())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
