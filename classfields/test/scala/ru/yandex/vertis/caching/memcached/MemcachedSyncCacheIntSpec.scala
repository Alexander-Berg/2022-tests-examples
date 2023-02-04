package ru.yandex.vertis.caching.memcached

import ru.yandex.vertis.caching.base.impl.StringIntLayout
import ru.yandex.vertis.caching.base.layout.keyspace.KeyspacedLayout
import ru.yandex.vertis.caching.base.{SyncCache, SyncCacheSpec}

import scala.util.Random

/**
  * @author korvit
  */
class MemcachedSyncCacheIntSpec
  extends SyncCacheSpec
    with MemcachedCacheSpecBase {

  override protected val syncCache: SyncCache[String, Int] =
    new MemcachedSyncCache[String, Int](
      client = client,
      layout = new StringIntLayout with KeyspacedLayout[String, Int] {
        override val keyspace: String = s"common.caching.memcached.sync.${Random.nextInt()}"
      })
}
