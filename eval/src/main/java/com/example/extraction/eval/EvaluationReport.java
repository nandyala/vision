package com.example.extraction.eval;

import java.util.List;
import java.util.Map;

public record EvaluationReport(
        List<FormatScore> formats,
        Map<String, Integer> outcomeDistribution,
        double straightThroughProcessingRate,
        List<HighConfidenceMiss> highConfidenceMisses) {
}
