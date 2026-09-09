package com.example.extraction.eval;

import java.util.Map;

public record FormatScore(
        String format,
        int docs,
        double classifierAccuracy,
        Map<String, Double> fieldAccuracy) {
}
