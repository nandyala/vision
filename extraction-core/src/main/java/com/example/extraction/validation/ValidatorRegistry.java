package com.example.extraction.validation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ValidatorRegistry {
    private final Map<String, FieldValidator> validators;

    public ValidatorRegistry(List<FieldValidator> validators) {
        this.validators = new HashMap<>();
        validators.forEach(validator -> this.validators.put(validator.name(), validator));
    }

    public FieldValidator get(String name) {
        FieldValidator validator = validators.get(name);
        if (validator == null) {
            throw new IllegalArgumentException("Unknown validator: " + name);
        }
        return validator;
    }

    public static ValidatorRegistry defaults() {
        return new ValidatorRegistry(List.of(
                new AbaRoutingChecksumValidator(),
                new DigitLength9Validator(),
                new DigitLengthRangeValidator(),
                new CurrencyAmountRangeValidator(),
                new DateWithinWindowValidator(),
                new EnumMembershipValidator()));
    }
}
