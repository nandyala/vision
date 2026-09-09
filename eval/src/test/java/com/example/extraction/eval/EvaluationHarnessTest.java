package com.example.extraction.eval;

import com.example.extraction.ExtractionService;
import com.example.extraction.di.AnalyzedDocument;
import com.example.extraction.di.ClassifiedDocument;
import com.example.extraction.di.DocumentIntelligenceGateway;
import com.example.extraction.di.ExtractedField;
import com.example.extraction.mapping.FieldMapper;
import com.example.extraction.registry.RegistryLoader;
import com.example.extraction.validation.ValidatorRegistry;
import com.example.extraction.decision.DecisionEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluationHarnessTest {
    @TempDir
    Path tempDir;

    @Test
    void scoresExactMatchesAndHighConfidenceMisses() throws Exception {
        Path doc = tempDir.resolve("scan.pdf");
        Files.write(doc, new byte[]{1});
        Path golden = tempDir.resolve("golden-set.json");
        Files.writeString(golden, """
                {
                  "documents": [{
                    "path": "%s",
                    "category": "RECURRING_AUTO_LOAN_PAYMENT",
                    "expectedFormatId": "RALP_FORMAT_01",
                    "expectedFields": {
                      "bankRoutingNumber": "****0021",
                      "bankAccountNumber": "****7890",
                      "paymentAmount": "300.00",
                      "firstPaymentDate": "2026-10-01"
                    }
                  }]
                }
                """.formatted(doc.toString().replace("\\", "\\\\")));
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        ExtractionService service = new ExtractionService(
                new RegistryLoader(mapper).loadDefault(),
                new FakeGateway(),
                new FieldMapper(ValidatorRegistry.defaults()),
                new DecisionEngine());

        EvaluationReport report = new EvaluationHarness(service, mapper).run(golden, null);

        assertThat(report.formats()).singleElement().satisfies(score -> {
            assertThat(score.classifierAccuracy()).isEqualTo(1.0d);
            assertThat(score.fieldAccuracy().get("paymentAmount")).isZero();
        });
        assertThat(report.highConfidenceMisses()).extracting(HighConfidenceMiss::field).contains("paymentAmount");
    }

    private static final class FakeGateway implements DocumentIntelligenceGateway {
        @Override
        public ClassifiedDocument classify(byte[] document, String classifierId) {
            return new ClassifiedDocument("RALP_FORMAT_01", 0.99d);
        }

        @Override
        public AnalyzedDocument analyze(byte[] document, String modelId) {
            return new AnalyzedDocument(Map.of(
                    "RoutingNo", new ExtractedField("RoutingNo", "021000021", 0.99d, List.of()),
                    "AcctNo", new ExtractedField("AcctNo", "1234567890", 0.99d, List.of()),
                    "DebitAmt", new ExtractedField("DebitAmt", "250.00", 0.99d, List.of()),
                    "StartDate", new ExtractedField("StartDate", "2026-10-01", 0.99d, List.of()),
                    "Frequency", new ExtractedField("Frequency", "MONTHLY", 0.99d, List.of())));
        }
    }
}
