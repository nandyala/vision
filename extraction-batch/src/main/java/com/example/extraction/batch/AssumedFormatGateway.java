package com.example.extraction.batch;

import com.example.extraction.di.AnalyzedDocument;
import com.example.extraction.di.ClassifiedDocument;
import com.example.extraction.di.DocumentIntelligenceGateway;

public final class AssumedFormatGateway implements DocumentIntelligenceGateway {
    private final DocumentIntelligenceGateway delegate;
    private final String formatId;

    public AssumedFormatGateway(DocumentIntelligenceGateway delegate, String formatId) {
        this.delegate = delegate;
        this.formatId = formatId;
    }

    @Override
    public ClassifiedDocument classify(byte[] document, String classifierId) {
        return new ClassifiedDocument(formatId, 1.0d);
    }

    @Override
    public AnalyzedDocument analyze(byte[] document, String modelId) {
        return delegate.analyze(document, modelId);
    }
}
