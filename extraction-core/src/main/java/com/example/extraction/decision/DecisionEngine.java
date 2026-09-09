package com.example.extraction.decision;

import com.example.extraction.model.FieldResult;
import com.example.extraction.model.Outcome;
import com.example.extraction.model.Reason;
import com.example.extraction.model.ValidationResult;
import com.example.extraction.registry.FieldConfig;
import com.example.extraction.registry.FormatConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.example.extraction.model.Criticality.CRITICAL;

public final class DecisionEngine {
    public Decision decide(FormatConfig format, Map<String, FieldResult> fields) {
        List<Reason> reasons = new ArrayList<>();
        double overallConfidence = 1.0d;

        for (FieldConfig config : format.fields()) {
            FieldResult field = fields.get(config.canonical());
            if (config.criticality() == CRITICAL) {
                overallConfidence = Math.min(overallConfidence, field.confidence());
                if (field.confidence() < config.threshold()) {
                    reasons.add(new Reason(
                            "LOW_CONFIDENCE_CRITICAL_FIELD",
                            config.canonical(),
                            String.format("%.3f < %.3f", field.confidence(), config.threshold())));
                }
                for (ValidationResult validation : field.validations()) {
                    if (!validation.passed()) {
                        reasons.add(new Reason("VALIDATION_FAILED", config.canonical(), validation.name()));
                    }
                }
            }
        }

        boolean allCriticalAccepted = format.fields().stream()
                .filter(field -> field.criticality() == CRITICAL)
                .allMatch(field -> fields.get(field.canonical()).accepted());
        Outcome outcome = allCriticalAccepted ? Outcome.AUTO_APPROVED : Outcome.NEEDS_REVIEW;
        return new Decision(outcome, overallConfidence == 1.0d ? minimumCriticalConfidence(format, fields) : overallConfidence, reasons);
    }

    private double minimumCriticalConfidence(FormatConfig format, Map<String, FieldResult> fields) {
        return format.fields().stream()
                .filter(field -> field.criticality() == CRITICAL)
                .map(field -> fields.get(field.canonical()).confidence())
                .min(Double::compare)
                .orElse(0.0d);
    }

    public record Decision(Outcome outcome, double overallConfidence, List<Reason> reasons) {
    }
}
