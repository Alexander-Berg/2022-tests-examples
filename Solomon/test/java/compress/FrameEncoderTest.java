package ru.yandex.solomon.codec.compress;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

import ru.yandex.solomon.codec.bits.BitBuf;
import ru.yandex.solomon.codec.bits.BitBufAllocator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.solomon.codec.compress.FrameEncoder.initFrame;

/**
 * @author Vladimir Gordiychuk
 */
public class FrameEncoderTest {

    @Test
    public void initFrameChangeWriterIndex() {
        BitBuf buffer = allocate();
        long frameIdx = initFrame(buffer);
        assertEquals(0, frameIdx);
        assertEquals(40, buffer.writerIndex());
    }

    @Test
    public void startedEmptyFrame() {
        BitBuf buffer = allocate();
        long frameIdx = initFrame(buffer);
        assertEquals(0, buffer.readerIndex());

        var it = new FrameIterator(buffer.duplicate());
        assertTrue(it.next());
        assertFalse(it.hasFooter());
        assertNotEquals(0, it.payloadIndex());
        assertEquals(0, it.payloadBits());
        assertFalse(it.next());
    }

    @Test
    public void startedWithDataFrame() {
        BitBuf buffer = allocate();
        long frameIdx = initFrame(buffer);
        assertEquals(0, buffer.readerIndex());

        buffer.writeBit(true);
        buffer.write32Bits(42);
        buffer.writeBit(false);

        var it = new FrameIterator(buffer.duplicate());
        assertTrue(it.next());
        assertFalse(it.next());
        assertFalse(it.hasFooter());
        assertNotEquals(0, it.payloadIndex());
        assertEquals(34, it.payloadBits());

        BitBuf payload = buffer.slice(it.payloadIndex(), it.payloadBits());
        assertTrue(payload.readBit());
        assertEquals(42, payload.read32Bits());
        assertFalse(payload.readBit());
        assertEquals(0, payload.readableBits());
    }

    @Test
    public void finishedWithDataFrame() {
        BitBuf buffer = allocate();
        long frameIdx = initFrame(buffer);
        assertEquals(0, buffer.readerIndex());

        buffer.writeBit(true);
        buffer.write32Bits(42);
        buffer.writeBit(false);

        FrameEncoder.finishFrame(buffer, frameIdx, (footer) -> {
            // state absent
        });

        var it = new FrameIterator(buffer.duplicate());

        assertTrue(it.next());
        assertFalse(it.next());
        assertTrue(it.hasFooter());
        assertNotEquals(0, it.payloadIndex());
        assertEquals(34, it.payloadBits());

        BitBuf payload = buffer.slice(it.payloadIndex(), it.payloadBits());
        assertTrue(payload.readBit());
        assertEquals(42, payload.read32Bits());
        assertFalse(payload.readBit());
        assertEquals(0, payload.readableBits());
        buffer.release();
    }

    @Test
    public void readFrameFooters() {
        BitBuf buffer = allocate();
        {
            // Frame №1
            long frameIdx = initFrame(buffer);
            buffer.writeBit(true);
            buffer.write32Bits(42);

            FrameEncoder.finishFrame(buffer, frameIdx, (buf) -> {
                buf.write64Bits(111);
                buf.writeBit(true);
            });
        }
        {
            // Frame №2
            long frameIdx = initFrame(buffer);
            buffer.writeBit(false);
            buffer.write32Bits(43);
            buffer.writeBit(false);
            buffer.write32Bits(44);

            FrameEncoder.finishFrame(buffer, frameIdx, (buf) -> {
                buf.write64Bits(444);
                buf.write32Bits(1);
                buf.writeBit(false);
            });
        }
        {
            // Frame №3
            long frameIdx = initFrame(buffer);
            buffer.writeBit(false);
        }


        var it = new FrameIterator(buffer.duplicate());
        {
            // Frame №1
            assertTrue(it.next());
            BitBuf payload = buffer.slice(it.payloadIndex(), it.payloadBits());
            assertTrue(payload.readBit());
            assertEquals(42, payload.read32Bits());
            assertEquals(0, payload.readableBits());

            assertTrue(it.hasFooter());
            it.readFooter(footer -> {
                assertEquals(111, footer.read64Bits());
                assertTrue(footer.readBit());
                return null;
            });
        }
        {
            // Frame №2
            assertTrue(it.next());
            BitBuf payload = buffer.slice(it.payloadIndex(), it.payloadBits());
            assertFalse(payload.readBit());
            assertEquals(43, payload.read32Bits());
            assertFalse(payload.readBit());
            assertEquals(44, payload.read32Bits());
            assertEquals(0, payload.readableBits());

            assertTrue(it.hasFooter());
            it.readFooter(footer -> {
                assertEquals(444, footer.read64Bits());
                assertEquals(1, footer.read32Bits());
                assertFalse(footer.readBit());
                return null;
            });
        }
        {
            // Frame №3
            assertTrue(it.next());
            BitBuf payload = buffer.slice(it.payloadIndex(), it.payloadBits());
            assertFalse(payload.readBit());
            assertEquals(0, payload.readableBits());

            assertFalse(it.hasFooter());
        }

        assertFalse(it.next());
    }

