package com.notification.processor.consumer;

import com.notification.channel.ChannelRegistry;
import com.notification.channel.NotificationChannel;
import com.notification.channel.dto.ChannelDeliveryResult;
import com.notification.common.context.TenantContext;
import com.notification.metrics.NotificationMetrics;
import com.notification.notification.domain.NotificationRecord;
import com.notification.notification.domain.NotificationStatus;
import com.notification.notification.repository.NotificationRecordRepository;
import com.notification.publisher.GenericPublisher;
import com.notification.publisher.event.NotificationAuditEvent;
import com.notification.publisher.event.NotificationEnrichedEvent;
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
public class EnrichedEventConsumer {

    private final ChannelRegistry channelRegistry;
    private final NotificationRecordRepository recordRepository;
    private final GenericPublisher genericPublisher;
    private final NotificationMetrics notificationMetrics;

    @Value("${app.kafka.topics.audit:notification.audit}")
    private String auditTopic = "notification.audit";

    @Value("${app.kafka.topics.retry:notification.retry}")
    private String retryTopic = "notification.retry";

    @Value("${app.kafka.topics.dlq:notification.dlq}")
    private String dlqTopic = "notification.dlq";

    private static final int MAX_RETRIES = 3;

    @KafkaListener(topics = "${app.kafka.topics.enriched:notification.enriched}", groupId = "notification-enriched-group")
    public void consumeEnrichedEvent(
            @Payload NotificationEnrichedEvent event,
            @Header(name = "X-Tenant-Id", required = false) String tenantId,
            @Header(name = "X-Correlation-Id", required = false) String correlationId) {

        String effectiveTenant = tenantId != null ? tenantId : event.getTenantId();
        String effectiveCorrelation = correlationId != null ? correlationId : event.getCorrelationId();

        long startTime = System.currentTimeMillis();

        try {
            TenantContext.setTenantId(effectiveTenant);
            MDC.put("tenantId", effectiveTenant);
            MDC.put("correlationId", effectiveCorrelation);

            log.info("Dispatching notification [id={}] via channel [{}]", event.getNotificationId(), event.getNotificationType());

            NotificationChannel channel = channelRegistry.getChannel(event.getNotificationType());
            ChannelDeliveryResult result = channel.send(event);

            long duration = System.currentTimeMillis() - startTime;
            notificationMetrics.recordProcessingLatency(effectiveTenant, event.getNotificationType().name(), duration);

            Map<String, String> headers = Map.of(
                    "X-Tenant-Id", effectiveTenant,
                    "X-Correlation-Id", effectiveCorrelation
            );

            NotificationRecord record = recordRepository.findByIdAndTenantId(event.getNotificationId(), effectiveTenant)
                    .orElseGet(() -> NotificationRecord.builder()
                            .id(event.getNotificationId())
                            .tenantId(effectiveTenant)
                            .correlationId(effectiveCorrelation)
                            .build());

            if (result.isSuccess()) {
                // Success Path
                record.setStatus(NotificationStatus.DELIVERED);
                record.setUpdatedAt(LocalDateTime.now());
                recordRepository.save(record);

                NotificationAuditEvent auditEvent = NotificationAuditEvent.builder()
                        .notificationId(event.getNotificationId())
                        .correlationId(effectiveCorrelation)
                        .tenantId(effectiveTenant)
                        .templateId(event.getTemplateId())
                        .notificationType(event.getNotificationType())
                        .status(NotificationStatus.DELIVERED)
                        .recipient(event.getRecipient())
                        .details("Notification delivered successfully via " + event.getNotificationType())
                        .retryCount(event.getRetryCount())
                        .timestamp(LocalDateTime.now())
                        .build();

                genericPublisher.publish(auditTopic, event.getNotificationId(), auditEvent, headers);
                notificationMetrics.incrementNotificationRequests(effectiveTenant, event.getNotificationType().name(), NotificationStatus.DELIVERED.name());

            } else {
                // Failure Path
                log.warn("Delivery failed for notification [id={}]: {}", event.getNotificationId(), result.getErrorMessage());

                if (event.getRetryCount() < MAX_RETRIES && result.getStatusCode() != 400) {
                    // Retryable Transient Failure
                    int nextRetry = event.getRetryCount() + 1;
                    event.setRetryCount(nextRetry);

                    record.setStatus(NotificationStatus.RETRYING);
                    record.setRetryCount(nextRetry);
                    record.setErrorMessage(result.getErrorMessage());
                    record.setUpdatedAt(LocalDateTime.now());
                    recordRepository.save(record);

                    genericPublisher.publish(retryTopic, event.getNotificationId(), event, headers);

                    NotificationAuditEvent auditEvent = NotificationAuditEvent.builder()
                            .notificationId(event.getNotificationId())
                            .correlationId(effectiveCorrelation)
                            .tenantId(effectiveTenant)
                            .templateId(event.getTemplateId())
                            .notificationType(event.getNotificationType())
                            .status(NotificationStatus.RETRYING)
                            .recipient(event.getRecipient())
                            .details("Delivery failed, scheduling retry attempt " + nextRetry)
                            .errorMessage(result.getErrorMessage())
                            .retryCount(nextRetry)
                            .timestamp(LocalDateTime.now())
                            .build();

                    genericPublisher.publish(auditTopic, event.getNotificationId(), auditEvent, headers);
                    notificationMetrics.incrementRetries(effectiveTenant, event.getNotificationType().name());

                } else {
                    // Fatal or Exhausted Retries -> Send to DLQ
                    record.setStatus(NotificationStatus.DLQ);
                    record.setErrorMessage(result.getErrorMessage());
                    record.setUpdatedAt(LocalDateTime.now());
                    recordRepository.save(record);

                    genericPublisher.publish(dlqTopic, event.getNotificationId(), event, headers);

                    NotificationAuditEvent auditEvent = NotificationAuditEvent.builder()
                            .notificationId(event.getNotificationId())
                            .correlationId(effectiveCorrelation)
                            .tenantId(effectiveTenant)
                            .templateId(event.getTemplateId())
                            .notificationType(event.getNotificationType())
                            .status(NotificationStatus.DLQ)
                            .recipient(event.getRecipient())
                            .details("Notification processing permanently failed. Sent to DLQ.")
                            .errorMessage(result.getErrorMessage())
                            .retryCount(event.getRetryCount())
                            .timestamp(LocalDateTime.now())
                            .build();

                    genericPublisher.publish(auditTopic, event.getNotificationId(), auditEvent, headers);
                    notificationMetrics.incrementDlq(effectiveTenant, event.getNotificationType().name());
                }
            }

        } finally {
            TenantContext.clear();
            MDC.remove("tenantId");
            MDC.remove("correlationId");
        }
    }
}
