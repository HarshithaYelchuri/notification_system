package com.notification.processor;

import com.notification.common.exception.ValidationException;
import com.notification.common.util.TemplateSubstitutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateSubstitutorTest {

    @Test
    @DisplayName("Should extract variables correctly from template text")
    void shouldExtractVariablesCorrectly() {
        String template = "Hello {{name}}, your order {{orderId}} has been shipped!";
        Set<String> variables = TemplateSubstitutor.extractVariables(template);

        assertEquals(2, variables.size());
        assertTrue(variables.contains("name"));
        assertTrue(variables.contains("orderId"));
    }

    @Test
    @DisplayName("Should substitute placeholders with provided map values")
    void shouldSubstitutePlaceholders() {
        String template = "Welcome {{name}}! Your code is {{code}}.";
        Map<String, Object> values = Map.of(
                "name", "Harshi",
                "code", 9876
        );

        String result = TemplateSubstitutor.substitute(template, values);
        assertEquals("Welcome Harshi! Your code is 9876.", result);
    }

    @Test
    @DisplayName("Should throw ValidationException when placeholder variable value is missing")
    void shouldThrowExceptionWhenValueIsMissing() {
        String template = "Hello {{name}}, order {{orderId}}";
        Map<String, Object> values = Map.of("name", "Harshi");

        assertThrows(ValidationException.class, () -> TemplateSubstitutor.substitute(template, values));
    }

    @Test
    @DisplayName("Should validate variables in template body against declared variables")
    void shouldValidateVariablesAgainstDeclaredSet() {
        String template = "Hello {{name}}, your balance is {{balance}}";
        Set<String> declared = Set.of("name");

        assertThrows(ValidationException.class, () -> TemplateSubstitutor.validateVariablesInTemplate(template, declared));
    }
}
