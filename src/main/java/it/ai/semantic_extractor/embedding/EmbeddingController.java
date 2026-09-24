package it.ai.semantic_extractor.embedding;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/embeddings")
public class EmbeddingController {

    private final EmbeddingClient embeddingClient;

    public EmbeddingController(EmbeddingClient embeddingClient) {
        this.embeddingClient = embeddingClient;
    }

    @PostMapping(value = "/test", consumes = MediaType.TEXT_PLAIN_VALUE)
    public EmbeddingTestResponse test(@RequestBody String input) {
        if (input.isBlank()) {
            throw new IllegalArgumentException("Request body must contain text");
        }

        List<Float> vector = embeddingClient.embed(List.of(input.strip())).get(0);
        return new EmbeddingTestResponse(
                embeddingClient.modelId(),
                vector.size(),
                vector.subList(0, Math.min(5, vector.size())));
    }

    public record EmbeddingTestResponse(
            String modelId,
            int dimensions,
            List<Float> sample) {
    }
}
