package ru.yandex.solomon.codec.bits;

import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.misc.random.Random2;

/**
 * @author Stepan Koltsov
 */
public class BitStreamTest {

    private Random2 random = new Random2(17);

    private abstract class TestTemplate {
        protected abstract void write(BitBuf os);
        protected abstract void readAndCheck(BitBuf is);
    }

    private void randomTest(Supplier<TestTemplate> testTemplate) {
        for (int i = 0; i < 10000; ++i) {
            BitBuf bb = BitBufAllocator.buffer(20);
            int randomPrefixBits = random.nextInt(64);
            long randomPrefix = random.nextLong() & ((1 << randomPrefixBits) - 1);
            bb.writeBits(randomPrefix, randomPrefixBits);
            TestTemplate test = testTemplate.get();
            test.write(bb);
            Assert.assertEquals(randomPrefix, bb.readBitsToLong(randomPrefixBits));
            test.readAndCheck(bb);
            Assert.assertEquals(0, bb.readableBits());
            bb.release();
        }
    }

    @Test
    public void writeReadFloat() {
        randomTest(() -> new TestTemplate() {
            final float value;

            {
                for (;;) {
                    float value = Float.intBitsToFloat(random.nextInt());
                    if (!Float.isNaN(value)) {
                        this.value = value;
                        break;
                    }
                }
            }

            @Override
            protected void write(BitBuf os) {
                os.writeFloatBits(value);
            }

            @Override
            protected void readAndCheck(BitBuf is) {
                Assert.assertEquals(value, is.readFloatBits(), 0);
            }
        });
    }

    @Test
    public void writeReadIntVarint8() {
        randomTest(() -> new TestTemplate() {
            final int value = RandomTestInts.randomIntForTest(random.random);

            @Override
            protected void write(BitBuf os) {
                os.writeIntVarint8(value);
            }

            @Override
            protected void readAndCheck(BitBuf is) {
                Assert.assertEquals(value, is.readIntVarint8());
            }
        });
    }

    @Test
    public void writeReadLongVarint8() {
        randomTest(() -> new TestTemplate() {
            final long value = RandomTestInts.randomLongForTest(random.random);

            @Override
            protected void write(BitBuf os) {
                os.writeLongVarint8(value);
            }

            @Override
            protected void readAndCheck(BitBuf is) {
                Assert.assertEquals(value, is.readLongVarint8());
            }
        });
    }

    @Test
    public void writeReadIntVarint1N() {
        randomTest(() -> new TestTemplate() {
            final int max = random.nextInt(8);
            final int value = random.nextInt(max + 1);

            @Override
            protected void write(BitBuf os) {
                os.writeIntVarint1N(value, max);
            }

            @Override
            protected void readAndCheck(BitBuf is) {
                Assert.assertEquals(value, is.readIntVarint1N(max));
            }
        });
    }

    @Test
    public void writeReadBits8AsByte() {
        randomTest(() -> new TestTemplate() {
            final byte value = random.nextByte();

            @Override
            protected void write(BitBuf os) {
                os.write8Bits(value);
            }

            @Override
            protected void readAndCheck(BitBuf is) {
                Assert.assertEquals(value, is.read8Bits());
            }
        });
    }

    @Test
    public void writeReadBits64() {
        randomTest(() -> new TestTemplate() {
            final long value = random.nextLong();

            @Override
            protected void write(BitBuf os) {
                os.write64Bits(value);
            }

            @Override
            protected void readAndCheck(BitBuf is) {
                Assert.assertEquals(value, is.read64Bits());
            }
        });
    }

    @Test
    public void writeReadBits32() {
        randomTest(() -> new TestTemplate() {
            final int value = random.nextInt();

            @Override
            protected void write(BitBuf os) {
                os.write32Bits(value);
            }

            @Override
            protected void readAndCheck(BitBuf is) {
                Assert.assertEquals(value, is.read32Bits());
            }
        });
    }

    @Test
    public void writeReadBitsLong() {
        randomTest(() -> new TestTemplate() {
            final int count = random.nextInt(65);
            final long bits = count == 64 ? random.nextLong() : random.nextLong() & ((1L << count) - 1);

            @Override
            protected void write(BitBuf os) {
                os.writeBits(bits, count);
            }

            @Override
            protected void readAndCheck(BitBuf is) {
                long read = is.readBitsToLong(count);
                Assert.assertEquals("count: " + count, read, bits);
            }
        });
    }

    @Test
    public void writeReadBitsInt() {
        randomTest(() -> new TestTemplate() {
            final int count = random.nextInt(33);
            final int bits = count == 32 ? random.nextInt() : random.nextInt() & ((1 << count) - 1);

            @Override
            protected void write(BitBuf os) {
                os.writeBits(bits, count);
            }

            @Override
            protected void readAndCheck(BitBuf is) {
                int read = is.readBitsToInt(count);
                Assert.assertEquals("count: " + count, read, bits);
            }
        });
    }
}
