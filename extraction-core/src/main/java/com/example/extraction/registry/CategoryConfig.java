package com.example.extraction.registry;

import java.util.Map;

public record CategoryConfig(
        String classifierId,
        double minClassificationConfidence,
        Map<String, FormatConfig> formats) {
    public FormatConfig format(String formatId) {
        FormatConfig config = formats.get(formatId);
        if (config == null) {
            throw new IllegalArgumentException("Unrecognized format: " + formatId);
        }
        return config;
    }
}
