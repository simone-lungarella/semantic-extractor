package it.ai.semantic_extractor.embedding;

import java.net.URI;
import java.time.Duration;

import com.ibm.watsonx.ai.embedding.EmbeddingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        name = "semantic-extractor.embedding.provider",
        havingValue = "watsonx")
public class WatsonxEmbeddingConfiguration {

    @Bean
    EmbeddingService watsonxEmbeddingService(
            @Value("${semantic-extractor.embedding.watsonx.api-key}") String apiKey,
            @Value("${semantic-extractor.embedding.watsonx.project-id}") String projectId,
            @Value("${semantic-extractor.embedding.watsonx.url}") String url,
            @Value("${semantic-extractor.embedding.watsonx.model-id}") String modelId) {
        requireValue(apiKey, "WATSONX_API_KEY");
        requireValue(projectId, "WATSONX_PROJECT_ID");
        requireValue(url, "WATSONX_URL");

        return EmbeddingService.builder()
                .apiKey(apiKey)
                .projectId(projectId)
                .baseUrl(URI.create(url))
                .modelId(modelId)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    private static void requireValue(String value, String environmentVariable) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(environmentVariable + " is required when EMBEDDING_PROVIDER=watsonx");
        }
    }
}
