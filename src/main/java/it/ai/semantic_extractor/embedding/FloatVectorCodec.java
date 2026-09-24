package it.ai.semantic_extractor.embedding;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class FloatVectorCodec {

    public byte[] encode(List<Float> vector) {
        ByteBuffer buffer = ByteBuffer.allocate(vector.size() * Float.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN);
        vector.forEach(buffer::putFloat);
        return buffer.array();
    }
}
