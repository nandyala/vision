package com.example.extraction.batch;

import com.example.extraction.model.ExtractionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public final class JsonItemWriter implements ItemWriter<ExtractionResult> {
    private static final Logger log = LoggerFactory.getLogger(JsonItemWriter.class);

    private final ObjectMapper mapper;
    private final Path outputFolder;

    public JsonItemWriter(ObjectMapper mapper, Path outputFolder) {
        if (outputFolder == null || outputFolder.toString().isBlank()) {
            throw new IllegalArgumentException("EXTRACTION_OUTPUT_FOLDER is required");
        }
        this.mapper = mapper;
        this.outputFolder = outputFolder;
    }

    @Override
    public void write(Chunk<? extends ExtractionResult> chunk) throws Exception {
        Files.createDirectories(outputFolder);
        for (ExtractionResult result : chunk) {
            String source = result.sourceFile() == null ? "unknown" : result.sourceFile();
            Path output = outputFolder.resolve(source + ".json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), result);
            log.info("Wrote extraction result {}", output);
        }
    }
}
