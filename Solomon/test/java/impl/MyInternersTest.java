package ru.yandex.metabase.client.impl;

import java.util.BitSet;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Egor Litvinenko
 */
public class MyInternersTest {

    @Test
    public void testBitSetOne() {
        IntStream.rangeClosed(1, 1024).forEach(total -> {
            BitSet bitSet = new BitSet(total);
            int partitionId = total - 1;
            bitSet.set(partitionId);
            BitSet intern1 = MyInterners.bitset().intern(bitSet, total);
            BitSet intern2 = MyInterners.bitset().intern(bitSet, total);
            BitSet one = MyInterners.BitSetInterner.ofOne(partitionId);
            if (MyInterners.BitSetInterner.calcSmallPowerOfTwo(total) > -1 || total == 1) {
                Assert.assertTrue("" + total, intern1 == intern2);
                Assert.assertTrue("" + total, intern1 == one);
                Assert.assertFalse("" + total, intern1 == bitSet);
            } else {
                Assert.assertTrue("" + total, intern1 == intern2);
                Assert.assertTrue("" + total, intern1 == bitSet);
            }
            Assert.assertEquals("" + total, 1, intern2.cardinality());
            Assert.assertEquals("" + total, partitionId, intern2.nextSetBit(0));
            Assert.assertEquals("" + total, 1, bitSet.cardinality());
            Assert.assertEquals("" + total, partitionId, bitSet.nextSetBit(0));
        });
    }

    @Test
    public void testBitSetAll() {
        IntStream.rangeClosed(1, 1024).forEach(total -> {
            BitSet bitSet = new BitSet(total);
            for (int i = 0; i < total; i++) {
                bitSet.set(i);
            }
            BitSet intern1 = MyInterners.bitset().intern(bitSet, total);
            BitSet intern2 = MyInterners.bitset().intern(bitSet, total);
            BitSet all = MyInterners.BitSetInterner.ofAll(total);
            if (MyInterners.BitSetInterner.calcSmallPowerOfTwo(total) > -1 || total == 1) {
                Assert.assertTrue("" + total, intern1 == intern2);
                Assert.assertTrue("" + total, intern1 == all);
                Assert.assertFalse("" + total, intern1 == bitSet);
            } else {
                Assert.assertTrue("" + total, intern1 == intern2);
                Assert.assertTrue("" + total, intern1 == bitSet);
            }
            Assert.assertEquals("" + total, total, intern2.cardinality());
            Assert.assertEquals("" + total, total, intern2.cardinality());
            Assert.assertEquals("" + total, total, bitSet.cardinality());
            Assert.assertEquals("" + total, all.cardinality(), bitSet.cardinality());
        });
    }

    @Test
    public void testBitSetAllZeroes() {
        IntStream.rangeClosed(1, 1024).forEach(total -> {
            BitSet bitSet = new BitSet(total);
            for (int i = 0; i < total; i++) {
                bitSet.set(i, false);
            }
            BitSet intern1 = MyInterners.bitset().intern(bitSet, total);
            BitSet intern2 = MyInterners.bitset().intern(bitSet, total);
            BitSet allZeros = MyInterners.BitSetInterner.ofAllZeros(total);
            if (MyInterners.BitSetInterner.calcSmallPowerOfTwo(total) > -1 || total == 1) {
                Assert.assertTrue("" + total, intern1 == intern2);
                Assert.assertTrue("" + total, intern1 == allZeros);
                Assert.assertFalse("" + total, intern1 == bitSet);
            } else {
                Assert.assertTrue("" + total, intern1 == intern2);
                Assert.assertTrue("" + total, intern1 == bitSet);
            }
            Assert.assertEquals("" + total, 0, intern2.cardinality());
            Assert.assertEquals("" + total, 0, intern2.cardinality());
            Assert.assertEquals("" + total, 0, bitSet.cardinality());
            Assert.assertEquals("" + total, allZeros.cardinality(), bitSet.cardinality());
        });
    }

}
