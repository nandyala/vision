package com.example.extraction.di;

import java.util.Map;

public record AnalyzedDocument(Map<String, ExtractedField> fields) {
}
