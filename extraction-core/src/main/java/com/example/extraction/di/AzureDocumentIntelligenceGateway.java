package com.example.extraction.di;

import com.azure.ai.documentintelligence.DocumentIntelligenceClient;
import com.azure.core.exception.HttpResponseException;
import com.azure.core.http.rest.RequestOptions;
import com.azure.core.util.BinaryData;
import com.azure.core.util.polling.SyncPoller;
import com.example.extraction.model.BoundingRegion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

public final class AzureDocumentIntelligenceGateway implements DocumentIntelligenceGateway {
    private static final Logger log = LoggerFactory.getLogger(AzureDocumentIntelligenceGateway.class);

    private final DocumentIntelligenceClient client;
    private final ObjectMapper mapper;
    private final Semaphore inFlight;
    private final int maxAttempts;
    private final Duration initialBackoff;

    public AzureDocumentIntelligenceGateway(
            DocumentIntelligenceClient client,
            ObjectMapper mapper,
            int maxInFlightRequests,
            int maxAttempts,
            Duration initialBackoff) {
        this.client = client;
        this.mapper = mapper;
        this.inFlight = new Semaphore(maxInFlightRequests);
        this.maxAttempts = maxAttempts;
        this.initialBackoff = initialBackoff;
    }

    @Override
    public ClassifiedDocument classify(byte[] document, String classifierId) {
        JsonNode result = callWithRetry("classify", () ->
                client.beginClassifyDocument(classifierId, requestBody(document), requestOptions()).getFinalResult());
        JsonNode documentNode = firstDocument(result);
        return new ClassifiedDocument(
                text(documentNode, "docType"),
                documentNode.path("confidence").asDouble(0.0d));
    }

    @Override
    public AnalyzedDocument analyze(byte[] document, String modelId) {
        JsonNode result = callWithRetry("analyze", () ->
                client.beginAnalyzeDocument(modelId, requestBody(document), requestOptions()).getFinalResult());
        JsonNode fieldsNode = firstDocument(result).path("fields");
        Map<String, ExtractedField> fields = new LinkedHashMap<>();
        fieldsNode.fields().forEachRemaining(entry -> fields.put(entry.getKey(), field(entry.getKey(), entry.getValue())));
        return new AnalyzedDocument(fields);
    }

    private JsonNode callWithRetry(String operation, AzureCall call) {
        acquirePermit();
        try {
            RuntimeException lastFailure = null;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    BinaryData data = call.execute();
                    return data.toObject(JsonNode.class);
                } catch (HttpResponseException e) {
                    lastFailure = e;
                    if (!isTransportRetryable(e) || attempt == maxAttempts) {
                        throw e;
                    }
                    log.warn("{} retry {} after status {}", operation, attempt, e.getResponse().getStatusCode());
                    sleepBeforeRetry(attempt);
                } catch (RuntimeException e) {
                    lastFailure = e;
                    if (attempt == maxAttempts) {
                        throw new TransientDocumentIntelligenceException(operation + " failed after retries", e);
                    }
                    log.warn("{} retry {} after {}", operation, attempt, e.getClass().getSimpleName());
                    sleepBeforeRetry(attempt);
                }
            }
            throw new TransientDocumentIntelligenceException(operation + " failed", lastFailure);
        } catch (HttpResponseException e) {
            if (e.getResponse() != null && (e.getResponse().getStatusCode() == 400 || e.getResponse().getStatusCode() == 415)) {
                throw new UnreadableDocumentException(operation + " rejected the document", e);
            }
            throw e;
        } finally {
            inFlight.release();
        }
    }

    private boolean isTransportRetryable(HttpResponseException e) {
        if (e.getResponse() == null) {
            return false;
        }
        int status = e.getResponse().getStatusCode();
        return status == 429 || status == 503 || status == 504;
    }

    private void sleepBeforeRetry(int attempt) {
        long baseMillis = initialBackoff.toMillis() * (1L << Math.max(0, attempt - 1));
        long jitter = ThreadLocalRandom.current().nextLong(100, 500);
        try {
            Thread.sleep(baseMillis + jitter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransientDocumentIntelligenceException("Interrupted during retry backoff", e);
        }
    }

    private void acquirePermit() {
        try {
            inFlight.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransientDocumentIntelligenceException("Interrupted waiting for in-flight request permit", e);
        }
    }

    private BinaryData requestBody(byte[] document) {
        return BinaryData.fromObject(Map.of("base64Source", document));
    }

    private RequestOptions requestOptions() {
        return new RequestOptions();
    }

    private JsonNode firstDocument(JsonNode result) {
        JsonNode analyzeResult = result.path("analyzeResult");
        JsonNode documents = analyzeResult.path("documents");
        if (!documents.isArray() || documents.isEmpty()) {
            throw new UnreadableDocumentException("Document Intelligence returned no document", null);
        }
        return documents.get(0);
    }

    private ExtractedField field(String name, JsonNode node) {
        String value = firstTextValue(node);
        double confidence = node.path("confidence").asDouble(0.0d);
        return new ExtractedField(name, value, confidence, boundingRegions(node.path("boundingRegions")));
    }

    private String firstTextValue(JsonNode node) {
        for (String property : List.of("content", "valueString", "valueDate", "valueNumber", "valueCurrency")) {
            JsonNode value = node.get(property);
            if (value != null && !value.isMissingNode() && !value.isNull()) {
                return value.isValueNode() ? value.asText() : value.toString();
            }
        }
        return "";
    }

    private List<BoundingRegion> boundingRegions(JsonNode nodes) {
        if (!nodes.isArray()) {
            return List.of();
        }
        List<BoundingRegion> regions = new ArrayList<>();
        for (JsonNode node : nodes) {
            List<Double> polygon = new ArrayList<>();
            node.path("polygon").forEach(value -> polygon.add(value.asDouble()));
            regions.add(new BoundingRegion(node.path("pageNumber").asInt(), polygon));
        }
        return regions;
    }

    private String text(JsonNode node, String property) {
        JsonNode value = node.get(property);
        return value == null || value.isNull() ? "" : value.asText();
    }

    @FunctionalInterface
    private interface AzureCall {
        BinaryData execute();
    }
}
