package ru.yandex.solomon.codec.bits;

import java.util.concurrent.ThreadLocalRandom;

import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import org.junit.Assert;
import org.junit.Test;
import org.openjdk.jol.info.GraphLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.misc.codec.Hex;
import ru.yandex.misc.random.Random2;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vladimir Gordiychuk
 */
public abstract class AbstractBitBufTest {
    private Logger logger = LoggerFactory.getLogger(AbstractBitBufTest.class);

    @Test
    public void writeBit() {
        {
            BitBuf os = allocate();
            os.writeBit(true);
            expectHex(os, 1, "01");
            os.writeBit(false);
            expectHex(os, 2, "01");
            os.writeBit(true);
            expectHex(os, 3, "05");
            os.release();
        }

        Random2 r = new Random2(17);

        for (int i = 0; i < 10000; ++i) {
            BitBuf os = allocate(r.nextBytes(r.nextInt(5)), 0);
            byte[] bytes = r.nextBytes(r.nextInt(5));
            for (byte b : bytes) {
                for (int j = 0; j < 8; ++j) {
                    os.writeBit((b & (1 << j)) != 0);
                }
            }
            expect(os, bytes, bytes.length * 8);
            os.release();
        }
    }

    @Test
    public void write8Bits() {
        BitBuf os = allocate();
        try {
            os.writeBit(false);
            os.writeBit(true);
            expectBin(os, "01");
            os.write8Bits((byte) 0b11111111);
            expectBin(os, "01 11111111");
            os.write8Bits((byte) 0b11110000);
            expectBin(os, "01 11111111 00001111");
            os.write8Bits((byte) 0b00001111);
            expectBin(os, "01 11111111 00001111 11110000");
            os.alignToByte();
            expectBin(os, "01 11111111 00001111 11110000 000000");
            os.write8Bits((byte) 0b10010110);
            expectBin(os, "01 11111111 00001111 11110000 000000 01101001");
        } finally {
            os.release();
        }
    }

    @Test
    public void write8BitsShift() {
        for (int num = 0; num < 255; num++) {
            for (int shift = 0; shift < 8; shift++) {
                BitBuf os = allocate();
                for (int index = 0; index < shift; index++) {
                    os.writeBit(false);
                }
                os.write8Bits(num);

                for (int index = 0; index < shift; index++) {
                    assertFalse("shift: " + shift + " pos " + index, os.readBit());
                }

                assertEquals(num, Byte.toUnsignedInt(os.read8Bits()));
                os.release();
            }
        }
    }

    @Test
    public void write8BitsInEmpty() {
        BitBuf os = allocate();
        try {
            os.write8Bits(0b1010_0111);
            expectBin(os, "11100101");
        } finally {
            os.release();
        }
    }

    @Test
    public void write8BitOverDirty() {
        BitBuf os = allocate(new byte[]{(byte) 0xff, (byte) 0xff}, 0);
        try {
            os.write8Bits(0);
            expectBin(os, "00000000");
        } finally {
            os.release();
        }
    }

    @Test
    public void writeBitsLong() {
        BitBuf os = allocate();
        try {
            os.writeBit(false);
            expectBin(os, "0");
            os.writeBits(0x8000000000000000L, 64);
            expectBin(os, "0 00000000 00000000 00000000 00000000 00000000 00000000 00000000 00000001");
        } finally {
            os.release();
        }
    }

    @Test
    public void write32Bits() {
        BitBuf os = allocate();
        try {
            os.writeBit(true);
            expectBin(os, "1");
            os.write32Bits(0b0100_0101_0010_0100_0101_1010_1010_1011);
            expectBin(os, "1 1101 0101 0101 1010 0010 0100 1010 0010");
        } finally {
            os.release();
        }
    }

