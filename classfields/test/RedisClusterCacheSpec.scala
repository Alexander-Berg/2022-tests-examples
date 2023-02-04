package ru.yandex.vertis.general.common.cache.test

import ru.yandex.vertis.general.common.cache.Cache
import ru.yandex.vertis.general.common.cache.testkit.RedisTestCache
import zio.ZIO
import zio.test.Assertion._
import zio.test._

import scala.concurrent.duration._

object RedisClusterCacheSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    suite("RedisClusterCache")(
      testM("set and get value") {
        val key = Array(0, 2, 4, 6, 8).map(_.toByte)
        val value = Array(1, 3, 5, 7, 9).map(_.toByte)
        for {
          _ <- Cache.set(key, value, 10.minutes)
          result <- Cache.get[Array[Byte], Array[Byte]](key)
        } yield assert(result)(isSome(equalTo(value)))
      },
      testM("get value for unknown key") {
        val key = Array(10).map(_.toByte)
        for {
          result <- Cache.get[Array[Byte], Array[Byte]](key)
        } yield assert(result)(isNone)
      },
      testM("set value with ttl") {
        val key = "third-test-key"
        val value = "third-test-value"
        for {
          _ <- Cache.set(key, value, 4.second)
          firstResult <- Cache.get[String, String](key)
          _ <- zio.clock.sleep(java.time.Duration.ofSeconds(8))
          secondResult <- Cache.get[String, String](key)
        } yield assert(firstResult)(isSome(equalTo(value))) && assert(secondResult)(isNone)
      },
      testM("set value with namespace") {
        val key = "namespace-test-key"
        val value1 = "value1"
        val value2 = "value2"
        for {
          cacheService <- ZIO.service[Cache.Service]
          _ <- cacheService.withNamespace("a").set(key, value1, 5.hours)
          _ <- cacheService.withNamespace("b").set(key, value2, 5.hours)
          result <- cacheService.withNamespace("a").get[String, String](key)
        } yield assert(result)(isSome(equalTo(value1)))
      }
    ).provideCustomLayerShared {
      RedisTestCache.managedRedisCache ++ zio.clock.Clock.live
    }
  }
}
