package com.example.extraction.registry;

import java.util.Map;

public record Registry(Map<String, CategoryConfig> categories) {
    public CategoryConfig category(String category) {
        CategoryConfig config = categories.get(category);
        if (config == null) {
            throw new IllegalArgumentException("Unknown category: " + category);
        }
        return config;
    }
}
