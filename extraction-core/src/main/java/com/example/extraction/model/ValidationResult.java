package com.example.extraction.model;

public record ValidationResult(String name, boolean passed, String detail) {
    public static ValidationResult passed(String name) {
        return new ValidationResult(name, true, "");
    }

    public static ValidationResult failed(String name, String detail) {
        return new ValidationResult(name, false, detail);
    }
}
