package com.example.extraction.eval;

import com.example.extraction.ExtractionService;
import com.example.extraction.model.ExtractionResult;
import com.example.extraction.model.FieldResult;
import com.example.extraction.model.Outcome;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class EvaluationHarness {
    private final ExtractionService extractionService;
    private final ObjectMapper mapper;

    public EvaluationHarness(ExtractionService extractionService, ObjectMapper mapper) {
        this.extractionService = extractionService;
        this.mapper = mapper;
    }

    public EvaluationReport run(Path goldenSetPath, Path reportPath) {
        GoldenSet goldenSet = readGoldenSet(goldenSetPath);
        Map<String, MutableFormatScore> byFormat = new TreeMap<>();
        Map<String, Integer> outcomes = new TreeMap<>();
        List<HighConfidenceMiss> highConfidenceMisses = new ArrayList<>();

        for (GoldenDocument golden : goldenSet.documents()) {
            ExtractionResult result = extract(golden);
            outcomes.merge(result.outcome().name(), 1, Integer::sum);
            MutableFormatScore score = byFormat.computeIfAbsent(golden.expectedFormatId(), MutableFormatScore::new);
            score.docs++;
            if (golden.expectedFormatId().equals(result.detectedFormatId())) {
                score.classifierCorrect++;
            }
            golden.expectedFields().forEach((field, expected) -> {
                FieldResult fieldResult = result.fields().get(field);
                String actual = fieldResult == null ? "" : fieldResult.value();
                score.fieldTotals.merge(field, 1, Integer::sum);
                if (exactMatch(expected, actual)) {
                    score.fieldCorrect.merge(field, 1, Integer::sum);
                } else if (fieldResult != null && fieldResult.confidence() >= 0.95d) {
                    highConfidenceMisses.add(new HighConfidenceMiss(
                            result.sourceFile(), field, expected, actual, fieldResult.confidence()));
                }
            });
        }

        int total = goldenSet.documents().size();
        int autoApproved = outcomes.getOrDefault(Outcome.AUTO_APPROVED.name(), 0);
        EvaluationReport report = new EvaluationReport(
                byFormat.values().stream().map(MutableFormatScore::toScore).toList(),
                outcomes,
                total == 0 ? 0.0d : autoApproved / (double) total,
                highConfidenceMisses);
        writeReport(reportPath, report);
        return report;
    }

    public String renderTable(EvaluationReport report) {
        StringBuilder table = new StringBuilder();
        table.append("Format Docs Classifier bankRoutingNumber bankAccountNumber paymentAmount firstPaymentDate\n");
        for (FormatScore score : report.formats()) {
            table.append(String.format(
                    "%s %d %.1f%% %.1f%% %.1f%% %.1f%% %.1f%%%n",
                    score.format(),
                    score.docs(),
                    score.classifierAccuracy() * 100,
                    score.fieldAccuracy().getOrDefault("bankRoutingNumber", 0.0d) * 100,
                    score.fieldAccuracy().getOrDefault("bankAccountNumber", 0.0d) * 100,
                    score.fieldAccuracy().getOrDefault("paymentAmount", 0.0d) * 100,
                    score.fieldAccuracy().getOrDefault("firstPaymentDate", 0.0d) * 100));
        }
        table.append(String.format("Straight-through-processing rate: %.1f%%%n",
                report.straightThroughProcessingRate() * 100));
        return table.toString();
    }

    private ExtractionResult extract(GoldenDocument golden) {
        try {
            Path path = Path.of(golden.path());
            return extractionService.extract(Files.readAllBytes(path), golden.category(), path.getFileName().toString());
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read golden document: " + golden.path(), e);
        }
    }

    private GoldenSet readGoldenSet(Path goldenSetPath) {
        try {
            return mapper.readValue(goldenSetPath.toFile(), GoldenSet.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read golden set: " + goldenSetPath, e);
        }
    }

    private void writeReport(Path reportPath, EvaluationReport report) {
        if (reportPath == null) {
            return;
        }
        try {
            Files.createDirectories(reportPath.getParent());
            mapper.writerWithDefaultPrettyPrinter().writeValue(reportPath.toFile(), report);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to write report: " + reportPath, e);
        }
    }

    private boolean exactMatch(String expected, String actual) {
        return normalize(expected).equals(normalize(actual));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class MutableFormatScore {
        private final String format;
        private int docs;
        private int classifierCorrect;
        private final Map<String, Integer> fieldTotals = new LinkedHashMap<>();
        private final Map<String, Integer> fieldCorrect = new LinkedHashMap<>();

        private MutableFormatScore(String format) {
            this.format = format;
        }

        private FormatScore toScore() {
            Map<String, Double> fieldAccuracy = new LinkedHashMap<>();
            fieldTotals.forEach((field, total) ->
                    fieldAccuracy.put(field, total == 0 ? 0.0d : fieldCorrect.getOrDefault(field, 0) / (double) total));
            return new FormatScore(format, docs, docs == 0 ? 0.0d : classifierCorrect / (double) docs, fieldAccuracy);
        }
    }
}
