package ru.yandex.vertis.caching.base

import ru.yandex.vertis.caching.plain.base.{AsyncPlainCacheAdapter, PlainCache}
import ru.yandex.vertis.util.concurrent.Threads._

/**
  * @author korvit
  */
trait AsyncCacheSpec
  extends PlainCacheSpec {

  protected val asyncCache: AsyncCache[String, Int]

  override protected lazy val cache: PlainCache[String, Int] =
    new AsyncPlainCacheAdapter[String, Int](
      asyncCache = asyncCache,
      ec = SameThreadEc)
}
