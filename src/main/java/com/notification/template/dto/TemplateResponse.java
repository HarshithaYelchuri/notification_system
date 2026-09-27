package com.notification.template.dto;

import com.notification.template.domain.NotificationTemplate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateResponse {

    private String id;
    private String tenantId;
    private String name;
    private String messageTemplate;
    private Set<String> variables;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TemplateResponse fromEntity(NotificationTemplate entity) {
        return TemplateResponse.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .name(entity.getName())
                .messageTemplate(entity.getMessageTemplate())
                .variables(entity.getVariables())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
