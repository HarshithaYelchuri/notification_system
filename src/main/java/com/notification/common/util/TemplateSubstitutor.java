package com.notification.common.util;

import com.notification.common.exception.ValidationException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateSubstitutor {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*\\}\\}");

    private TemplateSubstitutor() {
    }

    /**
     * Extracts placeholder variable names from template text (e.g., {{name}} -> "name").
     */
    public static Set<String> extractVariables(String messageTemplate) {
        Set<String> variables = new HashSet<>();
        if (messageTemplate == null) {
            return variables;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(messageTemplate);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }

        return variables;
    }

    /**
     * Validates that all placeholder variables in template body are declared in the variable set.
     */
    public static void validateVariablesInTemplate(String messageTemplate, Set<String> declaredVariables) {
        Set<String> extracted = extractVariables(messageTemplate);
        if (declaredVariables == null || !declaredVariables.containsAll(extracted)) {
            Set<String> missing = new HashSet<>(extracted);
            if (declaredVariables != null) {
                missing.removeAll(declaredVariables);
            }
            throw new ValidationException("Message template contains undeclared placeholder variables: " + missing);
        }
    }

    /**
     * Replaces placeholder variables in template text with provided values.
     */
    public static String substitute(String messageTemplate, Map<String, Object> values) {
        if (messageTemplate == null) {
            return "";
        }
        if (values == null || values.isEmpty()) {
            return messageTemplate;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(messageTemplate);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String varName = matcher.group(1);
            Object replacement = values.get(varName);
            if (replacement == null) {
                throw new ValidationException("Missing value for template variable: " + varName);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(replacement)));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }
}
