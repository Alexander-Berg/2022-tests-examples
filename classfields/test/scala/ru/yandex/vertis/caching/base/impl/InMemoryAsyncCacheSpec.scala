package ru.yandex.vertis.caching.base.impl

import ru.yandex.vertis.caching.base.impl.inmemory.InMemoryAsyncCache
import ru.yandex.vertis.caching.base.{AsyncCache, AsyncCacheSpec}

/**
  * @author korvit
  */
class InMemoryAsyncCacheSpec
  extends AsyncCacheSpec {

  override protected val asyncCache: AsyncCache[String, Int] =
    new InMemoryAsyncCache[String, Int] (new StringIntLayout)
}
