/*
 * (C) YANDEX LLC, 2014-2016
 *
 * The Source Code called "YoctoDB" available at
 * https://github.com/yandex/yoctodb is subject to the terms of the
 * Mozilla Public License, v. 2.0 (hereinafter referred to as the "License").
 *
 * A copy of the License is also available at http://mozilla.org/MPL/2.0/.
 */

package com.yandex.yoctodb.v1.immutable.segment;

import static org.junit.Assert.assertEquals;

import com.yandex.yoctodb.util.buf.Buffer;
import com.yandex.yoctodb.util.mutable.ArrayBitSetPool;
import com.yandex.yoctodb.util.mutable.impl.AllocatingArrayBitSetPool;
import java.util.NoSuchElementException;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

/**
 * Unit tests for {@link SegmentRegistry}
 *
 * @author incubos
 */
public class SegmentRegistryTest {
  private final Segment PROBE = new Segment() {};

  private final SegmentReader DUMMY =
      new SegmentReader() {
        @NotNull
        @Override
        public Segment read(
            @NotNull final Buffer buffer, @NotNull ArrayBitSetPool bitSetPool, int documentCount) {
          return PROBE;
        }
      };

  @Test(expected = NoSuchElementException.class)
  public void notExisting() {
    final int ID = -1;
    SegmentRegistry.read(ID, Buffer.from(new byte[] {}), AllocatingArrayBitSetPool.INSTANCE, 0);
  }

  @Test
  public void register() {
    final int ID = -1;
    SegmentRegistry.register(ID, DUMMY);
  }

  @Test
  public void registerAndReturn() {
    final int ID = -2;
    SegmentRegistry.register(ID, DUMMY);
    assertEquals(
        PROBE,
        SegmentRegistry.read(
            ID, Buffer.from(new byte[] {}), AllocatingArrayBitSetPool.INSTANCE, 0));
  }

  @Test(expected = IllegalArgumentException.class)
  public void registerExisting() {
    final int ID = -3;
    SegmentRegistry.register(ID, DUMMY);
    SegmentRegistry.register(ID, DUMMY);
  }
}
