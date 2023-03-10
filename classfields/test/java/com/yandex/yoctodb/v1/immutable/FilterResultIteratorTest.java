/*
 * (C) YANDEX LLC, 2014-2016
 *
 * The Source Code called "YoctoDB" available at
 * https://github.com/yandex/yoctodb is subject to the terms of the
 * Mozilla Public License, v. 2.0 (hereinafter referred to as the "License").
 *
 * A copy of the License is also available at http://mozilla.org/MPL/2.0/.
 */

package com.yandex.yoctodb.v1.immutable;

import com.yandex.yoctodb.immutable.IndexedDatabase;
import com.yandex.yoctodb.query.QueryBuilder;
import com.yandex.yoctodb.util.mutable.impl.ThreadLocalCachedArrayBitSetPool;
import java.util.Collections;
import org.junit.Test;

/**
 * Unit tests for {@link FilterResultIterator}
 *
 * @author incubos
 */
public class FilterResultIteratorTest {
  @Test(expected = UnsupportedOperationException.class)
  public void unsupportedRemove() {
    new FilterResultIterator(
            QueryBuilder.select(),
            Collections.<IndexedDatabase>emptyIterator(),
            new ThreadLocalCachedArrayBitSetPool())
        .remove();
  }
}
