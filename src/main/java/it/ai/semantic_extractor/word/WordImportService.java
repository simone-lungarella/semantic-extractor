package it.ai.semantic_extractor.word;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import it.ai.semantic_extractor.embedding.EmbeddingClient;
import it.ai.semantic_extractor.embedding.FloatVectorCodec;

@Service
public class WordImportService {

    static final String CATALOG_KEYS = "catalog:linux-words:keys";
    static final String LEXICAL_TERMS = "catalog:linux-words:terms";
    static final String WORD_KEY_PREFIX = "word:";
    static final int MAX_BATCH_SIZE = 1_000;
    static final int MIN_WORD_LENGTH = 4;
    static final int MAX_WORD_LENGTH = 15;
    private static final int EMBEDDING_BATCH_SIZE = 100;

    private final StringRedisTemplate redisTemplate;
    private final EmbeddingClient embeddingClient;
    private final FloatVectorCodec vectorCodec;
    private final WordVectorStore vectorStore;

    public WordImportService(
            StringRedisTemplate redisTemplate,
            EmbeddingClient embeddingClient,
            FloatVectorCodec vectorCodec,
            WordVectorStore vectorStore) {
        this.redisTemplate = redisTemplate;
        this.embeddingClient = embeddingClient;
        this.vectorCodec = vectorCodec;
        this.vectorStore = vectorStore;
    }

    public WordImportResponse importWords(List<String> words, boolean reset) {
        if (words.isEmpty()) {
            throw new WordImportException("Request body must contain at least one word");
        }
        if (words.size() > MAX_BATCH_SIZE) {
            throw new WordImportException("A single request cannot contain more than " + MAX_BATCH_SIZE + " lines");
        }

        Instant startedAt = Instant.now();
        Set<String> acceptedTerms = new LinkedHashSet<>();
        int skipped = 0;
        for (String word : words) {
            String term = normalize(word);
            if (!isAccepted(term) || !acceptedTerms.add(term)) {
                skipped++;
            }
        }

        List<String> terms = List.copyOf(acceptedTerms);
        List<List<Float>> embeddings = embedInBatches(terms);

        if (reset) {
            clearCatalog();
        }
        vectorStore.prepareIndex(reset, embeddingClient.dimensions());

        for (int index = 0; index < terms.size(); index++) {
            store(terms.get(index), embeddings.get(index));
        }

        Long total = redisTemplate.opsForSet().size(CATALOG_KEYS);
        return new WordImportResponse(
                acceptedTerms.size(),
                skipped,
                total == null ? 0 : total.intValue(),
                Duration.between(startedAt, Instant.now()).toMillis());
    }

    private String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }

    private boolean isAccepted(String term) {
        return term.length() >= MIN_WORD_LENGTH
                && term.length() <= MAX_WORD_LENGTH
                && term.chars().allMatch(character -> character >= 'a' && character <= 'z');
    }

    private List<List<Float>> embedInBatches(List<String> terms) {
        List<List<Float>> embeddings = new java.util.ArrayList<>(terms.size());

        for (int start = 0; start < terms.size(); start += EMBEDDING_BATCH_SIZE) {

            int end = Math.min(start + EMBEDDING_BATCH_SIZE, terms.size());
            List<String> batch = terms.subList(start, end);
            List<List<Float>> vectors = embeddingClient.embed(batch);
            if (vectors.size() != batch.size()) {
                throw new WordImportException("Embedding provider returned an unexpected number of vectors");
            }

            for (List<Float> vector : vectors) {
                if (vector.size() != embeddingClient.dimensions()) {
                    throw new WordImportException("Embedding provider returned an unexpected vector dimension");
                }
            }

            embeddings.addAll(vectors);
        }
        return embeddings;
    }

    private void store(String term, List<Float> embedding) {

        String key = WORD_KEY_PREFIX + term;
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("term", term);
        fields.put("normalizedTerm", term);
        fields.put("source", "linux-words");

        redisTemplate.opsForHash().putAll(key, fields);
        vectorStore.storeEmbedding(key, vectorCodec.encode(embedding));
        redisTemplate.opsForSet().add(CATALOG_KEYS, key);
        redisTemplate.opsForZSet().add(LEXICAL_TERMS, term, 0);
    }

    private void clearCatalog() {

        Set<String> previousKeys = redisTemplate.opsForSet().members(CATALOG_KEYS);
        if (previousKeys != null && !previousKeys.isEmpty()) {
            redisTemplate.delete(previousKeys);
        }
        redisTemplate.delete(CATALOG_KEYS);
        redisTemplate.delete(LEXICAL_TERMS);
    }
}
