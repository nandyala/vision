package com.example.extraction.model;

import java.util.List;
import java.util.Map;

public record ExtractionResult(
        String sourceFile,
        String detectedFormatId,
        double classificationConfidence,
        Outcome outcome,
        double overallConfidence,
        List<Reason> reasons,
        Map<String, FieldResult> fields,
        Timings timings) {
}
