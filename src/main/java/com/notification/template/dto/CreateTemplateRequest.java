package com.notification.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
public class CreateTemplateRequest {

    @NotBlank(message = "Template name is required")
    @Size(min = 2, max = 100, message = "Template name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Message template is required")
    @Size(max = 5000, message = "Message template cannot exceed 5000 characters")
    private String messageTemplate;

    @NotEmpty(message = "Template variables set cannot be empty")
    @Size(max = 20, message = "Template cannot have more than 20 dynamic variables")
    private Set<@NotBlank(message = "Variable name cannot be blank") String> variables;
}
