package com.notification.processor.consumer;

import com.notification.common.context.TenantContext;
import com.notification.common.exception.ResourceNotFoundException;
import com.notification.common.util.TemplateSubstitutor;
import com.notification.metrics.NotificationMetrics;
import com.notification.notification.domain.NotificationRecord;
import com.notification.notification.domain.NotificationStatus;
import com.notification.notification.repository.NotificationRecordRepository;
import com.notification.publisher.GenericPublisher;
import com.notification.publisher.event.NotificationAuditEvent;
import com.notification.publisher.event.NotificationEnrichedEvent;
import com.notification.publisher.event.NotificationIngestEvent;
import com.notification.template.cache.TemplateCacheService;
import com.notification.template.domain.NotificationTemplate;
import com.notification.template.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class IngestEventConsumer {

    private final TemplateCacheService templateCacheService;
    private final TemplateRepository templateRepository;
    private final NotificationRecordRepository recordRepository;
    private final GenericPublisher genericPublisher;
    private final NotificationMetrics notificationMetrics;

    @Value("${app.kafka.topics.enriched:notification.enriched}")
    private String enrichedTopic = "notification.enriched";

    @Value("${app.kafka.topics.audit:notification.audit}")
    private String auditTopic = "notification.audit";

    @Value("${app.kafka.topics.dlq:notification.dlq}")
    private String dlqTopic = "notification.dlq";

    @KafkaListener(topics = "${app.kafka.topics.ingest:notification.ingest}", groupId = "notification-ingest-group")
    public void consumeIngestEvent(
            @Payload NotificationIngestEvent event,
            @Header(name = "X-Tenant-Id", required = false) String tenantId,
            @Header(name = "X-Correlation-Id", required = false) String correlationId) {

        String effectiveTenant = tenantId != null ? tenantId : event.getTenantId();
        String effectiveCorrelation = correlationId != null ? correlationId : event.getCorrelationId();

        try {
            TenantContext.setTenantId(effectiveTenant);
            MDC.put("tenantId", effectiveTenant);
            MDC.put("correlationId", effectiveCorrelation);

            log.info("Processing ingest event for notification [id={}]", event.getNotificationId());

            // 1. Resolve Template
            NotificationTemplate template = templateCacheService.getTemplateById(effectiveTenant, event.getTemplateId());
            if (template == null) {
                template = templateRepository.findByIdAndTenantId(event.getTemplateId(), effectiveTenant)
                        .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + event.getTemplateId()));
                templateCacheService.cacheTemplate(template);
            }

            // 2. Perform Dynamic Variable Interpolation
            String renderedContent = TemplateSubstitutor.substitute(template.getMessageTemplate(), event.getDynamicVariables());

            // 3. Update MongoDB Record
            NotificationRecord record = recordRepository.findByIdAndTenantId(event.getNotificationId(), effectiveTenant)
                    .orElseGet(() -> NotificationRecord.builder()
                            .id(event.getNotificationId())
                            .tenantId(effectiveTenant)
                            .correlationId(effectiveCorrelation)
                            .build());

            record.setStatus(NotificationStatus.ENRICHED);
            record.setRenderedContent(renderedContent);
            record.setUpdatedAt(LocalDateTime.now());
            recordRepository.save(record);

            // 4. Publish Enriched Event to Kafka
            NotificationEnrichedEvent enrichedEvent = NotificationEnrichedEvent.builder()
                    .notificationId(event.getNotificationId())
                    .correlationId(effectiveCorrelation)
                    .tenantId(effectiveTenant)
                    .templateId(event.getTemplateId())
                    .notificationType(event.getNotificationType())
                    .recipient(event.getRecipient())
                    .renderedContent(renderedContent)
                    .retryCount(0)
                    .timestamp(LocalDateTime.now())
                    .build();

            Map<String, String> headers = Map.of(
                    "X-Tenant-Id", effectiveTenant,
                    "X-Correlation-Id", effectiveCorrelation
            );

            genericPublisher.publish(enrichedTopic, event.getNotificationId(), enrichedEvent, headers);

            // 5. Audit Event
            NotificationAuditEvent auditEvent = NotificationAuditEvent.builder()
                    .notificationId(event.getNotificationId())
                    .correlationId(effectiveCorrelation)
                    .tenantId(effectiveTenant)
                    .templateId(event.getTemplateId())
                    .notificationType(event.getNotificationType())
                    .status(NotificationStatus.ENRICHED)
                    .recipient(event.getRecipient())
                    .details("Notification template resolved and content enriched")
                    .retryCount(0)
                    .timestamp(LocalDateTime.now())
                    .build();

            genericPublisher.publish(auditTopic, event.getNotificationId(), auditEvent, headers);

            notificationMetrics.incrementNotificationRequests(effectiveTenant, event.getNotificationType().name(), NotificationStatus.ENRICHED.name());

        } catch (Exception ex) {
            log.error("Failed to process ingest event for notification [id={}]: {}", event.getNotificationId(), ex.getMessage(), ex);
            
            // Route to DLQ on fatal template rendering error
            Map<String, String> headers = Map.of(
                    "X-Tenant-Id", effectiveTenant,
                    "X-Correlation-Id", effectiveCorrelation
            );
            genericPublisher.publish(dlqTopic, event.getNotificationId(), event, headers);
            notificationMetrics.incrementDlq(effectiveTenant, event.getNotificationType().name());

        } finally {
            TenantContext.clear();
            MDC.remove("tenantId");
            MDC.remove("correlationId");
        }
    }
}
