package com.example.extraction.validation;

import com.example.extraction.model.ValidationResult;
import com.example.extraction.registry.FieldConfig;

public final class DigitLength9Validator implements FieldValidator {
    @Override
    public String name() {
        return "digitLength9";
    }

    @Override
    public ValidationResult validate(String value, FieldConfig config) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        return digits.length() == 9
                ? ValidationResult.passed(name())
                : ValidationResult.failed(name(), "expected 9 digits");
    }
}
