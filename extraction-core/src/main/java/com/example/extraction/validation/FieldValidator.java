package com.example.extraction.validation;

import com.example.extraction.model.ValidationResult;
import com.example.extraction.registry.FieldConfig;

public interface FieldValidator {
    String name();

    ValidationResult validate(String value, FieldConfig config);
}
