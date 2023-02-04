package ru.yandex.solomon.codec.bits;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
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
public class NettyBitBufTest extends AbstractBitBufTest {
    @Override
    protected BitBuf allocate() {
        return new NettyBitBuf(UnpooledByteBufAllocator.DEFAULT.heapBuffer(), 0);
    }

    @Override
    protected BitBuf allocate(byte[] array, long expectedBitLength) {
        ByteBuf buffer = UnpooledByteBufAllocator.DEFAULT.heapBuffer(array.length);
        buffer.writeBytes(array);
        return new NettyBitBuf(buffer, expectedBitLength);
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
            allocated.writeBit(false);
            fail("See io.netty.util.ReferenceCounted");
        } catch (IllegalReferenceCountException e) {
            // it's ok
        }
    }

    @Test
    public void allocatedWithSameTime() {
        var direct = PooledByteBufAllocator.DEFAULT.directBuffer();
        var heap = PooledByteBufAllocator.DEFAULT.heapBuffer();
        try {
            {
                var bitBufDirect = new NettyBitBuf(direct, 0);
                var allocated = bitBufDirect.allocate(8);
                assertTrue(allocated.isDirect());
                allocated.release();
            }
            {
                var bitBufHeap = new NettyBitBuf(heap, 0);
                var allocated = bitBufHeap.allocate(8);
                assertFalse(allocated.isDirect());
                allocated.release();
            }
        } finally {
            direct.release();
            heap.release();
        }
    }
}
