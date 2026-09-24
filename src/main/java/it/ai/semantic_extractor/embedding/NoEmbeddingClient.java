package it.ai.semantic_extractor.embedding;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "semantic-extractor.embedding.provider",
        havingValue = "none",
        matchIfMissing = true)
public class NoEmbeddingClient implements EmbeddingClient {

    @Override
    public List<List<Float>> embed(List<String> inputs) {
        throw new EmbeddingUnavailableException(
                "No embedding provider is configured. Set EMBEDDING_PROVIDER=watsonx and provide watsonx credentials.");
    }

    @Override
    public String modelId() {
        return "none";
    }
}