    @Test
    public void write32BitsShift() {
        for (int num = 0; num < 255; num++) {
            int v = ThreadLocalRandom.current().nextInt();
            logger.info("==");
            logger.info("expected value: " + v);
            for (int shift = 0; shift < 8; shift++) {
                logger.info("shift: " + shift);
                BitBuf os = allocate();
                for (int index = 0; index < shift; index++) {
                    os.writeBit(false);
                }
                logger.info("Shifted: " + os);
                os.write32Bits(v);
                logger.info("Payload: " + os);

                for (int index = 0; index < shift; index++) {
                    assertFalse("shift: " + shift + " pos " + index, os.readBit());
                }

                logger.info("Read:    " + os);
                assertEquals(v, os.read32Bits());
                os.release();
            }
        }
    }

    @Test
    public void write64Bits() {
        BitBuf os = allocate();
        try {
            os.writeBit(false);
            expectBin(os, "0");
            os.write64Bits(0b0101_0101_0010_0100_0101_1010_1010_1011_0000_0101_0010_0100_0101_1010_1010_0011L);
            expectBin(os, "0 1100 0101 0101 1010 0010 0100 1010 0000 1101 0101 0101 1010 0010 0100 1010 1010");
        } finally {
            os.release();
        }
    }

    @Test
    public void writeDoubleBits() {
        BitBuf os = allocate();
        try {
            os.writeBit(false);
            os.writeBit(true);
            expectBin(os, "01");

            long idx = os.writerIndex();
            os.writeDoubleBits(Double.MIN_NORMAL);
            expectBin(os, "01 00000000 00000000 00000000 00000000 00000000 00000000 00001000 00000000");

            os.writerIndex(idx);
            os.writeDoubleBits(Double.MIN_VALUE);
            expectBin(os, "01 10000000 00000000 00000000 00000000 00000000 00000000 00000000 00000000");

            os.writerIndex(idx);
            os.writeDoubleBits(Double.MAX_VALUE);
            expectBin(os, "01 11111111 11111111 11111111 11111111 11111111 11111111 11110111 11111110");

            os.writerIndex(idx);
            os.writeDoubleBits(Double.POSITIVE_INFINITY);
            expectBin(os, "01 00000000 00000000 00000000 00000000 00000000 00000000 00001111 11111110");

            os.writerIndex(idx);
            os.writeDoubleBits(Double.NEGATIVE_INFINITY);
            expectBin(os, "01 00000000 00000000 00000000 00000000 00000000 00000000 00001111 11111111");

            os.writerIndex(idx);
            os.writeDoubleBits(Double.NaN);
            expectBin(os, "01 00000000 00000000 00000000 00000000 00000000 00000000 00011111 11111110");

            os.writerIndex(idx);
            os.writeDoubleBits(Math.E);
            expectBin(os, "01 10010110 11101010 00101000 11010001 01010000 11111101 10100000 00000010");

            os.writerIndex(idx);
            os.writeDoubleBits(Math.PI);
            expectBin(os, "01 00011000 10110100 00100010 00101010 11011111 10000100 10010000 00000010");
        } finally {
            os.release();
        }
    }

    @Test
    public void writeBitsCountShift() {
        for (int cnt = 1; cnt < Long.SIZE; cnt++) {
            long max = (1 << cnt) - 1;
            logger.info("==");
            logger.info("expected value: " + max);
            for (int shift = 0; shift < 8; shift++) {
                logger.info("shift: " + shift);
                BitBuf os = allocate();
                for (int index = 0; index < shift; index++) {
                    os.writeBit(false);
                }
                logger.info("Shifted: " + os);
                os.writeBits(max, cnt);
                logger.info("Payload: " + os);

                for (int index = 0; index < shift; index++) {
                    assertFalse("shift: " + shift + " pos " + index, os.readBit());
                }

                logger.info("Read:    " + os);
                assertEquals(os.toString(), max, os.readBitsToLong(cnt));
                os.release();
            }
        }
    }

