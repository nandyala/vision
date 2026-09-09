package com.example.extraction.model;

import java.util.List;

public record BoundingRegion(int pageNumber, List<Double> polygon) {
}
