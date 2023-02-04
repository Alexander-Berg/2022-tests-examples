package ru.yandex.vertis.caching.base.impl

import ru.yandex.vertis.caching.base.impl.inmemory.InMemorySyncCache
import ru.yandex.vertis.caching.base.{SyncCache, SyncCacheSpec}

/**
  * @author korvit
  */
class InMemorySyncCacheSpec
  extends SyncCacheSpec {

  override protected val syncCache: SyncCache[String, Int] =
    new InMemorySyncCache[String, Int](new StringIntLayout)
}
