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

import static com.yandex.yoctodb.util.UnsignedByteArrays.from;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertTrue;

import com.google.common.primitives.Ints;
import com.yandex.yoctodb.util.UnsignedByteArray;
import com.yandex.yoctodb.util.mutable.ByteArrayIndexedList;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import org.junit.Test;

/**
 * Unit tests for {@link FixedLengthByteArrayIndexedList}
 *
 * @author incubos
 */
public class FixedLengthByteArrayIndexedListTest {
  @Test(expected = AssertionError.class)
  public void expectFixed() throws IOException {
    new FixedLengthByteArrayIndexedList(asList(from(1), from(1L)))
        .writeTo(new ByteArrayOutputStream());
  }

  @Test
  public void string() {
    final Collection<UnsignedByteArray> elements = new LinkedList<>();
    final int size = 10;
    for (int i = 0; i < size; i++) elements.add(from(i));
    final ByteArrayIndexedList set = new FixedLengthByteArrayIndexedList(elements);
    final String text = set.toString();
    assertTrue(text.contains(Integer.toString(size)));
    assertTrue(text.contains(Integer.toString(Ints.BYTES)));
  }
}
