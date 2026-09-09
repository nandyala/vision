package com.example.extraction.eval;

public record HighConfidenceMiss(
        String sourceFile,
        String field,
        String expected,
        String actual,
        double confidence) {
}
