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
                "Semantic operations require an embedding provider. "
                        + "Set EMBEDDING_PROVIDER=ollama for local inference or EMBEDDING_PROVIDER=watsonx "
                        + "with the required watsonx.ai configuration.");
    }

    @Override
    public String modelId() {
        return "none";
    }

    @Override
    public int dimensions() {
        return 0;
    }
}
