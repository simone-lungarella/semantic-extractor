package it.ai.semantic_extractor.word;

public record WordImportResponse(
        int imported,
        int skipped,
        int total,
        long durationMs) {
}
