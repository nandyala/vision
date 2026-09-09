package com.example.extraction.batch;

import java.nio.file.Path;

public record DocumentItem(Path path, byte[] bytes) {
    public String sourceFile() {
        return path.getFileName().toString();
    }
}
