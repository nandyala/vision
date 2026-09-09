package com.example.extraction.di;

public class UnreadableDocumentException extends RuntimeException {
    public UnreadableDocumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
