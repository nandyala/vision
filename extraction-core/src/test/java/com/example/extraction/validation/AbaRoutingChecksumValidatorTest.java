package com.example.extraction.validation;

import com.example.extraction.model.ValidationResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AbaRoutingChecksumValidatorTest {
    private final AbaRoutingChecksumValidator validator = new AbaRoutingChecksumValidator();

    @Test
    void acceptsValidRoutingNumber() {
        ValidationResult result = validator.validate("021000021", null);

        assertThat(result.passed()).isTrue();
    }

    @Test
    void rejectsChecksumMiss() {
        ValidationResult result = validator.validate("021000022", null);

        assertThat(result.passed()).isFalse();
    }

    @Test
    void rejectsWrongLength() {
        ValidationResult result = validator.validate("1234", null);

        assertThat(result.passed()).isFalse();
    }
}
