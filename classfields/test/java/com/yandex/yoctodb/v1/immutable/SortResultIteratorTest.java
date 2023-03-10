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

import com.yandex.yoctodb.query.QueryBuilder;
import java.util.Collections;
import org.junit.Test;

/**
 * Unit tests for {@link SortResultIterator}
 *
 * @author incubos
 */
public class SortResultIteratorTest {
  @Test(expected = UnsupportedOperationException.class)
  public void unsupportedRemove() {
    new SortResultIterator(QueryBuilder.select(), Collections.<QueryContext>emptyIterator())
        .remove();
  }
}