    @Test
    public void write64BitsShift() {
        for (int num = 0; num < 255; num++) {
            long v = ThreadLocalRandom.current().nextLong();
            for (int shift = 0; shift < 8; shift++) {
                BitBuf os = allocate();
                for (int index = 0; index < shift; index++) {
                    os.writeBit(false);
                }
                os.write64Bits(v);

                for (int index = 0; index < shift; index++) {
                    assertFalse("shift: " + shift + " pos " + index, os.readBit());
                }

                assertEquals(v, os.read64Bits());
                os.release();
            }
        }
    }

    @Test
    public void writeIntVarint1N() {
        BitBuf os = allocate();
        try {
            os.writeIntVarint1N(0, 3);
            expectBin(os, "0");
            os.writeIntVarint1N(1, 3);
            expectBin(os, "0 10");
            os.writeIntVarint1N(2, 3);
            expectBin(os, "0 10 110");
            os.writeIntVarint1N(3, 3);
            expectBin(os, "0 10 110 111");
        } finally {
            os.release();
        }
    }

    @Test
    public void writeIntVarint1N2() {
        for (int num = 0; num < 8; num++) {
            for (int shift = 0; shift < 8; shift++) {
                BitBuf os = allocate();
                for (int index = 0; index < shift; index++) {
                    os.writeBit(false);
                }
                os.writeIntVarint1N(num, 8);

                for (int index = 0; index < shift; index++) {
                    assertFalse("shift: " + shift + " pos " + index, os.readBit());
                }

                assertEquals(num, os.readIntVarint1N(8));
            }
        }
    }

    @Test
    public void writeIntVarint8() {
        BitBuf os = allocate();
        os.writeIntVarint8(0);
        expectBin(os, "00000000");
        os.writeIntVarint8(3);
        expectBin(os, "00000000 11000000");
        os.release();
        os = allocate();
        os.writeIntVarint8(-5);
        expectBin(os, "11011111 11111111 11111111 11111111 11110000");
        os.release();
    }

    @Test
    public void writeIntVarint8Shift() {
        for (int num = 0; num < 255; num++) {
            int v = ThreadLocalRandom.current().nextInt();
            for (int shift = 0; shift < 8; shift++) {
                BitBuf os = allocate();
                for (int index = 0; index < shift; index++) {
                    os.writeBit(false);
                }
                os.writeIntVarint8(v);

                for (int index = 0; index < shift; index++) {
                    assertFalse("shift: " + shift + " pos " + index, os.readBit());
                }

                assertEquals(v, os.readIntVarint8());
                os.release();
            }
        }
    }

    @Test
    public void writeLongVarint8() {
        BitBuf os = allocate();
        os.writeLongVarint8(0);
        expectBin(os, "00000000");
        os.writeLongVarint8(3);
        expectBin(os, "00000000 11000000");
        os.release();
        os = allocate();
        os.writeLongVarint8(-5);
        expectBin(os, "11011111 11111111 11111111 11111111 11111111 11111111 11111111 11111111 11111111 10000000");
        os.release();
    }

    @Test
    public void emptySelfSize() {
        BitBuf out = allocate();
        try {
            GraphLayout layout = GraphLayout.parseInstance(out);
            assertEquals(layout.totalSize(), out.memorySizeIncludingSelf(), 400);
        } finally {
            out.release();
        }

    }

    @Test
    public void selfSizeWithValues() {
        BitBuf out = allocate();
        try {
            for (int index = 0; index < 100; index++) {
                out.write64Bits(index);
            }

            GraphLayout layout = GraphLayout.parseInstance(out);
            assertEquals(layout.totalSize(), out.memorySizeIncludingSelf(), 400);
        } finally {
            out.release();
        }
    }

    @Test
    public void bytesCount() {
        BitBuf out = allocate();
        assertEquals(0, out.bytesSize());

        for (int index = 0; index < Byte.SIZE; index++) {
            out.writeBit(true);
            assertEquals(1, out.bytesSize());
        }

        for (int index = 0; index < Byte.SIZE; index++) {
            out.writeBit(true);
            assertEquals(2, out.bytesSize());
        }
        out.release();
    }

