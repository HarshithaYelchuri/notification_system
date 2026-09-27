package com.notification.template.service;

import com.notification.common.dto.PageResponse;
import com.notification.template.dto.CreateTemplateRequest;
import com.notification.template.dto.TemplateResponse;
import com.notification.template.dto.UpdateTemplateRequest;

public interface TemplateService {

    TemplateResponse createTemplate(CreateTemplateRequest request);

    TemplateResponse getTemplateById(String id);

    TemplateResponse updateTemplate(String id, UpdateTemplateRequest request);

    void deleteTemplate(String id);

    PageResponse<TemplateResponse> searchTemplates(String name, int page, int size, String sortBy, String sortDir);
}
