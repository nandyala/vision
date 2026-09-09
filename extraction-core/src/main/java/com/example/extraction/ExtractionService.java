package com.example.extraction;

import com.example.extraction.decision.DecisionEngine;
import com.example.extraction.di.AnalyzedDocument;
import com.example.extraction.di.ClassifiedDocument;
import com.example.extraction.di.DocumentIntelligenceGateway;
import com.example.extraction.di.UnreadableDocumentException;
import com.example.extraction.mapping.FieldMapper;
import com.example.extraction.model.ExtractionResult;
import com.example.extraction.model.FieldResult;
import com.example.extraction.model.Outcome;
import com.example.extraction.model.Reason;
import com.example.extraction.model.Timings;
import com.example.extraction.registry.CategoryConfig;
import com.example.extraction.registry.FormatConfig;
import com.example.extraction.registry.Registry;

import java.util.List;
import java.util.Map;

public final class ExtractionService {
    private final Registry registry;
    private final DocumentIntelligenceGateway gateway;
    private final FieldMapper fieldMapper;
    private final DecisionEngine decisionEngine;

    public ExtractionService(
            Registry registry,
            DocumentIntelligenceGateway gateway,
            FieldMapper fieldMapper,
            DecisionEngine decisionEngine) {
        this.registry = registry;
        this.gateway = gateway;
        this.fieldMapper = fieldMapper;
        this.decisionEngine = decisionEngine;
    }

    public ExtractionResult extract(byte[] document, String category) {
        return extract(document, category, null);
    }

    public ExtractionResult extract(byte[] document, String category, String sourceFile) {
        long started = System.nanoTime();
        CategoryConfig categoryConfig = registry.category(category);
        long classifyStarted = System.nanoTime();
        ClassifiedDocument classified;
        try {
            classified = gateway.classify(document, categoryConfig.classifierId());
        } catch (UnreadableDocumentException e) {
            long totalMs = elapsedMs(started);
            return rejected(sourceFile, "", 0.0d, "UNREADABLE_DOCUMENT", e.getMessage(), new Timings(0, 0, totalMs));
        }
        long classifyMs = elapsedMs(classifyStarted);

        if (classified.confidence() < categoryConfig.minClassificationConfidence()) {
            long totalMs = elapsedMs(started);
            return rejected(
                    sourceFile,
                    classified.formatId(),
                    classified.confidence(),
                    "LOW_CLASSIFICATION_CONFIDENCE",
                    String.format("%.3f < %.3f", classified.confidence(), categoryConfig.minClassificationConfidence()),
                    new Timings(classifyMs, 0, totalMs));
        }

        FormatConfig format;
        try {
            format = categoryConfig.format(classified.formatId());
        } catch (IllegalArgumentException e) {
            long totalMs = elapsedMs(started);
            return rejected(sourceFile, classified.formatId(), classified.confidence(), "UNRECOGNIZED_FORMAT", e.getMessage(),
                    new Timings(classifyMs, 0, totalMs));
        }

        long analyzeStarted = System.nanoTime();
        AnalyzedDocument analyzedDocument = gateway.analyze(document, format.modelId());
        long analyzeMs = elapsedMs(analyzeStarted);
        Map<String, FieldResult> fields = fieldMapper.map(format, analyzedDocument);
        DecisionEngine.Decision decision = decisionEngine.decide(format, fields);

        return new ExtractionResult(
                sourceFile,
                classified.formatId(),
                classified.confidence(),
                decision.outcome(),
                decision.overallConfidence(),
                decision.reasons(),
                fields,
                new Timings(classifyMs, analyzeMs, elapsedMs(started)));
    }

    private ExtractionResult rejected(String sourceFile, String formatId, double classificationConfidence, String code, String detail, Timings timings) {
        return new ExtractionResult(
                sourceFile,
                formatId,
                classificationConfidence,
                Outcome.REJECTED,
                0.0d,
                List.of(new Reason(code, null, detail)),
                Map.of(),
                timings);
    }

    private long elapsedMs(long started) {
        return (System.nanoTime() - started) / 1_000_000;
    }
}
