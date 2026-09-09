package com.example.extraction.decision;

import com.example.extraction.ExtractionService;
import com.example.extraction.di.AnalyzedDocument;
import com.example.extraction.di.ClassifiedDocument;
import com.example.extraction.di.DocumentIntelligenceGateway;
import com.example.extraction.di.ExtractedField;
import com.example.extraction.mapping.FieldMapper;
import com.example.extraction.model.ExtractionResult;
import com.example.extraction.model.Outcome;
import com.example.extraction.registry.Registry;
import com.example.extraction.registry.RegistryLoader;
import com.example.extraction.validation.ValidatorRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExtractionServiceTest {
    @Test
    void autoApprovesWhenCriticalFieldsMeetThresholdsAndValidators() {
        ExtractionService service = service(new FakeGateway("RALP_FORMAT_01", 0.99d, validFields()));

        ExtractionResult result = service.extract(new byte[]{1}, "RECURRING_AUTO_LOAN_PAYMENT", "scan.pdf");

        assertThat(result.outcome()).isEqualTo(Outcome.AUTO_APPROVED);
        assertThat(result.overallConfidence()).isEqualTo(0.96d);
        assertThat(result.fields().get("bankRoutingNumber").value()).isEqualTo("****0021");
    }

    @Test
    void rejectsWhenClassificationConfidenceIsBelowCategoryThreshold() {
        ExtractionService service = service(new FakeGateway("RALP_FORMAT_01", 0.40d, validFields()));

        ExtractionResult result = service.extract(new byte[]{1}, "RECURRING_AUTO_LOAN_PAYMENT", "scan.pdf");

        assertThat(result.outcome()).isEqualTo(Outcome.REJECTED);
        assertThat(result.reasons()).extracting("code").containsExactly("LOW_CLASSIFICATION_CONFIDENCE");
    }

    @Test
    void needsReviewWhenCriticalValidatorFailsDespiteHighConfidence() {
        Map<String, ExtractedField> fields = validFields();
        fields.put("RoutingNo", new ExtractedField("RoutingNo", "021000022", 0.99d, java.util.List.of()));
        ExtractionService service = service(new FakeGateway("RALP_FORMAT_01", 0.99d, fields));

        ExtractionResult result = service.extract(new byte[]{1}, "RECURRING_AUTO_LOAN_PAYMENT", "scan.pdf");

        assertThat(result.outcome()).isEqualTo(Outcome.NEEDS_REVIEW);
        assertThat(result.reasons()).extracting("code").contains("VALIDATION_FAILED");
    }

    private ExtractionService service(DocumentIntelligenceGateway gateway) {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        Registry registry = new RegistryLoader(mapper).loadDefault();
        return new ExtractionService(registry, gateway, new FieldMapper(ValidatorRegistry.defaults()), new DecisionEngine());
    }

    private Map<String, ExtractedField> validFields() {
        return new java.util.LinkedHashMap<>(Map.of(
                "RoutingNo", new ExtractedField("RoutingNo", "021000021", 0.99d, java.util.List.of()),
                "AcctNo", new ExtractedField("AcctNo", "1234567890", 0.98d, java.util.List.of()),
                "DebitAmt", new ExtractedField("DebitAmt", "250.00", 0.97d, java.util.List.of()),
                "StartDate", new ExtractedField("StartDate", "2026-10-01", 0.96d, java.util.List.of()),
                "Frequency", new ExtractedField("Frequency", "MONTHLY", 0.90d, java.util.List.of())));
    }

    private record FakeGateway(String format, double classificationConfidence, Map<String, ExtractedField> fields)
            implements DocumentIntelligenceGateway {
        @Override
        public ClassifiedDocument classify(byte[] document, String classifierId) {
            return new ClassifiedDocument(format, classificationConfidence);
        }

        @Override
        public AnalyzedDocument analyze(byte[] document, String modelId) {
            return new AnalyzedDocument(fields);
        }
    }
}
