package it.ai.semantic_extractor.word;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

class WordImportServiceTests {

    @Test
    void importsNormalizedUniqueWordsAndReportsSkippedLines() {
        StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        SetOperations<String, String> setOperations = Mockito.mock(SetOperations.class);
        @SuppressWarnings("unchecked")
        HashOperations<String, Object, Object> hashOperations = Mockito.mock(HashOperations.class);
        @SuppressWarnings("unchecked")
        ZSetOperations<String, String> zSetOperations = Mockito.mock(ZSetOperations.class);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(setOperations.members(WordImportService.CATALOG_KEYS)).thenReturn(Set.of());
        when(setOperations.size(WordImportService.CATALOG_KEYS)).thenReturn(2L);

        WordImportService service = new WordImportService(redisTemplate);

        WordImportResponse response = service.importWords(
                List.of("Cat", "Library", "library", "can't", "FIREWALL", "1234"), true);

        assertThat(response.imported()).isEqualTo(2);
        assertThat(response.skipped()).isEqualTo(4);
        verify(hashOperations).putAll(org.mockito.ArgumentMatchers.eq("word:library"), anyCollectionMap());
        verify(hashOperations).putAll(org.mockito.ArgumentMatchers.eq("word:firewall"), anyCollectionMap());
    }

    @Test
    void rejectsBatchesAboveMaximum() {
        WordImportService service = new WordImportService(Mockito.mock(StringRedisTemplate.class));
        List<String> oversizedBatch = java.util.Collections.nCopies(1_001, "library");

        assertThatThrownBy(() -> service.importWords(oversizedBatch, false))
                .isInstanceOf(WordImportException.class)
                .hasMessage("A single request cannot contain more than 1000 lines");
    }

    @SuppressWarnings("unchecked")
    private static java.util.Map<Object, Object> anyCollectionMap() {
        return org.mockito.ArgumentMatchers.anyMap();
    }
}
