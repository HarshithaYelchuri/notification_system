package com.notification.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTemplateRequest {

    @NotBlank(message = "Message template is required")
    @Size(max = 5000, message = "Message template cannot exceed 5000 characters")
    private String messageTemplate;

    @Size(max = 20, message = "Template cannot have more than 20 dynamic variables")
    private Set<@NotBlank(message = "Variable name cannot be blank") String> variables;
}
