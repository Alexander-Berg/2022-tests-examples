package ru.yandex.solomon.codec.compress;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.solomon.codec.bits.BitBuf;
import ru.yandex.solomon.codec.bits.BitBufAllocator;

import static org.junit.Assert.assertEquals;

/**
 * @author Vladimir Gordiychuk
 */
public class VarintEncoderTest {

    BitBuf bb;

    @Before
    public void setUp() throws Exception {
        bb = BitBufAllocator.buffer(16);
    }

    @After
    public void tearDown() throws Exception {
        bb.release();
    }

    @Test
    public void writeReadVarintMode64() {
        VarintEncoder.writeVarintMode64(bb, 0);
        VarintEncoder.writeVarintMode64(bb, 5);
        VarintEncoder.writeVarintMode64(bb, 125);
        finishWrite();

        assertEquals(0, VarintEncoder.readVarintMode64(bb));
        assertEquals(5, VarintEncoder.readVarintMode64(bb));
        assertEquals(125, VarintEncoder.readVarintMode64(bb));
    }

    @Test
    public void writeReadVarintMode64Random() {
        long[] source = LongStream.range(0, 4000)
            .map(ignore -> ThreadLocalRandom.current().nextLong())
            .toArray();

        for (long value : source) {
            VarintEncoder.writeVarintMode64(bb, value);
        }

        finishWrite();

        for (long value : source) {
            assertEquals(value, VarintEncoder.readVarintMode64(bb));
        }
    }

    @Test
    public void writeReadVarintMode32() {
        VarintEncoder.writeVarintMode32(bb, 0);
        VarintEncoder.writeVarintMode32(bb, 5);
        VarintEncoder.writeVarintMode32(bb, 125);
        finishWrite();

        assertEquals(0, VarintEncoder.readVarintMode32(bb));
        assertEquals(5, VarintEncoder.readVarintMode32(bb));
        assertEquals(125, VarintEncoder.readVarintMode32(bb));
    }

    @Test
    public void writeReadVarintMode32Random() {
        int[] source = IntStream.range(0, 4000)
            .map(ignore -> ThreadLocalRandom.current().nextInt())
            .toArray();

        for (int value : source) {
            VarintEncoder.writeVarintMode64(bb, value);
        }

        finishWrite();

        for (int value : source) {
            assertEquals(value, VarintEncoder.readVarintMode64(bb));
        }
    }

    private void finishWrite() {
        var copy = bb.copy();
        bb.release();
        bb = copy;
    }
}
