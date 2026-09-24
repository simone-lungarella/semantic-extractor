package it.ai.semantic_extractor.embedding;

import java.util.List;

/**
 * Provider-independent contract used by ingestion and search.
 *
 * Implementations must return one vector per input, in the same order.
 */
public interface EmbeddingClient {

    List<List<Float>> embed(List<String> inputs);

    String modelId();
}
