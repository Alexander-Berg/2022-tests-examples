package ru.yandex.vertis.caching.support

import ru.yandex.vertis.caching.base.AsyncCache
import ru.yandex.vertis.caching.base.impl.StringIntLayout
import ru.yandex.vertis.caching.base.impl.inmemory.InMemoryAsyncCache
import ru.yandex.vertis.caching.plain.base.{AsyncPlainCacheAdapter, PlainCache}
import ru.yandex.vertis.caching.plain.support.{AsyncPlainCacheSupportAdapter, PlainCacheSupport}
import ru.yandex.vertis.util.concurrent.Threads

import scala.language.reflectiveCalls

/**
  * @author korvit
  */
class AsyncCacheSupportSpec
  extends PlainCacheSupportSpec {

  override protected def newPlainCache(): PlainCache[String, Int] =
    new AsyncPlainCacheAdapter[String, Int](
      asyncCache = new InMemoryAsyncCache[String, Int](new StringIntLayout),
      ec = Threads.SameThreadEc)

  override protected def newPlainCacheSupport[K, V](plainCache: PlainCache[K, V]): PlainCacheSupport[String, Int] =
    new AsyncPlainCacheSupportAdapter[String, Int](
      asyncCacheSupport = new AsyncCacheSupport[String, Int] {
        override def cache: AsyncCache[String, Int] =
          plainCache.asInstanceOf[AsyncPlainCacheAdapter[String, Int]].asyncCache
      },
      ec = Threads.SameThreadEc)
}
