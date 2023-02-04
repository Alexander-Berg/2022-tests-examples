package ru.yandex.solomon.codec.bits;

import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.util.IllegalReferenceCountException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vladimir Gordiychuk
 */
public class PooledHeapBitBufTest  extends AbstractBitBufTest {

    @Override
    protected PooledHeapBitBuf allocate() {
        return new PooledHeapBitBuf(UnpooledByteBufAllocator.DEFAULT.heapBuffer(), 0);
    }

    @Override
    protected BitBuf allocate(byte[] array, long expectedBitLength) {
        BitArray.checkSize(array, expectedBitLength);
        var buffer = UnpooledByteBufAllocator.DEFAULT.heapBuffer(array.length);
        buffer.writeBytes(array);
        return new PooledHeapBitBuf(buffer, expectedBitLength);
    }

    @Test
    public void bufferWithWriterIndexAtTheEnd() {
        NettyBitBuf buffer = new NettyBitBuf(UnpooledByteBufAllocator.DEFAULT.heapBuffer(), 80);
        for (int index = 0; index < 80; index++) {
            assertFalse(buffer.readBit());
        }
    }

    @Test
    public void refCnt() {
        var allocated = allocate();
        assertEquals(1, allocated.refCnt());
        allocated.retain();
        assertEquals(2, allocated.refCnt());
        assertFalse(allocated.release());
        assertEquals(1, allocated.refCnt());
        allocated.retain(2);
        assertEquals(3, allocated.refCnt());
        assertTrue(allocated.release(3));
        assertEquals(0, allocated.refCnt());
        try {
            allocated.duplicate();
            fail("See io.netty.util.ReferenceCounted");
        } catch (IllegalReferenceCountException e) {
            // it's ok
        }
    }
}
