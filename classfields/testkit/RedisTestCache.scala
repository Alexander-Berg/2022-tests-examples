package ru.yandex.vertis.general.common.cache.testkit

import common.zio.redis.cluster.testkit.TestRedisClusterClient
import ru.yandex.vertis.general.common.cache.Cache.Cache
import ru.yandex.vertis.general.common.cache.redis.RedisClusterCache
import zio.ZLayer
import zio.blocking.Blocking

object RedisTestCache {

  val managedRedisCache: ZLayer[Blocking, Nothing, Cache] =
    TestRedisClusterClient.test >>> RedisClusterCache.live
}
