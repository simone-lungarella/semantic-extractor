package it.ai.semantic_extractor.word;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class WordSearchService {

    static final int DEFAULT_LIMIT = 5;
    static final int MAX_LIMIT = 20;

    private final StringRedisTemplate redisTemplate;

    public WordSearchService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
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
