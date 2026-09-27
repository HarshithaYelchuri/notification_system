package com.notification.notification.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notifications")
@CompoundIndexes({
    @CompoundIndex(name = "tenant_correlation_idx", def = "{'tenantId': 1, 'correlationId': 1}"),
    @CompoundIndex(name = "tenant_created_idx", def = "{'tenantId': 1, 'createdAt': -1}")
})
public class NotificationRecord {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    @Indexed
    private String correlationId;

    @Indexed
    private String templateId;

    private NotificationType notificationType;

    private NotificationStatus status;

    private Map<String, Object> dynamicVariables;

    private String recipient;

    private String renderedContent;

    private int retryCount;

    private String errorMessage;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
