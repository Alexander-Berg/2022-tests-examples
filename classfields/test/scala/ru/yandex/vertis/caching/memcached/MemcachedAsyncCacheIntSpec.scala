package ru.yandex.vertis.caching.memcached

import ru.yandex.vertis.caching.base.impl.StringIntLayout
import ru.yandex.vertis.caching.base.layout.keyspace.KeyspacedLayout
import ru.yandex.vertis.caching.base.{AsyncCache, AsyncCacheSpec}

import scala.util.Random

/**
  * @author korvit
  */
class MemcachedAsyncCacheIntSpec
  extends AsyncCacheSpec
    with MemcachedCacheSpecBase {

  override protected val asyncCache: AsyncCache[String, Int] =
    new MemcachedAsyncCache[String, Int](
      client = client,
      layout = new StringIntLayout with KeyspacedLayout[String, Int] {
        override val keyspace: String = s"common.caching.memcached.async.${Random.nextInt()}"
      })
}
