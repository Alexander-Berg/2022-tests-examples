package ru.yandex.solomon.codec.serializer;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.solomon.codec.bits.BitBuf;
import ru.yandex.solomon.codec.bits.BitBufAllocator;
import ru.yandex.solomon.codec.serializer.array.BitBufSerializer;

import static org.junit.Assert.assertEquals;
import static ru.yandex.solomon.util.CloseableUtils.release;

/**
 * @author Vladimir Gordiychuk
 */
@RunWith(Parameterized.class)
public class BitBufSerializerTest {

    @Parameterized.Parameter
    public StockpileFormat format;

    @Parameterized.Parameters(name = "{0}")
    public static Object[] data() {
        return StockpileFormat.values();
    }

    @Test
    public void empty() {
        BitBuf empty = allocate(new byte[0], 0);
        BitBuf result = deserialize(serialize(empty));
        assertEquals(empty, result);
        release(empty, result);
    }

    @Test
    public void oneByte() {
        for (int bits = 0; bits < 8; bits++) {
            BitBuf expected = allocate(randomBytes(1), bits);
            BitBuf result = deserialize(serialize(expected));
            assertEquals(expected, result);
            release(expected, result);
        }
    }

    @Test
    public void twoBytes() {
        for (int bits = 0; bits < 8 * 2; bits++) {
            BitBuf expected = allocate(randomBytes(2), bits);
            BitBuf result = deserialize(serialize(expected));
            assertEquals(expected, result);
            release(expected, result);
        }
    }

    private BitBuf allocate(byte[] bytes, long bits) {
        return BitBufAllocator.buffer(bytes, bits);
    }

    private byte[] randomBytes(int size) {
        var bytes = new byte[size];
        ThreadLocalRandom.current().nextBytes(bytes);
        return bytes;
    }

    private byte[] serialize(BitBuf view) {
        var s = new HeapStockpileSerializer();
        BitBufSerializer.I.serializeWithLength(view, s);
        return s.build();
    }

    private BitBuf deserialize(byte[] bytes) {
        StockpileDeserializer d = new StockpileDeserializer(bytes);
        return BitBufSerializer.I.deserializeWithLength(d);
    }
}
