package it.ai.semantic_extractor.word;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import it.ai.semantic_extractor.embedding.EmbeddingClient;
import it.ai.semantic_extractor.embedding.FloatVectorCodec;

@Service
public class WordSearchService {

    static final int DEFAULT_LIMIT = 5;
    static final int MAX_LIMIT = 20;

    private final StringRedisTemplate redisTemplate;
    private final EmbeddingClient embeddingClient;
    private final FloatVectorCodec vectorCodec;
    private final WordVectorStore vectorStore;

    public WordSearchService(
            StringRedisTemplate redisTemplate,
            EmbeddingClient embeddingClient,
            FloatVectorCodec vectorCodec,
            WordVectorStore vectorStore) {
        this.redisTemplate = redisTemplate;
        this.embeddingClient = embeddingClient;
        this.vectorCodec = vectorCodec;
        this.vectorStore = vectorStore;
    }

    public SemanticSearchResponse semanticSearch(String rawQuery, Integer requestedLimit) {
        if (rawQuery == null || rawQuery.isBlank()) {
            throw new IllegalArgumentException("q must contain text");
        }
        String query = rawQuery.strip();
        int limit = validateLimit(requestedLimit);
        List<Float> vector = embeddingClient.embed(List.of(query)).get(0);
        return new SemanticSearchResponse(
                query,
                embeddingClient.modelId(),
                vectorStore.search(vectorCodec.encode(vector), limit));
    }

    public WordSearchResponse lexicalSearch(String rawQuery, Integer requestedLimit) {
        String query = normalizeQuery(rawQuery);
        int limit = validateLimit(requestedLimit);

        Set<String> terms = redisTemplate.opsForZSet().rangeByLex(
                WordImportService.LEXICAL_TERMS,
                Range.closed(query, query + "\uffff"),
                Limit.limit().count(limit));

        return new WordSearchResponse(
                query,
                terms == null ? List.of() : List.copyOf(terms));
    }

    private String normalizeQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            throw new IllegalArgumentException("q must contain text");
        }

        String query = rawQuery.strip().toLowerCase(Locale.ROOT);
        if (!query.chars().allMatch(character -> character >= 'a' && character <= 'z')) {
            throw new IllegalArgumentException("q must contain only letters a-z");
        }
        return query;
    }

    private int validateLimit(Integer requestedLimit) {
        int limit = requestedLimit == null ? DEFAULT_LIMIT : requestedLimit;
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }
        return limit;
    }
}
