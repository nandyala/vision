package com.example.extraction.mapping;

import com.example.extraction.di.AnalyzedDocument;
import com.example.extraction.di.ExtractedField;
import com.example.extraction.model.FieldResult;
import com.example.extraction.model.ValidationResult;
import com.example.extraction.registry.FieldConfig;
import com.example.extraction.registry.FormatConfig;
import com.example.extraction.validation.ValidatorRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FieldMapper {
    private final ValidatorRegistry validatorRegistry;

    public FieldMapper(ValidatorRegistry validatorRegistry) {
        this.validatorRegistry = validatorRegistry;
    }

    public Map<String, FieldResult> map(FormatConfig format, AnalyzedDocument analyzedDocument) {
        Map<String, FieldResult> results = new LinkedHashMap<>();
        for (FieldConfig fieldConfig : format.fields()) {
            ExtractedField extracted = analyzedDocument.fields().get(fieldConfig.model());
            String value = extracted == null ? "" : extracted.value();
            double confidence = extracted == null ? 0.0d : extracted.confidence();
            List<ValidationResult> validations = fieldConfig.validators().stream()
                    .map(name -> validatorRegistry.get(name).validate(value, fieldConfig))
                    .toList();
            boolean accepted = confidence >= fieldConfig.threshold()
                    && validations.stream().allMatch(ValidationResult::passed);
            String outputValue = fieldConfig.sensitive() ? mask(value) : value;
            results.put(fieldConfig.canonical(), new FieldResult(
                    outputValue,
                    fieldConfig.sensitive(),
                    confidence,
                    accepted,
                    validations,
                    extracted == null ? List.of() : extracted.boundingRegions()));
        }
        return results;
    }

    private String mask(String value) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return "";
        }
        String suffix = digits.length() <= 4 ? digits : digits.substring(digits.length() - 4);
        return "****" + suffix;
    }
}
