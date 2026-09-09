package com.example.extraction.validation;

import com.example.extraction.model.ValidationResult;
import com.example.extraction.registry.FieldConfig;

public final class AbaRoutingChecksumValidator implements FieldValidator {
    @Override
    public String name() {
        return "abaRoutingChecksum";
    }

    @Override
    public ValidationResult validate(String value, FieldConfig config) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        if (digits.length() != 9) {
            return ValidationResult.failed(name(), "routing number must contain 9 digits");
        }

        int sum = 0;
        int[] weights = {3, 7, 1};
        for (int i = 0; i < digits.length(); i++) {
            sum += Character.digit(digits.charAt(i), 10) * weights[i % weights.length];
        }
        return sum % 10 == 0
                ? ValidationResult.passed(name())
                : ValidationResult.failed(name(), "checksum failed");
    }
}
