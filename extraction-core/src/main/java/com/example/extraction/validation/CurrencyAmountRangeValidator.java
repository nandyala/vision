package com.example.extraction.validation;

import com.example.extraction.model.ValidationResult;
import com.example.extraction.registry.FieldConfig;

import java.math.BigDecimal;

public final class CurrencyAmountRangeValidator implements FieldValidator {
    @Override
    public String name() {
        return "currencyAmountRange";
    }

    @Override
    public ValidationResult validate(String value, FieldConfig config) {
        try {
            BigDecimal amount = new BigDecimal(value.replace("$", "").replace(",", "").trim());
            return amount.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(new BigDecimal("100000")) <= 0
                    ? ValidationResult.passed(name())
                    : ValidationResult.failed(name(), "amount outside allowed range");
        } catch (RuntimeException e) {
            return ValidationResult.failed(name(), "invalid currency amount");
        }
    }
}
