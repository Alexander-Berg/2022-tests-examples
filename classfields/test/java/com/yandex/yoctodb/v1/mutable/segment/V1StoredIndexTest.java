/*
 * (C) YANDEX LLC, 2014-2016
 *
 * The Source Code called "YoctoDB" available at
 * https://github.com/yandex/yoctodb is subject to the terms of the
 * Mozilla Public License, v. 2.0 (hereinafter referred to as the "License").
 *
 * A copy of the License is also available at http://mozilla.org/MPL/2.0/.
 */

package com.yandex.yoctodb.v1.mutable.segment;

import static com.yandex.yoctodb.util.UnsignedByteArrays.from;

import com.yandex.yoctodb.util.UnsignedByteArray;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

/**
 * Unit tests for {@link V1StoredIndex}
 *
 * @author incubos
 */
public class V1StoredIndexTest {
  @Test
  public void addDocument() {
    new V1StoredIndex("field").addDocument(0, Collections.singletonList(from(0)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void wrongDocument() {
    new V1StoredIndex("field").addDocument(-1, Collections.singletonList(from(0)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void emptyValues() {
    new V1StoredIndex("field").addDocument(0, Collections.<UnsignedByteArray>emptyList());
  }

  @Test(expected = IllegalArgumentException.class)
  public void multipleValues() {
    new V1StoredIndex("field").addDocument(0, Arrays.asList(from(0), from(1)));
  }
}