    @Test
    public void readBit() {
        String bits = "1011 0011 01";
        BitBuf is = parse(bits);

        BooleanArrayList r = new BooleanArrayList();
        while (is.readableBits() > 0) {
            r.add(is.readBit());
        }

        assertArrayEquals(BitStreamUtils.parseBits(bits), r.toBooleanArray());
        is.release();
    }

    @Test
    public void readBitsToLong() {
        BitBuf is = parse("11001");
        assertEquals(0b011, is.readBitsToInt(3));
        assertEquals(0b10, is.readBitsToInt(2));
        assertEquals(0L, is.readableBits());
        is.release();
    }

    @Test
    public void readBitsToLongFromEmpty() {
        BitBuf is = parse("");
        assertEquals(0, is.readBitsToLong(0));
        assertEquals(0L, is.readableBits());
        is.release();
    }

    @Test
    public void read32Bits() {
        BitBuf is = parse("1 1101 0011 0100 0101 1010 1011 1100 0011");
        assertEquals(true, is.readBit());
        assertEquals(0b1100_0011_1101_0101_1010_0010_1100_1011, is.read32Bits());
        assertEquals(0L, is.readableBits());
        is.release();
    }

    @Test
    public void readIntVarint1N() {
        BitBuf is = parse("0 10 110 111 0");
        for (int i = 0; i <= 3; ++i) {
            assertEquals(i, is.readIntVarint1N(3));
        }
        assertEquals(0, is.readIntVarint1N(4));
        assertEquals(0L, is.readableBits());
        is.release();
    }

    @Test
    public void read8BitsToByteUnaligned() {
        BitBuf is = parse("1 1010 1000 0101 0001");
        assertTrue(is.readBit());
        assertEquals((byte) 0b0001_0101, is.read8Bits());
        assertEquals((byte) 0b1000_1010, is.read8Bits());
        assertEquals(0L, is.readableBits());
        is.release();
    }

    @Test
    public void read8BitsToByteAligned() {
        BitBuf is = parse("1010 1000 0101 0001");
        assertEquals((byte) 0b0001_0101, is.read8Bits());
        assertEquals((byte) 0b1000_1010, is.read8Bits());
        assertEquals(0L, is.readableBits());
        is.release();
    }

    @Test
    public void writeReadBit() {
        BitBuf buf = allocate();
        buf.writeBit(true);
        buf.writeBit(true);
        buf.writeBit(false);
        buf.writeBit(true);

        assertTrue(buf.readBit());
        assertTrue(buf.readBit());
        assertFalse(buf.readBit());
        assertTrue(buf.readBit());
        assertEquals(0, buf.readableBits());
        buf.release();
    }

    @Test
    public void writeRead8Bits() {
        BitBuf buf = allocate();
        int bits = (int) ((byte) ThreadLocalRandom.current().nextInt());
        buf.write8Bits(bits);
        assertEquals(bits, buf.read8Bits());
        assertEquals(0, buf.readableBits());
        buf.release();
    }

    @Test
    public void writeRead32Bits() {
        BitBuf buf = allocate();
        int bits = ThreadLocalRandom.current().nextInt();
        buf.write32Bits(bits);
        assertEquals(bits, buf.read32Bits());
        assertEquals(0, buf.readableBits());

        buf.writeBit(false);
        buf.write32Bits(bits);

        assertFalse(buf.readBit());
        assertEquals(bits, buf.read32Bits());
        assertEquals(0, buf.readableBits());
        buf.release();
    }

    @Test
    public void writeRead64Bits() {
        BitBuf buf = allocate();
        long bits = ThreadLocalRandom.current().nextLong();
        buf.write64Bits(bits);
        assertEquals(bits, buf.read64Bits());
        assertEquals(0, buf.readableBits());

        buf.writeBit(true);
        buf.write64Bits(bits);

        assertTrue(buf.readBit());
        assertEquals(bits, buf.read64Bits());
        assertEquals(0, buf.readableBits());
        buf.release();
    }

