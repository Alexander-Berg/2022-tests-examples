/*
 * (C) YANDEX LLC, 2014-2016
 *
 * The Source Code called "YoctoDB" available at
 * https://github.com/yandex/yoctodb is subject to the terms of the
 * Mozilla Public License, v. 2.0 (hereinafter referred to as the "License").
 *
 * A copy of the License is also available at http://mozilla.org/MPL/2.0/.
 */

package com.yandex.yoctodb.util.mutable.impl;

import static org.junit.Assert.*;

import com.yandex.yoctodb.util.buf.Buffer;
import com.yandex.yoctodb.util.mutable.BitSet;
import org.junit.Test;

/**
 * Unit tests for {@link ReadOnlyZeroBitSet}
 *
 * @author incubos
 */
public class ReadOnlyZeroBitSetTest {
  private final int SIZE = 1024;

  @Test
  public void empty() {
    for (int i = 0; i < SIZE; i++) assertTrue(new ReadOnlyZeroBitSet(i).isEmpty());
  }

  @Test
  public void size() {
    for (int i = 0; i < SIZE; i++) assertEquals(i, new ReadOnlyZeroBitSet(i).getSize());
  }

  @Test
  public void cardinality() {
    for (int i = 0; i < SIZE; i++) assertEquals(0, new ReadOnlyZeroBitSet(i).cardinality());
  }

  @Test
  public void get() {
    for (int i = 1; i < SIZE; i++) {
      final BitSet bs = new ReadOnlyZeroBitSet(i);
      assertFalse(bs.get(0));
      assertFalse(bs.get(i / 2));
      assertFalse(bs.get(i - 1));
    }
  }

  @Test(expected = AssertionError.class)
  public void invalidGet() {
    final BitSet bs = new ReadOnlyZeroBitSet(1);
    bs.get(1);
  }

  @Test(expected = AssertionError.class)
  public void invalidNegativeGet() {
    final BitSet bs = new ReadOnlyZeroBitSet(1);
    bs.get(-1);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void unsupportedPointSet() {
    new ReadOnlyZeroBitSet(1).set(0);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void unsupportedClear() {
    new ReadOnlyZeroBitSet(1).clear();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void unsupportedInverse() {
    new ReadOnlyZeroBitSet(1).inverse();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void unsupportedSet() {
    new ReadOnlyZeroBitSet(1).set();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void unsupportedAnd() {
    new ReadOnlyZeroBitSet(1).and(LongArrayBitSet.one(1));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void unsupportedOr() {
    new ReadOnlyZeroBitSet(1).or(LongArrayBitSet.zero(1));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void unsupportedXor() {
    new ReadOnlyZeroBitSet(1).xor(LongArrayBitSet.zero(1));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void unsupportedAndBuffer() {
    new ReadOnlyZeroBitSet(1).and(Buffer.from(new byte[] {0, 0, 0, 0, 0, 0, 0, 0}), 0, 1);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void unsupportedXorBuffer() {
    new ReadOnlyZeroBitSet(1).xor(Buffer.from(new byte[] {0, 0, 0, 0, 0, 0, 0, 0}), 0, 1);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void unsupportedOrBuffer() {
    new ReadOnlyZeroBitSet(1).or(Buffer.from(new byte[] {0, 0, 0, 0, 0, 0, 0, 0}), 0, 1);
  }

  @Test
  public void nextSetBit() {
    final BitSet bs = new ReadOnlyZeroBitSet(SIZE);
    for (int i = 0; i < SIZE; i++) {
      assertEquals(-1, bs.nextSetBit(i));
    }
    assertEquals(-1, bs.nextSetBit(SIZE));
  }

  @Test(expected = AssertionError.class)
  public void invalidNextSetBit() {
    final BitSet bs = new ReadOnlyZeroBitSet(SIZE);
    bs.nextSetBit(-1);
  }
}
