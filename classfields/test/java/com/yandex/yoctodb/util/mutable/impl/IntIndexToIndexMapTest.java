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

import static org.junit.Assert.assertTrue;

import com.yandex.yoctodb.util.mutable.IndexToIndexMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.Test;

/**
 * Unit tests for {@link IntIndexToIndexMap}
 *
 * @author incubos
 */
public class IntIndexToIndexMapTest {
  @Test(expected = IllegalArgumentException.class)
  public void negativeKey() {
    new IntIndexToIndexMap().put(-1, 0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void negativeValue() {
    new IntIndexToIndexMap().put(0, -1);
  }

  @Test(expected = IllegalStateException.class)
  public void nonContinuous() throws IOException {
    final IndexToIndexMap idx = new IntIndexToIndexMap();
    idx.put(0, 0);
    idx.put(2, 0);
    idx.writeTo(new ByteArrayOutputStream());
  }

  @Test(expected = IllegalArgumentException.class)
  public void overwrite() {
    final IndexToIndexMap idx = new IntIndexToIndexMap();
    idx.put(0, 0);
    idx.put(0, 0);
  }

  @Test
  public void string() {
    final IndexToIndexMap idx = new IntIndexToIndexMap();
    idx.put(0, 0);
    idx.put(1, 0);
    assertTrue(idx.toString().contains("2"));
  }
}
