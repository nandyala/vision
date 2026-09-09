package com.example.extraction.validation;

import com.example.extraction.model.ValidationResult;
import com.example.extraction.registry.FieldConfig;

import java.util.Set;

public final class EnumMembershipValidator implements FieldValidator {
    private static final Set<String> FREQUENCIES = Set.of("MONTHLY", "BIWEEKLY", "WEEKLY", "SEMI_MONTHLY");

    @Override
    public String name() {
        return "enumMembership";
    }

    @Override
    public ValidationResult validate(String value, FieldConfig config) {
        String normalized = value == null ? "" : value.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        return FREQUENCIES.contains(normalized)
                ? ValidationResult.passed(name())
                : ValidationResult.failed(name(), "unsupported enum value");
    }
}
