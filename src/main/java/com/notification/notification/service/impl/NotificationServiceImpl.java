package com.notification.notification.service.impl;

import com.notification.common.context.TenantContext;
import com.notification.common.dto.PageResponse;
import com.notification.common.exception.ResourceNotFoundException;
import com.notification.common.exception.ValidationException;
import com.notification.common.util.TemplateSubstitutor;
import com.notification.notification.domain.NotificationRecord;
import com.notification.notification.domain.NotificationStatus;
import com.notification.notification.dto.NotificationStatusResponse;
import com.notification.notification.dto.SendNotificationRequest;
import com.notification.notification.dto.SendNotificationResponse;
import com.notification.notification.repository.NotificationRecordRepository;
import com.notification.notification.service.NotificationService;
import com.notification.publisher.GenericPublisher;
import com.notification.publisher.event.NotificationAuditEvent;
import com.notification.publisher.event.NotificationIngestEvent;
import com.notification.template.cache.TemplateCacheService;
import com.notification.template.domain.NotificationTemplate;
import com.notification.template.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRecordRepository recordRepository;
    private final TemplateRepository templateRepository;
    private final TemplateCacheService templateCacheService;
    private final GenericPublisher genericPublisher;

    @Value("${app.kafka.topics.ingest:notification.ingest}")
    private String ingestTopic = "notification.ingest";

    @Value("${app.kafka.topics.audit:notification.audit}")
    private String auditTopic = "notification.audit";

    @Override
    public SendNotificationResponse sendNotification(SendNotificationRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        // 1. Resolve Template (Cache first -> Mongo fallback)
        NotificationTemplate template = templateCacheService.getTemplateById(tenantId, request.getTemplateId());
        if (template == null) {
            template = templateRepository.findByIdAndTenantId(request.getTemplateId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Template not found with ID: " + request.getTemplateId() + " for tenant: " + tenantId));
            templateCacheService.cacheTemplate(template);
        }

        // 2. Validate Dynamic Variables against Template Variables
        Set<String> declaredVariables = template.getVariables();
        Map<String, Object> suppliedVariables = request.getDynamicVariables();

        if (declaredVariables != null && !declaredVariables.isEmpty()) {
            for (String requiredVar : declaredVariables) {
                if (suppliedVariables == null || !suppliedVariables.containsKey(requiredVar) || suppliedVariables.get(requiredVar) == null) {
                    throw new ValidationException("Missing required variable for template: " + requiredVar);
                }
            }
        }

        // 3. Persist Notification Record in MongoDB
        NotificationRecord record = NotificationRecord.builder()
                .tenantId(tenantId)
                .correlationId(correlationId)
                .templateId(template.getId())
                .notificationType(request.getNotificationType())
                .status(NotificationStatus.INGESTED)
                .dynamicVariables(suppliedVariables)
                .recipient(request.getRecipient())
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        NotificationRecord savedRecord = recordRepository.save(record);

        // 4. Publish Ingestion Event to Kafka
        NotificationIngestEvent ingestEvent = NotificationIngestEvent.builder()
                .notificationId(savedRecord.getId())
                .correlationId(correlationId)
                .tenantId(tenantId)
                .templateId(template.getId())
                .notificationType(request.getNotificationType())
                .recipient(request.getRecipient())
                .dynamicVariables(suppliedVariables)
                .timestamp(LocalDateTime.now())
                .build();

        Map<String, String> headers = Map.of(
                "X-Tenant-Id", tenantId,
                "X-Correlation-Id", correlationId
        );

        genericPublisher.publish(ingestTopic, savedRecord.getId(), ingestEvent, headers);

        // 5. Publish Audit Event
        NotificationAuditEvent auditEvent = NotificationAuditEvent.builder()
                .notificationId(savedRecord.getId())
                .correlationId(correlationId)
                .tenantId(tenantId)
                .templateId(template.getId())
                .notificationType(request.getNotificationType())
                .status(NotificationStatus.INGESTED)
                .recipient(request.getRecipient())
                .details("Notification ingested and queued for processing")
                .retryCount(0)
                .timestamp(LocalDateTime.now())
                .build();

        genericPublisher.publish(auditTopic, savedRecord.getId(), auditEvent, headers);

        log.info("Ingested notification [id={}, correlationId={}] for tenant [{}]", savedRecord.getId(), correlationId, tenantId);

        return SendNotificationResponse.builder()
                .correlationId(correlationId)
                .notificationId(savedRecord.getId())
                .status(NotificationStatus.INGESTED)
                .timestamp(savedRecord.getCreatedAt())
                .build();
    }

    @Override
    public NotificationStatusResponse getNotificationStatus(String id) {
        String tenantId = TenantContext.getRequiredTenantId();
        NotificationRecord record = recordRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification record not found with ID: " + id));
        return NotificationStatusResponse.fromEntity(record);
    }

    @Override
    public NotificationStatusResponse getNotificationStatusByCorrelationId(String correlationId) {
        String tenantId = TenantContext.getRequiredTenantId();
        NotificationRecord record = recordRepository.findByCorrelationIdAndTenantId(correlationId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification record not found with correlation ID: " + correlationId));
        return NotificationStatusResponse.fromEntity(record);
    }

    @Override
    public PageResponse<NotificationStatusResponse> searchNotifications(int page, int size) {
        String tenantId = TenantContext.getRequiredTenantId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<NotificationRecord> pageRecords = recordRepository.findByTenantId(tenantId, pageable);
        return PageResponse.from(pageRecords.map(NotificationStatusResponse::fromEntity));
    }
}