    @Test
    public void writeReadRandom() {
        for (int index = 0; index < 10; index++) {
            List<Content> list = randomContentList();

            BitBuf buffer = allocate();
            for (Content data : list) {
                writeFrame(buffer, data);
            }

            var it = new FrameIterator(buffer);
            for (Content data : list) {
                assertTrue(it.next());
                BitBuf payload = buffer.slice(it.payloadIndex(), it.payloadBits());
                data.read(payload);
                assertEquals(0, payload.readableBits());

                assertTrue(it.hasFooter());
                it.readFooter(data::readFooter);
                it.readFooter(data::readFooter);
            }
            assertFalse(it.next());
            buffer.release();
        }
    }

//    @Test
//    public void writeRestoreRandomOne() {
//        for (int index = 0; index < 10; index++) {
//            Content content = randomContent();
//
//            BitBuf buffer = allocate();
//            writeFrame(buffer, content);
//            FrameEncoder.restore(buffer, tsMillis -> content.readState(buffer, tsMillis));
//            buffer.write32Bits(42);
//
//            assertEquals(0, FrameEncoder.readFrameSize(buffer));
//            content.read(buffer);
//            assertEquals(42, buffer.read32Bits());
//            assertEquals(0, buffer.readableBits());
//        }
//    }
//
//    @Test
//    public void writeRestoreRandomLast() {
//        List<Content> list = randomContentList();
//
//        BitBuf buffer = allocate();
//        for (Content content : list) {
//            writeFrame(buffer, content);
//        }
//
//        FrameEncoder.restore(buffer, tsMillis -> list.get(list.size() - 1).readState(buffer, tsMillis));
//        buffer.write32Bits(33);
//        buffer.writeBit(true);
//
//        for (int index = 0; index < list.size() - 1; index++) {
//            long size = FrameEncoder.readFrameSize(buffer);
//            assertNotEquals(0, size);
//            list.get(index).read(buffer);
//            FrameEncoder.skipFooter(buffer);
//        }
//
//        assertEquals(0, FrameEncoder.readFrameSize(buffer));
//        list.get(list.size() - 1).read(buffer);
//        assertEquals(33, buffer.read32Bits());
//        assertTrue(buffer.readBit());
//        assertEquals(0, buffer.readableBits());
//    }
//
//    @Test
//    public void sliceNoFinishedFrames() {
//        BitBuf buffer = allocate();
//        FrameEncoder.initFrame(buffer);
//        buffer.write32Bits(2);
//        BitBuf result = FrameEncoder.slice(buffer, 0, System.currentTimeMillis());
//        assertEquals(buffer, result);
//    }
//
//    @Test
//    public void sliceOneFrame() {
//        BitBuf buffer = allocate();
//        Content content = randomContent();
//        writeFrame(buffer, content);
//
//        {
//            // 5 second range before
//            var result = FrameEncoder.slice(buffer, content.tsMillis - 15_000, content.tsMillis - 10_000);
//            assertEquals("range included into upper bound", buffer, result);
//            assertNotEquals(0, result.readableBits());
//        }
//
//        {
//            // 5 second range after
//            var result = FrameEncoder.slice(buffer, content.tsMillis + 10_000, content.tsMillis + 15_000);
//            assertEquals(0, result.readableBits());
//        }
//    }
//
//    @Test
//    public void sliceFewFrame() {
//        BitBuf buffer = allocate();
//        var source = IntStream.range(0, 8)
//            .mapToObj(ignore -> randomContent())
//            .sorted(Comparator.comparingLong(o -> o.tsMillis))
//            .collect(Collectors.toList());
//
//        long[] idx = new long[10];
//        for (int index = 0; index < source.size(); index++) {
//            idx[index] = buffer.writerIndex();
//            writeFrame(buffer, source.get(index));
//        }
//
//        // not finished frame
//        idx[9] = FrameEncoder.initFrame(buffer);
//        buffer.write64Bits(42);
//
//        {
//            // ()[0][1][2][3][4][5][6][7][8][9
//            long lastTsMillis = source.get(0).tsMillis;
//            var result = FrameEncoder.slice(buffer, lastTsMillis - 15_000, lastTsMillis - 10_000);
//            assertEquals(buffer.slice(0, idx[1]), result);
//
//            assertNotEquals(0, result.readableBits());
//        }
//
//        {
//            // ()[0][1][2][3][4][5][6][7][8][9()
//            long lastTsMillis = source.get(0).tsMillis;
//            var result = FrameEncoder.slice(buffer, lastTsMillis + 10_000, lastTsMillis + 15_000);
//            assertEquals(buffer.slice(idx[9], buffer.writerIndex() - idx[9]), result);
//            assertNotEquals(0, buffer.readableBits());
//        }
//
//        {
//            // [0][(1)][2][3][4][5][6][7][8][9
//            long from = source.get(0).tsMillis + 1;
//            long to = source.get(1).tsMillis - 1;
//            var result = FrameEncoder.slice(buffer, from, to);
//            assertEquals(buffer.slice(idx[1], idx[2] - idx[1]), result);
//        }
//
//        {
//            // [0][1][2][3][(4][5][6][7)][8][9
//            long from = source.get(3).tsMillis + 1;
//            long to = source.get(7).tsMillis - 1;
//
//            var result = FrameEncoder.slice(buffer, from, to);
//            assertEquals(buffer.slice(idx[4], idx[8] - idx[4]), result);
//        }
//
//        {
//            // [0][1][2][3][(4][5][6][7][)8][9
//            long from = source.get(3).tsMillis + 1;
//            long to = source.get(7).tsMillis;
//
//            var result = FrameEncoder.slice(buffer, from, to);
//            assertEquals(buffer.slice(idx[4], idx[9] - idx[4]), result);
//        }
//
//        {
//            // [0][1][2][3][4][5][6][7][(8][9)
//            long from = source.get(7).tsMillis + 1;
//            long to = from + 5_000;
//
//            var result = FrameEncoder.slice(buffer, from, to);
//            assertEquals(buffer.slice(idx[8], buffer.readableBits() - idx[8]), result);
//        }
//    }

