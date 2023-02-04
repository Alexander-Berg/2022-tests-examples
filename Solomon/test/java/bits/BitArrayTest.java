package ru.yandex.solomon.codec.bits;

import java.util.BitSet;
import java.util.Random;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ru.yandex.solomon.codec.bits.BitArray.toBits;

/**
 * @author Stepan Koltsov
 */
public class BitArrayTest {

    @Test
    public void test() {
        Random r = new Random(18);
        for (int i = 0; i < 1000; ++i) {
            int length = 1 + r.nextInt(100);
            byte[] bitArray = BitArray.allocate(length);
            BitSet bitSet = new BitSet(length);

            for (int j = 0; j < 100; ++j) {
                int index = r.nextInt(length);
                assertEquals(bitSet.get(index), BitArray.isBitSet(bitArray, index));
                bitSet.set(index);
                BitArray.setBit(bitArray, index);
            }
        }
    }

    @Test
    public void toBit() {
        assertEquals(0, toBits(0, 0));

        for (int bits = 1; bits < Byte.SIZE; bits++) {
            assertEquals(bits, toBits(1, bits));
        }

        assertEquals(953, toBits(120, 1));
    }

    @Test
    public void overflow() {
        assertEquals(2573922195L, BitArray.toBits(321740275, 3));
    }
}
