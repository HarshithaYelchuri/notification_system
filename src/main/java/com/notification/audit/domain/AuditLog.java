package com.notification.audit.domain;

import com.notification.notification.domain.NotificationStatus;
import com.notification.notification.domain.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "audit_logs")
@CompoundIndexes({
    @CompoundIndex(name = "tenant_audit_notification_idx", def = "{'tenantId': 1, 'notificationId': 1}"),
    @CompoundIndex(name = "tenant_audit_timestamp_idx", def = "{'tenantId': 1, 'timestamp': -1}")
})
public class AuditLog {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    @Indexed
    private String notificationId;

    @Indexed
    private String correlationId;

    private NotificationType notificationType;

    private NotificationStatus status;

    private String recipient;

    private String details;

    private String errorMessage;

    private int retryCount;

    private LocalDateTime timestamp;
}
