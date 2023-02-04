package ru.yandex.vertis.caching.redis

import akka.actor.ActorSystem
import redis.RedisClient
import ru.yandex.vertis.caching.base.impl.StringIntLayout
import ru.yandex.vertis.caching.base.layout.keyspace.KeyspacedLayout
import ru.yandex.vertis.caching.base.{AsyncCache, AsyncCacheSpec}

import scala.util.Random

/**
  * @author korvit
  */
class RedisAsyncCacheIntSpec
  extends AsyncCacheSpec
    with RedisCacheSpecBase {

  private implicit val actorSystem = ActorSystem()
  protected val client = RedisClient(port = container.getMappedPort(6379))

  override protected val asyncCache: AsyncCache[String, Int] =
    new RedisAsyncCache[String, Int](
      client = client,
      layout = new StringIntLayout with KeyspacedLayout[String, Int] {
        override val keyspace: String = s"common.caching.redis.async.${Random.nextInt()}"
      })

  override def afterAll(): Unit = actorSystem.terminate()
}
