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

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.TreeMultimap;
import com.yandex.yoctodb.util.mutable.IndexToIndexMultiMap;
import com.yandex.yoctodb.v1.V1DatabaseFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.junit.Test;

/**
 * Unit tests for {@link IndexToIndexMultiMapFactory}
 *
 * @author incubos
 */
public class IndexToIndexMultiMapFactoryTest {
  @Test(expected = IllegalArgumentException.class)
  public void zeroValues() {
    IndexToIndexMultiMapFactory.buildIndexToIndexMultiMap(
        Collections.<Collection<Integer>>emptyList(), 1, V1DatabaseFormat.Feature.valuesSet());
  }

  @Test(expected = IllegalArgumentException.class)
  public void zeroDocuments() {
    IndexToIndexMultiMapFactory.buildIndexToIndexMultiMap(
        singletonList(singletonList(0)), 0, V1DatabaseFormat.Feature.valuesSet());
  }

  @Test
  public void list() {
    final TreeMultimap<Integer, Integer> elements = TreeMultimap.create();
    for (int i = 0; i < 1024; i++) {
      elements.put(i, i);
      elements.put(i, i + 1);
    }
    final IndexToIndexMultiMap map =
        IndexToIndexMultiMapFactory.buildIndexToIndexMultiMap(
            elements.asMap().values(), 1024, V1DatabaseFormat.Feature.valuesSet());
    assertTrue(map instanceof IntIndexToIndexMultiMap);
  }

  @Test
  public void bitset() {
    @SuppressWarnings("unchecked")
    final IndexToIndexMultiMap map =
        IndexToIndexMultiMapFactory.buildIndexToIndexMultiMap(
            Arrays.asList(singletonList(0), singletonList(1), singletonList(1)),
            128,
            V1DatabaseFormat.Feature.valuesSet());
    assertTrue(map instanceof BitSetIndexToIndexMultiMap);
  }

  @Test
  public void ascending() {
    @SuppressWarnings("unchecked")
    final IndexToIndexMultiMap map =
        IndexToIndexMultiMapFactory.buildIndexToIndexMultiMap(
            Arrays.asList(singletonList(0), singletonList(1), singletonList(2)),
            128,
            V1DatabaseFormat.Feature.valuesSet());
    assertTrue(map instanceof AscendingBitSetIndexToIndexMultiMap);
  }

  @Test
  public void notAscendingWithoutFeature() {
    @SuppressWarnings("unchecked")
    final IndexToIndexMultiMap map =
        IndexToIndexMultiMapFactory.buildIndexToIndexMultiMap(
            Arrays.asList(singletonList(0), singletonList(1), singletonList(2)),
            128,
            singleton(V1DatabaseFormat.Feature.LEGACY));
    assertFalse(map instanceof AscendingBitSetIndexToIndexMultiMap);
  }

  @Test
  public void nullTypedShouldBeDefault() {
    @SuppressWarnings("unchecked")
    final IndexToIndexMultiMap map =
        IndexToIndexMultiMapFactory.buildIndexToIndexMultiMap(
            Arrays.asList(singletonList(0), singletonList(1), singletonList(2)),
            128,
            V1DatabaseFormat.Feature.valuesSet());
    assertTrue(map instanceof AscendingBitSetIndexToIndexMultiMap);
  }
}
