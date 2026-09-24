package it.ai.semantic_extractor.embedding;

import java.util.List;

import com.ibm.watsonx.ai.embedding.EmbeddingParameters;
import com.ibm.watsonx.ai.embedding.EmbeddingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "semantic-extractor.embedding.provider",
        havingValue = "watsonx")
public class WatsonxEmbeddingClient implements EmbeddingClient {

    private final EmbeddingService embeddingService;
    private final String modelId;

    public WatsonxEmbeddingClient(
            EmbeddingService embeddingService,
            @Value("${semantic-extractor.embedding.watsonx.model-id}") String modelId) {
        this.embeddingService = embeddingService;
        this.modelId = modelId;
    }

    @Override
    public List<List<Float>> embed(List<String> inputs) {
        if (inputs.isEmpty()) {
            return List.of();
        }

        try {
            var response = embeddingService.embed(
                    inputs,
                    EmbeddingParameters.builder()
                            .truncateInputTokens(512)
                            .inputText(false)
                            .build());

            if (response.results().size() != inputs.size()) {
                throw new EmbeddingUnavailableException(
                        "watsonx returned %d vectors for %d inputs"
                                .formatted(response.results().size(), inputs.size()));
            }

            return response.results().stream()
                    .map(result -> List.copyOf(result.embedding()))
                    .toList();
        } catch (EmbeddingUnavailableException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new EmbeddingUnavailableException("watsonx embedding request failed", exception);
        }
    }

    @Override
    public String modelId() {
        return modelId;
    }
}
