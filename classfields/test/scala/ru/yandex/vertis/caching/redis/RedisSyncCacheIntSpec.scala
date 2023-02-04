package ru.yandex.vertis.caching.redis

import redis.clients.jedis.Jedis
import ru.yandex.vertis.caching.base.impl.StringIntLayout
import ru.yandex.vertis.caching.base.layout.keyspace.KeyspacedLayout
import ru.yandex.vertis.caching.base.{SyncCache, SyncCacheSpec}

import scala.util.Random

/**
  * @author korvit
  */
class RedisSyncCacheIntSpec
  extends SyncCacheSpec
    with RedisCacheSpecBase {

  protected val client = new Jedis("localhost", container.getMappedPort(6379))

  override protected val syncCache: SyncCache[String, Int] =
    new RedisSyncCache[String, Int](
      client = client,
      layout = new StringIntLayout with KeyspacedLayout[String, Int] {
        override val keyspace: String = s"common.caching.redis.sync.${Random.nextInt()}"
      })
}
