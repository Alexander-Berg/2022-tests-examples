/*
 * (C) YANDEX LLC, 2014-2016
 *
 * The Source Code called "YoctoDB" available at
 * https://github.com/yandex/yoctodb is subject to the terms of the
 * Mozilla Public License, v. 2.0 (hereinafter referred to as the "License").
 *
 * A copy of the License is also available at http://mozilla.org/MPL/2.0/.
 */

package com.yandex.yoctodb.util.immutable.impl;

import static com.yandex.yoctodb.util.UnsignedByteArrays.from;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.yandex.yoctodb.util.UnsignedByteArray;
import com.yandex.yoctodb.util.buf.Buffer;
import com.yandex.yoctodb.util.immutable.ByteArraySortedSet;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

/**
 * Unit tests for {@link VariableLengthByteArraySortedSet}
 *
 * @author incubos
 */
public class VariableLengthByteArraySortedSetTest {
  private final int VALUES = 128;

  private ByteArraySortedSet build() throws IOException {
    final SortedSet<UnsignedByteArray> elements = new TreeSet<>();
    for (int i = 0; i < VALUES; i++) {
      if (i % 2 == 0) elements.add(from(i / 2));
      else elements.add(from(i / 2L));
    }
    final com.yandex.yoctodb.util.mutable.ByteArraySortedSet mutable =
        new com.yandex.yoctodb.util.mutable.impl.VariableLengthByteArraySortedSet(elements);

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    mutable.writeTo(baos);

    final Buffer buf = Buffer.from(baos.toByteArray());

    final ByteArraySortedSet result = VariableLengthByteArraySortedSet.from(buf);

    assertEquals(VALUES, result.size());

    return result;
  }

  @Test
  public void string() throws IOException {
    final String text = build().toString();
    assertTrue(text.contains(Integer.toString(VALUES)));
  }
}
