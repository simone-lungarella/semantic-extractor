package it.ai.semantic_extractor.embedding;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(
        name = "semantic-extractor.embedding.provider",
        havingValue = "ollama")
public class OllamaEmbeddingClient implements EmbeddingClient {

    private final RestClient restClient;
    private final String modelId;
    private final int dimensions;

    public OllamaEmbeddingClient(
            @Value("${semantic-extractor.embedding.ollama.url}") String url,
            @Value("${semantic-extractor.embedding.ollama.model-id}") String modelId,
            @Value("${semantic-extractor.embedding.ollama.dimensions}") int dimensions) {
        this.restClient = RestClient.builder().baseUrl(url).build();
        this.modelId = modelId;
        this.dimensions = dimensions;
    }

    @Override
    public List<List<Float>> embed(List<String> inputs) {
        if (inputs.isEmpty()) {
            return List.of();
        }

        try {
            OllamaEmbedResponse response = restClient.post()
                    .uri("/api/embed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new OllamaEmbedRequest(modelId, inputs, true))
                    .retrieve()
                    .body(OllamaEmbedResponse.class);

            if (response == null || response.embeddings() == null) {
                throw new EmbeddingUnavailableException("Ollama returned no embeddings");
            }
            if (response.embeddings().size() != inputs.size()) {
                throw new EmbeddingUnavailableException(
                        "Ollama returned %d vectors for %d inputs"
                                .formatted(response.embeddings().size(), inputs.size()));
            }
            for (List<Float> vector : response.embeddings()) {
                if (vector.size() != dimensions) {
                    throw new EmbeddingUnavailableException(
                            "Ollama returned a %d-dimensional vector; expected %d"
                                    .formatted(vector.size(), dimensions));
                }
            }
            return response.embeddings();
        } catch (EmbeddingUnavailableException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new EmbeddingUnavailableException(
                    "Ollama embedding request failed; confirm Ollama is running and the model is installed",
                    exception);
        }
    }

    @Override
    public String modelId() {
        return modelId;
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    private record OllamaEmbedRequest(String model, List<String> input, boolean truncate) {
    }

    private record OllamaEmbedResponse(List<List<Float>> embeddings) {
    }
}
