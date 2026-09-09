package com.example.extraction.batch;

import com.example.extraction.ExtractionService;
import com.example.extraction.model.ExtractionResult;
import org.springframework.batch.item.ItemProcessor;

public final class ExtractionItemProcessor implements ItemProcessor<DocumentItem, ExtractionResult> {
    private final ExtractionService extractionService;
    private final String category;

    public ExtractionItemProcessor(ExtractionService extractionService, String category) {
        this.extractionService = extractionService;
        this.category = category;
    }

    @Override
    public ExtractionResult process(DocumentItem item) {
        return extractionService.extract(item.bytes(), category, item.sourceFile());
    }
}
