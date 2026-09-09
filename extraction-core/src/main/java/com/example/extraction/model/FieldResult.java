package com.example.extraction.model;

import java.util.List;

public record FieldResult(
        String value,
        boolean masked,
        double confidence,
        boolean accepted,
        List<ValidationResult> validations,
        List<BoundingRegion> boundingRegions) {
}