    @Test
    public void writeReadIntVarint8() {
        BitBuf buf = allocate();

        for (int index = 0; index < 10; index++) {
            int bits = ThreadLocalRandom.current().nextInt();
            buf.writeIntVarint8(bits);
            buf.writeBit(true);

            assertEquals(bits, buf.readIntVarint8());
            assertTrue(buf.readBit());
            assertEquals(0, buf.readableBits());
        }
        buf.release();
    }

    @Test
    public void writeReadLongVarint8() {
        BitBuf buf = allocate();

        for (int index = 0; index < 10; index++) {
            long bits = ThreadLocalRandom.current().nextLong();
            buf.writeLongVarint8(bits);
            buf.writeBit(true);

            assertEquals(bits, buf.readLongVarint8());
            assertTrue(buf.readBit());
            assertEquals(0, buf.readableBits());
        }
        buf.release();
    }

    @Test
    public void writeReadIntVarint1N() {
        BitBuf buf = allocate();

        int max = 8;
        for (int index = 0; index < 10; index++) {
            int cnt = ThreadLocalRandom.current().nextInt(0, max);
            buf.writeIntVarint1N(cnt, max);
            buf.writeBit(true);

            assertEquals(cnt, buf.readIntVarint1N(max));
            assertTrue(buf.readBit());
            assertEquals(0, buf.readableBits());
        }
        buf.release();
    }

    @Test
    public void readableBits() {
        BitBuf bb = allocate();
        Assert.assertEquals(0, bb.readableBits());
        bb.writeBit(true);
        bb.writeBit(true);
        Assert.assertEquals(2, bb.readableBits());
        bb.readBit();
        Assert.assertEquals(1, bb.readableBits());
        bb.readBit();
        Assert.assertEquals(0, bb.readableBits());
        bb.release();
    }

    @Test
    public void resetReaderIndex() {
        BitBuf buffer = allocate();
        buffer.write32Bits(1);
        for (int index = 0; index < 32; index++) {
            buffer.readBit();
        }
        buffer.resetReadIndex();
        assertEquals(1, buffer.read32Bits());
        buffer.release();
    }

    @Test
    public void readOnly() {
        BitBuf buffer = allocate();
        buffer.writeBit(true);
        buffer.write32Bits(42);

        BitBuf readOnly = buffer.asReadOnly();
        try {
            readOnly.writeBit(false);
            fail("not able write via read only buffer");
        } catch (AssertionError e) {
            throw e;
        } catch (Throwable e) {
            // it's ok
        }

        assertTrue(readOnly.readBit());
        assertEquals(42, readOnly.read32Bits());
        assertEquals(readOnly.readableBits(), 0);

        buffer.write32Bits(43);
        assertEquals(readOnly.readableBits(), 0);
        assertTrue(buffer.readBit());
        assertEquals(42, buffer.read32Bits());
        assertEquals(43, buffer.read32Bits());
        buffer.release();
    }

    @Test
    public void changeReadIndex() {
        BitBuf buffer = allocate();

        buffer.writeBit(true);
        buffer.writeBit(true);
        buffer.writeBit(false);
        buffer.write32Bits(42);
        buffer.writeBit(true);

        assertEquals(0, buffer.readerIndex());
        assertTrue(buffer.readBit());
        assertEquals(1, buffer.readerIndex());

        buffer.readerIndex(2);
        assertEquals(2, buffer.readerIndex());
        assertFalse(buffer.readBit());

        buffer.readerIndex(1);
        assertTrue(buffer.readBit());

        buffer.readerIndex(3);
        assertEquals(42, buffer.read32Bits());
        assertTrue(buffer.readBit());
        assertEquals(0, buffer.readableBits());
        buffer.release();
    }

