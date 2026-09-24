package it.ai.semantic_extractor.word;

import java.util.List;

public record SemanticSearchResponse(
        String query,
        String modelId,
        List<WordVectorStore.SemanticMatch> matches) {
}
