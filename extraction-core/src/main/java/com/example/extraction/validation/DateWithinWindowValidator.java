package com.example.extraction.validation;

import com.example.extraction.model.ValidationResult;
import com.example.extraction.registry.FieldConfig;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class DateWithinWindowValidator implements FieldValidator {
    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US),
            DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US));

    @Override
    public String name() {
        return "dateWithinWindow";
    }

    @Override
    public ValidationResult validate(String value, FieldConfig config) {
        LocalDate date = parse(value);
        if (date == null) {
            return ValidationResult.failed(name(), "invalid date");
        }
        LocalDate today = LocalDate.now();
        LocalDate earliest = today.minusDays(30);
        LocalDate latest = today.plusYears(2);
        return !date.isBefore(earliest) && !date.isAfter(latest)
                ? ValidationResult.passed(name())
                : ValidationResult.failed(name(), "date outside allowed window");
    }

    private LocalDate parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDate.parse(value.trim(), formatter);
            } catch (RuntimeException ignored) {
                // Try the next expected bank-form date shape.
            }
        }
        return null;
    }
}
