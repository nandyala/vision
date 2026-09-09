package com.example.extraction.di;

public interface DocumentIntelligenceGateway {
    ClassifiedDocument classify(byte[] document, String classifierId);

    AnalyzedDocument analyze(byte[] document, String modelId);
}
