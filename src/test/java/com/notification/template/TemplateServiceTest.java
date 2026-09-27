package com.notification.template;

import com.notification.common.context.TenantContext;
import com.notification.common.exception.DuplicateResourceException;
import com.notification.common.exception.ResourceNotFoundException;
import com.notification.template.cache.TemplateCacheService;
import com.notification.template.domain.NotificationTemplate;
import com.notification.template.dto.CreateTemplateRequest;
import com.notification.template.dto.TemplateResponse;
import com.notification.template.repository.TemplateRepository;
import com.notification.template.service.impl.TemplateServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private TemplateCacheService templateCacheService;

    @InjectMocks
    private TemplateServiceImpl templateService;

    private final String TENANT_ID = "tenant-abc";

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should create template successfully and cache it")
    void shouldCreateTemplateSuccessfully() {
        CreateTemplateRequest request = CreateTemplateRequest.builder()
                .name("ORDER_CONFIRMATION")
                .messageTemplate("Hello {{name}}, order {{orderId}} confirmed.")
                .variables(Set.of("name", "orderId"))
                .build();

        NotificationTemplate saved = NotificationTemplate.builder()
                .id("tpl-123")
                .tenantId(TENANT_ID)
                .name(request.getName())
                .messageTemplate(request.getMessageTemplate())
                .variables(request.getVariables())
                .build();

        when(templateRepository.existsByTenantIdAndName(TENANT_ID, request.getName())).thenReturn(false);
        when(templateRepository.save(any(NotificationTemplate.class))).thenReturn(saved);

        TemplateResponse response = templateService.createTemplate(request);

        assertNotNull(response);
        assertEquals("tpl-123", response.getId());
        assertEquals("ORDER_CONFIRMATION", response.getName());
        verify(templateCacheService).cacheTemplate(saved);
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException if template name exists for tenant")
    void shouldThrowExceptionWhenDuplicateTemplateName() {
        CreateTemplateRequest request = CreateTemplateRequest.builder()
                .name("ORDER_CONFIRMATION")
                .messageTemplate("Hello {{name}}")
                .variables(Set.of("name"))
                .build();

        when(templateRepository.existsByTenantIdAndName(TENANT_ID, request.getName())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> templateService.createTemplate(request));
    }

    @Test
    @DisplayName("Should return template from cache if hit occurs")
    void shouldReturnTemplateFromCacheOnHit() {
        NotificationTemplate cached = NotificationTemplate.builder()
                .id("tpl-123")
                .tenantId(TENANT_ID)
                .name("WELCOME")
                .messageTemplate("Welcome {{name}}")
                .variables(Set.of("name"))
                .build();

        when(templateCacheService.getTemplateById(TENANT_ID, "tpl-123")).thenReturn(cached);

        TemplateResponse response = templateService.getTemplateById("tpl-123");

        assertNotNull(response);
        assertEquals("tpl-123", response.getId());
        assertEquals("WELCOME", response.getName());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when template id does not exist")
    void shouldThrowExceptionWhenTemplateNotFound() {
        when(templateCacheService.getTemplateById(TENANT_ID, "non-existent")).thenReturn(null);
        when(templateRepository.findByIdAndTenantId("non-existent", TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> templateService.getTemplateById("non-existent"));
    }
}
