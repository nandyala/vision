package com.example.extraction.batch;

import org.springframework.batch.item.ItemReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public final class DocumentItemReader implements ItemReader<DocumentItem> {
    private static final Logger log = LoggerFactory.getLogger(DocumentItemReader.class);

    private final Iterator<Path> iterator;

    public DocumentItemReader(Path inputFolder) {
        if (inputFolder == null || inputFolder.toString().isBlank()) {
            throw new IllegalArgumentException("EXTRACTION_INPUT_FOLDER is required");
        }
        if (!Files.isDirectory(inputFolder)) {
            throw new IllegalArgumentException("Input folder does not exist or is not a folder: " + inputFolder);
        }
        try {
            List<Path> files = Files.list(inputFolder)
                    .filter(Files::isRegularFile)
                    .filter(DocumentItemReader::isSupportedDocument)
                    .sorted()
                    .toList();
            log.info("Found {} supported document(s) in {}", files.size(), inputFolder);
            if (files.isEmpty()) {
                log.warn("No PDF/TIFF/JPEG/PNG files found in input folder {}", inputFolder);
            }
            this.iterator = files.iterator();
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read input folder: " + inputFolder, e);
        }
    }

    @Override
    public synchronized DocumentItem read() throws Exception {
        if (!iterator.hasNext()) {
            return null;
        }
        Path path = iterator.next();
        log.info("Processing {}", path.getFileName());
        return new DocumentItem(path, Files.readAllBytes(path));
    }

    private static boolean isSupportedDocument(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".pdf")
                || name.endsWith(".tif")
                || name.endsWith(".tiff")
                || name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".png");
    }
}
