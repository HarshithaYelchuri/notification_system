package com.notification.template.service.impl;

import com.notification.common.context.TenantContext;
import com.notification.common.dto.PageResponse;
import com.notification.common.exception.DuplicateResourceException;
import com.notification.common.exception.ResourceNotFoundException;
import com.notification.common.util.TemplateSubstitutor;
import com.notification.template.cache.TemplateCacheService;
import com.notification.template.domain.NotificationTemplate;
import com.notification.template.dto.CreateTemplateRequest;
import com.notification.template.dto.TemplateResponse;
import com.notification.template.dto.UpdateTemplateRequest;
import com.notification.template.repository.TemplateRepository;
import com.notification.template.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateCacheService templateCacheService;

    @Override
    public TemplateResponse createTemplate(CreateTemplateRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();

        if (templateRepository.existsByTenantIdAndName(tenantId, request.getName())) {
            throw new DuplicateResourceException("Template with name '" + request.getName() + "' already exists for tenant '" + tenantId + "'");
        }

        // Validate template placeholder variables match declared variables
        TemplateSubstitutor.validateVariablesInTemplate(request.getMessageTemplate(), request.getVariables());

        NotificationTemplate template = NotificationTemplate.builder()
                .tenantId(tenantId)
                .name(request.getName())
                .messageTemplate(request.getMessageTemplate())
                .variables(request.getVariables())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        NotificationTemplate saved = templateRepository.save(template);
        templateCacheService.cacheTemplate(saved);

        log.info("Created template [id={}, name={}] for tenant [{}]", saved.getId(), saved.getName(), tenantId);
        return TemplateResponse.fromEntity(saved);
    }

    @Override
    public TemplateResponse getTemplateById(String id) {
        String tenantId = TenantContext.getRequiredTenantId();

        // 1. Redis Cache Hit Check
        NotificationTemplate cached = templateCacheService.getTemplateById(tenantId, id);
        if (cached != null) {
            return TemplateResponse.fromEntity(cached);
        }

        // 2. Cache Miss -> Mongo DB Lookup
        NotificationTemplate template = templateRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with ID: " + id + " for current tenant"));

        // 3. Update Redis Cache
        templateCacheService.cacheTemplate(template);

        return TemplateResponse.fromEntity(template);
    }

    @Override
    public TemplateResponse updateTemplate(String id, UpdateTemplateRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();

        NotificationTemplate existing = templateRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with ID: " + id + " for current tenant"));

        if (request.getVariables() != null) {
            TemplateSubstitutor.validateVariablesInTemplate(request.getMessageTemplate(), request.getVariables());
            existing.setVariables(request.getVariables());
        } else {
            TemplateSubstitutor.validateVariablesInTemplate(request.getMessageTemplate(), existing.getVariables());
        }

        existing.setMessageTemplate(request.getMessageTemplate());
        existing.setUpdatedAt(LocalDateTime.now());

        NotificationTemplate updated = templateRepository.save(existing);

        // Evict & Update Cache
        templateCacheService.evictTemplate(tenantId, updated.getId(), updated.getName());
        templateCacheService.cacheTemplate(updated);

        log.info("Updated template [id={}] for tenant [{}]", updated.getId(), tenantId);
        return TemplateResponse.fromEntity(updated);
    }

    @Override
    public void deleteTemplate(String id) {
        String tenantId = TenantContext.getRequiredTenantId();

        NotificationTemplate existing = templateRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with ID: " + id + " for current tenant"));

        templateRepository.delete(existing);
        templateCacheService.evictTemplate(tenantId, existing.getId(), existing.getName());

        log.info("Deleted template [id={}] for tenant [{}]", id, tenantId);
    }

    @Override
    public PageResponse<TemplateResponse> searchTemplates(String name, int page, int size, String sortBy, String sortDir) {
        String tenantId = TenantContext.getRequiredTenantId();

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<NotificationTemplate> templatePage;
        if (name != null && !name.isBlank()) {
            templatePage = templateRepository.findByTenantIdAndNameContainingIgnoreCase(tenantId, name.trim(), pageable);
        } else {
            templatePage = templateRepository.findByTenantId(tenantId, pageable);
        }

        Page<TemplateResponse> dtoPage = templatePage.map(TemplateResponse::fromEntity);
        return PageResponse.from(dtoPage);
    }
}
