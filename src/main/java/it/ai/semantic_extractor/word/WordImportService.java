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

@Service
public class WordImportService {

    static final String CATALOG_KEYS = "catalog:linux-words:keys";
    static final String LEXICAL_TERMS = "catalog:linux-words:terms";
    static final String WORD_KEY_PREFIX = "word:";
    static final int MAX_BATCH_SIZE = 1_000;
    static final int MIN_WORD_LENGTH = 4;
    static final int MAX_WORD_LENGTH = 15;

    private final StringRedisTemplate redisTemplate;

    public WordImportService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public WordImportResponse importWords(List<String> words, boolean reset) {
        if (words.isEmpty()) {
            throw new WordImportException("Request body must contain at least one word");
        }
        if (words.size() > MAX_BATCH_SIZE) {
            throw new WordImportException("A single request cannot contain more than " + MAX_BATCH_SIZE + " lines");
        }

        Instant startedAt = Instant.now();
        if (reset) {
            clearCatalog();
        }

        Set<String> acceptedTerms = new LinkedHashSet<>();
        int skipped = 0;
        for (String word : words) {
            String term = normalize(word);
            if (!isAccepted(term) || !acceptedTerms.add(term)) {
                skipped++;
            }
        }

        for (String term : acceptedTerms) {
            store(term);
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

    private void store(String term) {
        String key = WORD_KEY_PREFIX + term;
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("term", term);
        fields.put("normalizedTerm", term);
        fields.put("source", "linux-words");

        // Embedding generation will be added here before the hash is persisted.
        redisTemplate.opsForHash().putAll(key, fields);
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
