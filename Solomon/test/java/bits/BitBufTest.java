package ru.yandex.solomon.codec.bits;

import io.netty.buffer.ByteBufAllocator;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author Vladimir Gordiychuk
 */
public class BitBufTest {

    @Test
    public void differentImplementationAreEqual() {
        var allocated = new NettyBitBuf(ByteBufAllocator.DEFAULT.heapBuffer(), 0);
        var heap = new HeapBitBuf();

        try {
            allocated.writeBit(true);
            allocated.write32Bits(42);

            heap.writeBit(true);
            heap.write32Bits(42);

            assertEquals(allocated, heap);
            assertEquals(heap, allocated);

            heap.writeBit(false);

            assertNotEquals(allocated, heap);
            assertNotEquals(heap, allocated);

            allocated.writeBit(false);

            assertEquals(allocated, heap);
            assertEquals(heap, allocated);
        } finally {
            allocated.release();
            heap.release();
        }
    }
}
