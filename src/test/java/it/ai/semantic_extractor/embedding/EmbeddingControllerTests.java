package it.ai.semantic_extractor.embedding;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EmbeddingControllerTests {

    @Test
    void reportsThatEmbeddingIsNotConfiguredByDefault() {
        EmbeddingController controller = new EmbeddingController(new NoEmbeddingClient());

        assertThatThrownBy(() -> controller.test("library"))
                .isInstanceOf(EmbeddingUnavailableException.class)
                .hasMessageContaining("No embedding provider is configured");
    }
}
