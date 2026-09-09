package com.example.extraction.registry;

import java.util.List;

public record FormatConfig(String modelId, List<FieldConfig> fields) {
}
