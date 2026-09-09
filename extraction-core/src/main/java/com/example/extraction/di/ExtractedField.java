package com.example.extraction.di;

import com.example.extraction.model.BoundingRegion;

import java.util.List;

public record ExtractedField(String name, String value, double confidence, List<BoundingRegion> boundingRegions) {
}