    private void writeFrame(BitBuf buffer, Content content) {
        long idx = initFrame(buffer);
        content.write(buffer);
        FrameEncoder.finishFrame(buffer, idx, content::writeState);
    }

    public BitBuf allocate() {
        return BitBufAllocator.buffer(256);
    }

    public List<Content> randomContentList() {
        return IntStream.range(1, ThreadLocalRandom.current().nextInt(20))
            .mapToObj(ignore -> randomContent())
            .sorted(Comparator.comparingLong(o -> o.tsMillis))
            .collect(Collectors.toList());
    }

    public Content randomContent() {
        int bits = ThreadLocalRandom.current().nextInt(0, 8);
        int size = ThreadLocalRandom.current().nextInt(100);
        long[] values = new long[size];
        for (int index = 0; index < size; index++) {
            values[index] = ThreadLocalRandom.current().nextLong();
        }
        long tsMillis = ThreadLocalRandom.current().nextLong();
        return new Content(bits, values, tsMillis);
    }

    private class Content {
        int bits;
        long[] values;
        long tsMillis;

        public Content(int bits, long[] values, long tsMillis) {
            this.bits = bits;
            this.values = values;
            this.tsMillis = tsMillis;
        }

        public void write(BitBuf buffer) {
            buffer.writeBit(true);
            for (long value : values) {
                buffer.write64Bits(value);
            }
            buffer.writeBit(false);
            for (int index = 0; index < bits; index++) {
                buffer.writeBit(true);
            }
            buffer.writeBit(false);
        }

        public void read(BitBuf buffer) {
            assertTrue(buffer.readBit());
            for (long value : values) {
                assertEquals(value, buffer.read64Bits());
            }
            assertFalse(buffer.readBit());
            for (int index = 0; index < bits; index++) {
                assertTrue(buffer.readBit());
            }
            assertFalse(buffer.readBit());
        }

        public void writeState(BitBuf buf) {
            buf.write64Bits(tsMillis);
            buf.writeBit(true);
            buf.writeLongVarint8(bits);
            buf.writeLongVarint8(values.length);
        }

        public Void readFooter(BitBuf buf) {
            assertEquals(tsMillis, buf.read64Bits());
            assertTrue(buf.readBit());
            assertEquals(bits, buf.readLongVarint8());
            assertEquals(values.length, buf.readLongVarint8());
            return null;
        }
    }
}
