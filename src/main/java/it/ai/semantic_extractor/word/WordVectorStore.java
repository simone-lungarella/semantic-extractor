package it.ai.semantic_extractor.word;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PreDestroy;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.stereotype.Component;

import io.lettuce.core.RedisClient;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.output.NestedMultiOutput;
import io.lettuce.core.protocol.Command;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.ProtocolKeyword;

@Component
public class WordVectorStore {

    static final String INDEX_NAME = "word-index";
    private static final byte[] EMBEDDING_FIELD = bytes("embedding");

    private final StringRedisTemplate redisTemplate;
    private final RedisClient redisClient;

    public WordVectorStore(
            StringRedisTemplate redisTemplate,
            @org.springframework.beans.factory.annotation.Value("${spring.data.redis.host}") String host,
            @org.springframework.beans.factory.annotation.Value("${spring.data.redis.port}") int port,
            @org.springframework.beans.factory.annotation.Value("${spring.data.redis.database}") int database) {
        this.redisTemplate = redisTemplate;
        this.redisClient = RedisClient.create("redis://" + host + ":" + port + "/" + database);
        this.redisClient.setOptions(ClientOptions.builder()
                .protocolVersion(ProtocolVersion.RESP2)
                .build());
    }

    public void prepareIndex(boolean reset, int dimensions) {
        if (reset) {
            dropIndexIfPresent();
        }

        try {
            redisTemplate.execute((RedisCallback<Object>) connection -> connection.execute(
                    "FT.CREATE",
                    bytes(INDEX_NAME),
                    bytes("ON"), bytes("HASH"),
                    bytes("PREFIX"), bytes("1"), bytes(WordImportService.WORD_KEY_PREFIX),
                    bytes("SCHEMA"),
                    bytes("term"), bytes("TAG"),
                    bytes("embedding"), bytes("VECTOR"), bytes("FLAT"), bytes("6"),
                    bytes("TYPE"), bytes("FLOAT32"),
                    bytes("DIM"), bytes(Integer.toString(dimensions)),
                    bytes("DISTANCE_METRIC"), bytes("COSINE")));
        } catch (DataAccessException exception) {
            if (!containsInCauseChain(exception, "Index already exists")) {
                throw exception;
            }
        }
    }

    public void storeEmbedding(String key, byte[] embedding) {
        redisTemplate.execute((RedisCallback<Boolean>) connection -> connection.hashCommands()
                .hSet(bytes(key), EMBEDDING_FIELD, embedding));
    }

    public List<SemanticMatch> search(byte[] queryVector, int limit) {
        try (StatefulRedisConnection<byte[], byte[]> connection = redisClient.connect(ByteArrayCodec.INSTANCE)) {
            CommandArgs<byte[], byte[]> args = new CommandArgs<>(ByteArrayCodec.INSTANCE)
                    .add(INDEX_NAME)
                    .add("*=>[KNN $limit @embedding $queryVector AS distance]")
                    .add("PARAMS").add(4)
                    .add("limit").add(limit)
                    .add("queryVector").add(queryVector)
                    .add("SORTBY").add("distance").add("ASC")
                    .add("RETURN").add(2).add("term").add("distance")
                    .add("LIMIT").add(0).add(limit)
                    .add("DIALECT").add(2);
            Command<byte[], byte[], List<Object>> command = new Command<>(
                    new RawCommand("FT.SEARCH"),
                    new NestedMultiOutput<>(ByteArrayCodec.INSTANCE),
                    args);
            connection.dispatch(command);
            while (!command.isDone()) {
                Thread.onSpinWait();
            }
            Object response = command.get();

            return parseSearchResponse(response);
        }
    }

    @PreDestroy
    void close() {
        redisClient.shutdown();
    }

    private List<SemanticMatch> parseSearchResponse(Object response) {
        if (!(response instanceof List<?> values) || values.size() < 2) {
            return List.of();
        }

        List<SemanticMatch> matches = new ArrayList<>();
        for (int index = 2; index < values.size(); index += 2) {
            if (!(values.get(index) instanceof List<?> fields)) {
                continue;
            }
            String term = null;
            Double distance = null;
            for (int fieldIndex = 0; fieldIndex + 1 < fields.size(); fieldIndex += 2) {
                String name = text(fields.get(fieldIndex));
                String value = text(fields.get(fieldIndex + 1));
                if ("term".equals(name)) {
                    term = value;
                } else if ("distance".equals(name)) {
                    distance = Double.valueOf(value);
                }
            }
            if (term != null && distance != null) {
                matches.add(new SemanticMatch(term, Math.max(0.0, 1.0 - distance)));
            }
        }
        return matches;
    }

    private void dropIndexIfPresent() {
        try {
            redisTemplate.execute((RedisCallback<Object>) connection -> connection.execute(
                    "FT.DROPINDEX", bytes(INDEX_NAME)));
        } catch (DataAccessException exception) {
            if (!containsInCauseChain(exception, "Unknown Index name")) {
                throw exception;
            }
        }
    }

    private static String text(Object value) {
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        return String.valueOf(value);
    }

    private static boolean containsInCauseChain(Throwable throwable, String expected) {
        for (Throwable current = throwable; current != null; current = current.getCause()) {
            if (current.getMessage() != null
                    && current.getMessage().toLowerCase().contains(expected.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    public record SemanticMatch(String term, double score) {
    }

    private record RawCommand(String command) implements ProtocolKeyword {

        @Override
        public byte[] getBytes() {
            return bytes(command);
        }
    }
}