    @Test
    public void changeWriteIndex() {
        BitBuf buffer = allocate();
        assertEquals(0, buffer.writerIndex());

        buffer.write8Bits(123);
        assertEquals(8, buffer.writerIndex());

        buffer.write32Bits(0);
        assertEquals(40, buffer.writerIndex());

        buffer.writeBit(true);
        assertEquals(41, buffer.writerIndex());

        buffer.writeBit(false);
        assertEquals(42, buffer.writerIndex());

        buffer.writerIndex(8);
        buffer.write32Bits(55);
        buffer.writerIndex(42);

        assertEquals(123, buffer.read8Bits());
        assertEquals(55, buffer.read32Bits());
        assertTrue(buffer.readBit());
        assertFalse(buffer.readBit());
        assertEquals(0, buffer.readableBits());
        buffer.release();
    }

    @Test
    public void slice() {
        BitBuf buffer = allocate();

        buffer.writeBit(true);
        buffer.write32Bits(42);

        buffer.alignToByte();

        buffer.writeBit(false);
        buffer.write32Bits(43);

        BitBuf first = buffer.slice(0, 40);
        assertEquals(40, first.readableBits());

        BitBuf last = buffer.slice(40, 33);
        assertEquals(33, last.readableBits());

        assertTrue(first.readBit());
        assertEquals(42, first.read32Bits());
        assertEquals(7, first.readableBits());

        assertFalse(last.readBit());
        assertEquals(43, last.read32Bits());
        assertEquals(0, last.readableBits());

        assertEquals(73, buffer.readableBits());
        assertTrue(buffer.readBit());
        assertEquals(42, buffer.read32Bits());
        assertEquals(40, buffer.readableBits());
        buffer.release();
    }

    @Test
    public void duplicate() {
        BitBuf buffer = allocate();

        buffer.writeBit(true);
        buffer.write32Bits(42);
        buffer.alignToByte();
        buffer.write32Bits(43);
        buffer.writeBit(false);

        assertTrue(buffer.readBit());

        BitBuf duplicate = buffer.duplicate();

        // duplicate have same readIdx/writeIdx
        assertEquals(1, buffer.readerIndex());
        assertEquals(73, buffer.writerIndex());

        assertEquals(1, duplicate.readerIndex());
        assertEquals(73, duplicate.writerIndex());

        // readIdx/writeIdx of duplicate independent on source
        buffer.writeBit(true);
        assertEquals(42, buffer.read32Bits());
        assertEquals(33, buffer.readerIndex());
        assertEquals(74, buffer.writerIndex());

        assertEquals(1, duplicate.readerIndex());
        assertEquals(73, duplicate.writerIndex());

        // changes into source visible into duplicate
        buffer.writerIndex(40);
        buffer.write32Bits(45);
        buffer.writeBit(true);
        buffer.writeBit(false);

        duplicate.readerIndex(40);
        assertEquals(45, duplicate.read32Bits());
        assertTrue(duplicate.readBit());
        assertEquals(
            "latest bit not visible, because duplicate writerIdx < source writerIdx",
            0, duplicate.readableBits());
        buffer.release();
    }

    @Test
    public void copy() {
        BitBuf buffer = allocate();

        buffer.writeBit(true);
        buffer.write32Bits(42);
        buffer.alignToByte();
        buffer.write32Bits(43);
        buffer.writeBit(false);

        buffer.readerIndex(40);
        BitBuf copy = buffer.copy();

        // copy have different readIndex/writeIndex
        assertEquals(40, buffer.readerIndex());
        assertEquals(73, buffer.writerIndex());

        assertEquals(0, copy.readerIndex());
        assertEquals(33, copy.writerIndex());

        // change source not affect copy
        buffer.writerIndex(40);
        buffer.write32Bits(45);
        buffer.writeBit(true);

        assertEquals(45, buffer.read32Bits());
        assertTrue(buffer.readBit());
        assertEquals(0, buffer.readableBits());

        assertEquals(0, copy.readerIndex());
        assertEquals(33, copy.writerIndex());

        assertEquals(43, copy.read32Bits());
        assertFalse(copy.readBit());
        assertEquals(0, copy.readableBits());
        buffer.release();
    }

