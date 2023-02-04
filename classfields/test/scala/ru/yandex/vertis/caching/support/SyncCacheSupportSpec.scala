package ru.yandex.vertis.caching.support

import ru.yandex.vertis.caching.base.SyncCache
import ru.yandex.vertis.caching.base.layout.Layout
import ru.yandex.vertis.caching.base.impl.StringIntLayout
import ru.yandex.vertis.caching.base.impl.inmemory.InMemorySyncCache
import ru.yandex.vertis.caching.plain.base.{PlainCache, SyncPlainCacheAdapter}
import ru.yandex.vertis.caching.plain.support.{PlainCacheSupport, SyncPlainCacheSupportAdapter}

/**
  * @author korvit
  */
class SyncCacheSupportSpec
  extends PlainCacheSupportSpec {

  override protected def newPlainCache(): PlainCache[String, Int] =
    new SyncPlainCacheAdapter[String, Int](
      new InMemorySyncCache[String, Int](new StringIntLayout))

  override protected def newPlainCacheSupport[K, V](plainCache: PlainCache[K, V]): PlainCacheSupport[String, Int] =
    new SyncPlainCacheSupportAdapter[String, Int](
      new SyncCacheSupport[String, Int] {
        override def cache: SyncCache[String, Int] =
          plainCache.asInstanceOf[SyncPlainCacheAdapter[String, Int]].syncCache
      })
}
