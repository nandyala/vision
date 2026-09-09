package com.example.extraction.validation;

import com.example.extraction.model.ValidationResult;
import com.example.extraction.registry.FieldConfig;

public final class DigitLengthRangeValidator implements FieldValidator {
    @Override
    public String name() {
        return "digitLengthRange";
    }

    @Override
    public ValidationResult validate(String value, FieldConfig config) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        return digits.length() >= 4 && digits.length() <= 17
                ? ValidationResult.passed(name())
                : ValidationResult.failed(name(), "expected 4 to 17 digits");
    }
}