    @Test
    public void copyRange() {
        BitBuf buffer = allocate();
        buffer.writeBit(true);
        buffer.write32Bits(42);

        assertTrue(buffer.readBit());

        buffer.alignToByte();
        buffer.write32Bits(43);
        buffer.writeBit(false);

        BitBuf copy = buffer.copy(40, 33);

        assertEquals(0, copy.readerIndex());
        assertEquals(33, copy.writerIndex());
        assertEquals(1, buffer.readerIndex());
        assertEquals(40 + 33, buffer.writerIndex());

        assertEquals(43, copy.read32Bits());
        assertFalse(copy.readBit());

        assertEquals(42, buffer.read32Bits());
        buffer.release();
    }

    @Test
    public void copyMutable() {
        BitBuf buffer = allocate();
        buffer.write32Bits(42);

        BitBuf readOnly = buffer.asReadOnly();
        BitBuf copy = readOnly.copy();

        copy.write32Bits(43);
        assertEquals(42, copy.read32Bits());
        assertEquals(43, copy.read32Bits());
        buffer.release();
    }

    @Test
    public void readOnlyDuplicate() {
        BitBuf buffer = allocate();

        buffer.writeBit(true);
        buffer.write32Bits(42);

        BitBuf readOnly = buffer.asReadOnly();
        BitBuf duplicate = readOnly.duplicate();

        try {
            duplicate.writeBit(false);
            fail("not able write via read only buffer");
        } catch (AssertionError e) {
            throw e;
        } catch (Throwable e) {
            // it's ok
        }
        buffer.release();
    }

    @Test
    public void skipBits() {
        for (int shift = 0; shift < Byte.SIZE * 100; shift++) {
            BitBuf buffer = allocate();
            for (int index = 0; index < shift; index++) {
                buffer.writeBit(false);
            }
            int expected = ThreadLocalRandom.current().nextInt();
            buffer.write32Bits(expected);
            buffer.skipBits(shift);
            int result = buffer.read32Bits();
            assertEquals(expected, result);
            buffer.release();
        }
    }

