package com.example.extraction.registry;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public final class RegistryLoader {
    private final ObjectMapper mapper;

    public RegistryLoader(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public Registry load(InputStream inputStream) {
        try {
            return mapper.readValue(inputStream, Registry.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read registry", e);
        }
    }

    public Registry loadDefault() {
        InputStream inputStream = RegistryLoader.class.getResourceAsStream("/registry.json");
        if (inputStream == null) {
            throw new IllegalStateException("Default registry resource not found");
        }
        return load(inputStream);
    }
}
