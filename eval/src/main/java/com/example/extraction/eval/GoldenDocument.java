package com.example.extraction.eval;

import java.util.Map;

public record GoldenDocument(
        String path,
        String category,
        String expectedFormatId,
        Map<String, String> expectedFields) {
}
