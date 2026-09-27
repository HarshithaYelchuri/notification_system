package com.notification.notification;

import com.notification.common.context.TenantContext;
import com.notification.common.exception.ValidationException;
import com.notification.notification.domain.NotificationRecord;
import com.notification.notification.domain.NotificationStatus;
import com.notification.notification.domain.NotificationType;
import com.notification.notification.dto.SendNotificationRequest;
import com.notification.notification.dto.SendNotificationResponse;
import com.notification.notification.repository.NotificationRecordRepository;
import com.notification.notification.service.impl.NotificationServiceImpl;
import com.notification.publisher.GenericPublisher;
import com.notification.template.cache.TemplateCacheService;
import com.notification.template.domain.NotificationTemplate;
import com.notification.template.repository.TemplateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRecordRepository recordRepository;

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private TemplateCacheService templateCacheService;

    @Mock
    private GenericPublisher genericPublisher;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private final String TENANT_ID = "tenant-xyz";

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should ingest send notification request successfully and publish Kafka event")
    void shouldIngestSendNotificationSuccessfully() {
        SendNotificationRequest request = SendNotificationRequest.builder()
                .templateId("tpl-99")
                .notificationType(NotificationType.EMAIL)
                .recipient("test@example.com")
                .dynamicVariables(Map.of("name", "Harshi", "orderId", "12345"))
                .build();

        NotificationTemplate template = NotificationTemplate.builder()
                .id("tpl-99")
                .tenantId(TENANT_ID)
                .name("ORDER_SUMMARY")
                .messageTemplate("Hello {{name}}, order {{orderId}}")
                .variables(Set.of("name", "orderId"))
                .build();

        NotificationRecord savedRecord = NotificationRecord.builder()
                .id("notif-001")
                .tenantId(TENANT_ID)
                .correlationId("corr-123")
                .templateId("tpl-99")
                .notificationType(NotificationType.EMAIL)
                .status(NotificationStatus.INGESTED)
                .build();

        when(templateCacheService.getTemplateById(TENANT_ID, "tpl-99")).thenReturn(template);
        when(recordRepository.save(any(NotificationRecord.class))).thenReturn(savedRecord);

        SendNotificationResponse response = notificationService.sendNotification(request);

        assertNotNull(response);
        assertEquals("notif-001", response.getNotificationId());
        assertEquals(NotificationStatus.INGESTED, response.getStatus());

        verify(genericPublisher).publish(eq("notification.ingest"), eq("notif-001"), any(), any());
        verify(genericPublisher).publish(eq("notification.audit"), eq("notif-001"), any(), any());
    }

    @Test
    @DisplayName("Should throw ValidationException when required template variable is missing in request")
    void shouldThrowValidationExceptionWhenVariableMissing() {
        SendNotificationRequest request = SendNotificationRequest.builder()
                .templateId("tpl-99")
                .notificationType(NotificationType.EMAIL)
                .recipient("test@example.com")
                .dynamicVariables(Map.of("name", "Harshi")) // Missing 'orderId'
                .build();

        NotificationTemplate template = NotificationTemplate.builder()
                .id("tpl-99")
                .tenantId(TENANT_ID)
                .variables(Set.of("name", "orderId"))
                .build();

        when(templateCacheService.getTemplateById(TENANT_ID, "tpl-99")).thenReturn(template);

        assertThrows(ValidationException.class, () -> notificationService.sendNotification(request));
    }
}
