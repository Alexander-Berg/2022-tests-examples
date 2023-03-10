/*
 * (C) YANDEX LLC, 2014-2016
 *
 * The Source Code called "YoctoDB" available at
 * https://github.com/yandex/yoctodb is subject to the terms of the
 * Mozilla Public License, v. 2.0 (hereinafter referred to as the "License").
 *
 * A copy of the License is also available at http://mozilla.org/MPL/2.0/.
 */

package com.yandex.yoctodb;

import com.yandex.yoctodb.immutable.Database;
import com.yandex.yoctodb.query.DocumentProcessor;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class DoubleDatabaseProcessor implements DocumentProcessor {
  private final @NotNull Database negative;
  @NotNull private final Database positive;
  private final @NotNull List<Integer> docs;

  public DoubleDatabaseProcessor(
      @NotNull final Database negative,
      @NotNull final Database positive,
      @NotNull final List<Integer> docs) {
    this.negative = negative;
    this.positive = positive;
    this.docs = docs;
  }

  @Override
  public boolean process(final int document, @NotNull final Database database) {
    assert database == negative || database == positive;

    if (database == negative) {
      docs.add(-document);
    } else {
      docs.add(document);
    }

    return true;
  }
}
