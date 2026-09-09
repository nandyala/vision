package com.example.extraction.registry;

import com.example.extraction.model.Criticality;

import java.util.List;

public record FieldConfig(
        String model,
        String canonical,
        Criticality criticality,
        boolean sensitive,
        double threshold,
        List<String> validators) {
}
