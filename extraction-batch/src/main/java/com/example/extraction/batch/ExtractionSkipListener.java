package com.example.extraction.batch;

import com.example.extraction.model.ExtractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;

public final class ExtractionSkipListener implements SkipListener<DocumentItem, ExtractionResult> {
    private static final Logger log = LoggerFactory.getLogger(ExtractionSkipListener.class);

    @Override
    public void onSkipInRead(Throwable throwable) {
        log.warn("Skipped document during read: {}", throwable.getMessage(), throwable);
    }

    @Override
    public void onSkipInProcess(DocumentItem item, Throwable throwable) {
        String source = item == null ? "unknown" : item.sourceFile();
        log.warn("Skipped document during extraction: {} - {}", source, throwable.getMessage(), throwable);
    }

    @Override
    public void onSkipInWrite(ExtractionResult item, Throwable throwable) {
        String source = item == null ? "unknown" : item.sourceFile();
        log.warn("Skipped result during write: {} - {}", source, throwable.getMessage(), throwable);
    }
}
