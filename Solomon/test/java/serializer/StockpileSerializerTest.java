package ru.yandex.solomon.codec.serializer;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.misc.codec.HexAssert;

/**
 * @author Stepan Koltsov
 */
public class StockpileSerializerTest {

    private void testImpl(String expected, Consumer<StockpileSerializer> write) {
        var heap = new HeapStockpileSerializer();
        var netty = new NettyStockpileSerializer(UnpooledByteBufAllocator.DEFAULT.heapBuffer());
        write.accept(heap);
        write.accept(netty);
        var heapResult = heap.build();
        var nettyResult = ByteBufUtil.getBytes(netty.getBuffer());
        HexAssert.assertArraysEqual(expected, heapResult);
        HexAssert.assertArraysEqual(expected, nettyResult);
        Assert.assertArrayEquals(nettyResult, heapResult);
    }

    private void testImpl(Consumer<StockpileSerializer> write) {
        var heap = new HeapStockpileSerializer();
        var netty = new NettyStockpileSerializer(UnpooledByteBufAllocator.DEFAULT.heapBuffer());
        write.accept(heap);
        write.accept(netty);
        var heapResult = heap.build();
        var nettyResult = ByteBufUtil.getBytes(netty.getBuffer());
        Assert.assertArrayEquals(nettyResult, heapResult);
    }

    @Test
    public void writeByte() {
        testImpl("ff", s -> s.writeByte(0xff));
        testImpl("ff", s -> s.writeByte((byte) 0xff));
    }

    @Test
    public void writeFixed32() {
        testImpl("01 02 03 04", s -> s.writeFixed32(0x04030201));
        testImpl("aa bb cc dd", s -> s.writeFixed32(0xddccbbaa));
    }

    @Test
    public void writeFixed64() {
        testImpl("01 02 03 04 05 06 07 08", s -> s.writeFixed64(0x0807060504030201L));
        testImpl("aa bb cc dd ee ff ab cd", s -> s.writeFixed64(0xcdabffeeddccbbaaL));
    }

    @Test
    public void writeBytes() {
        testImpl("aa bb cc", s -> s.writeBytes(new byte[] { (byte) 0xaa, (byte) 0xbb, (byte) 0xcc }));
        testImpl("bb cc", s -> s.writeBytes(
            new byte[] { (byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd }, 1, 2));
    }

    @Test
    public void writeVarint16() {
        testImpl("ff ff ff ff ff ff ff ff ff 01", s -> s.writeVarint16((short) -1));
    }

    @Test
    public void writeVarint32() {
        testImpl("ff ff ff ff ff ff ff ff ff 01", s -> s.writeVarint32(-1));
    }

    @Test
    public void writeVarint64() {
        testImpl("ff ff ff ff ff ff ff ff ff 01", s -> s.writeVarint64(-1));
    }

    @Test
    public void writeDouble() {
        testImpl("00 00 00 00 00 00 f0 7f", s -> s.writeDouble(Double.POSITIVE_INFINITY));
    }

    @Test
    public void randomBoolean() {
        boolean value = ThreadLocalRandom.current().nextBoolean();
        testImpl(s -> s.writeBoolean(value));
    }

    @Test
    public void randomByte() {
        byte one = (byte) ThreadLocalRandom.current().nextInt(0, 255);
        int two = ThreadLocalRandom.current().nextInt(0, 255);
        testImpl(s -> s.writeByte(one));
        testImpl(s -> s.writeByte(two));
    }

    @Test
    public void randomFixed64() {
        long value = ThreadLocalRandom.current().nextLong();
        testImpl(s -> s.writeFixed64(value));
    }

    @Test
    public void randomFixed32() {
        int value = ThreadLocalRandom.current().nextInt();
        testImpl(s -> s.writeFixed32(value));
    }

    @Test
    public void randomVarint16() {
        short value = (short) ThreadLocalRandom.current().nextInt(Short.MAX_VALUE);
        testImpl(s -> s.writeVarint16(value));
    }

    @Test
    public void randomVarint32() {
        int value = ThreadLocalRandom.current().nextInt();
        testImpl(s -> s.writeVarint32(value));
    }

    @Test
    public void randomVarint64() {
        long value = ThreadLocalRandom.current().nextLong();
        testImpl(s -> s.writeVarint64(value));
    }

    @Test
    public void randomDouble() {
        var value = ThreadLocalRandom.current().nextDouble();
        testImpl(s -> s.writeDouble(value));
    }

    @Test
    public void randomBytes() {
        byte[] bytes = new byte[ThreadLocalRandom.current().nextInt(1, 1024)];
        ThreadLocalRandom.current().nextBytes(bytes);
        testImpl(s -> s.writeBytes(bytes));

        int offset = ThreadLocalRandom.current().nextInt(0, bytes.length);
        int size = ThreadLocalRandom.current().nextInt(0, bytes.length - offset);
        testImpl(s -> s.writeBytes(bytes, offset, size));
    }
}