    @Test
    public void eq() {
        BitBuf left = allocate();
        BitBuf right = allocate();

        assertEquals(left, right);
        assertEquals(right, left);

        left.writeIntVarint8(42);

        assertNotEquals(left, right);
        assertNotEquals(right, left);

        right.writeIntVarint8(42);

        assertEquals(left, right);
        assertEquals(right, left);

        left.writeIntVarint8(ThreadLocalRandom.current().nextInt());
        right.writeIntVarint8(ThreadLocalRandom.current().nextInt());

        assertNotEquals(left, right);
        assertNotEquals(right, left);

        left.write64Bits(ThreadLocalRandom.current().nextLong());

        assertNotEquals(left, right);
        assertNotEquals(right, left);
        left.release();
        right.release();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void writeBitsBitBufShifted() {
        for (int bitsAtSrc = 0; bitsAtSrc < 8; bitsAtSrc++) {
            var src = allocate();
            src.write32Bits(42);
            for (int index = 0; index < bitsAtSrc; index++) {
                src.writeBit(true);
            }

            for (int bitsAtTarget = 0; bitsAtTarget < 8; bitsAtTarget++) {
                var target = allocate();
                target.write32Bits(256);
                for (int index = 0; index < bitsAtTarget; index++) {
                    target.writeBit(true);
                }

                var copySrc = src.copy();
                var expected = target.copy();
                while (copySrc.readableBits() > 0) {
                    expected.writeBit(copySrc.readBit());
                }

                logger.debug("src: {}", src);
                logger.debug("target: {}", target);
                logger.debug("expected: {}", expected);

                target.writeBits(src, 0, src.readableBits());
                assertEquals(expected, target);
                copySrc.release();
                expected.release();
                target.release();
            }
            src.release();
        }
    }

    @Test
    public void writeBitsEmptyBitBuf() {
        var target = allocate();

        target.writeBits(allocate());
        assertEquals(0, target.writerIndex());
        assertEquals(0, target.readerIndex());
        target.release();
    }

    @Test
    public void writeBitsBitBuf() {
        var target = allocate();
        var src = allocate();
        var expected = allocate();

        for (int index = 0; index < 100; index++) {
            src.write32Bits(index);
            expected.write32Bits(index);
            target.writeBits(src);
            assertEquals(expected, target);
        }
        target.release();
        src.release();
        expected.release();
    }

    @Test
    public void writeBitBuf() {
        var target = allocate();
        target.writeBit(false);
        target.alignToByte();
        logger.debug("d: {}", target);

        var src = allocate();
        src.writeBit(true);
        src.write32Bits(42);
        src.write64Bits(256);
        logger.debug("s: {}", src);

        target.writeBits(src);
        assertEquals(src.readableBits(), 0);
        logger.debug("r: {}", target);

        assertEquals(8 + src.writerIndex(), target.writerIndex());
        assertFalse(target.readBit());
        target.skipBits(7);
        assertTrue(target.readBit());
        assertEquals(42, target.read32Bits());
        assertEquals(256, target.read64Bits());
        assertEquals(0, target.readableBits());
        target.release();
        src.release();
    }

    @Test
    public void eqToReadOnly() {
        var target = allocate();
        target.writeBit(ThreadLocalRandom.current().nextBoolean());
        target.write32Bits(ThreadLocalRandom.current().nextInt());

        var readOnly = target.asReadOnly();
        assertEquals(target, readOnly);
        assertEquals(readOnly, target);
        target.release();
    }

    @Test
    public void asReadOnlySameBehaviour() {
        BitBuf buffer = allocate();
        buffer.writeBit(true);
        buffer.write32Bits(42);

        BitBuf readOnly = buffer.asReadOnly();
        try {
            readOnly.writeBit(false);
            fail("not able write via read only buffer");
        } catch (AssertionError e) {
            throw e;
        } catch (Throwable e) {
            // it's ok
        }

        // read only duplicate of already read only buffer
        BitBuf duplicate = readOnly.asReadOnly();

        assertEquals(33, readOnly.readableBits());
        assertTrue(readOnly.readBit());
        assertEquals(42, readOnly.read32Bits());
        assertEquals(readOnly.readableBits(), 0);

        assertEquals(33, duplicate.readableBits());
        assertTrue(duplicate.readBit());
        assertEquals(42, duplicate.read32Bits());
        assertEquals(duplicate.readableBits(), 0);

        buffer.write32Bits(43);
        assertEquals(0, readOnly.readableBits());
        assertEquals(0, duplicate.readableBits());
        buffer.release();
    }

    @Test
    public void invaliSizeOverflowInt() {
        Throwable error = null;
        try {
            allocate(new byte[18], 5910974510923918L);
        } catch (Throwable e) {
            error = e;
        }
        assertNotNull(error);
    }

    @Test
    public void invalidSize() {
        Throwable error = null;
        try {
            allocate(new byte[0], 100);
        } catch (Throwable e) {
            error = e;
        }
        assertNotNull(error);
    }

    private void expect(BitBuf buf, byte[] bytes, int expectedBitLength) {
        var expect = allocate(bytes, expectedBitLength);
        try {
            assertEquals(expect, buf);
        } finally {
            expect.release();
        }
    }

    private void expectHex(BitBuf buf, int expectedBitLength, String expectedBytes) {
        byte[] bytes = Hex.decodeHr(expectedBytes);
        expect(buf, bytes, expectedBitLength);
    }

    private void expectBin(BitBuf buf, String bits) {
        var expect = parse(bits);
        try {
            assertEquals(expect, buf);
        } finally {
            expect.release();
        }
    }

    private BitBuf parse(String bits) {
        return BitStreamUtils.parse(bits, allocate());
    }

    protected abstract BitBuf allocate();

    protected abstract BitBuf allocate(byte[] array, long expectedBitLength);
}
