package it.ai.semantic_extractor.word;

import java.util.List;

public record WordSearchResponse(
        String query,
        List<String> matches) {
}
