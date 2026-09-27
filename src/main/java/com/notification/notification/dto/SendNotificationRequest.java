package com.notification.notification.dto;

import com.notification.notification.domain.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {

    @NotBlank(message = "templateId is required")
    private String templateId;

    @NotNull(message = "notificationType is required (EMAIL, SMS, WEBHOOK)")
    private NotificationType notificationType;

    @NotBlank(message = "recipient is required")
    private String recipient;

    @NotNull(message = "dynamicVariables map is required")
    private Map<String, Object> dynamicVariables;
}
