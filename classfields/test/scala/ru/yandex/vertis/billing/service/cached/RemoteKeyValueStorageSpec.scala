package ru.yandex.vertis.billing.service.cached

import billing.common.testkit.zio.ZIOSpecBase
import common.zio.redis.sentinel.testkit.InMemoryRedisSentinelClient
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.service.cached.impl.{KryoCodec, Redis}
import zio.stm.TMap

import scala.language.implicitConversions

class RemoteKeyValueStorageSpec extends AnyWordSpec with Matchers with ZIOSpecBase {
  private val returns = new AfterWord("returns")
  private val does = new AfterWord("does")
  private val passed = new AfterWord("passed")
  private val provideMethods = new AfterWord("provide method")

  "KeyValueStorage" should provideMethods {
    "put".which(does {
      "ok" when {
        "io error does not occur" in {
          redisClient().put("test", 666).unsafeRun()
        }
      }
      "save different values" in {
        val cache = redisClient()

        cache
          .put("int", 1)
          .andThen(cache.put("string", "string"))
          .andThen(cache.put("float", 1.6f))
          .unsafeRun()

        cache.get[Int]("int").unsafeRun() shouldBe Some(1)
        cache.get[String]("string").unsafeRun() shouldBe Some("string")
        cache.get[Float]("float").unsafeRun() shouldBe Some(1.6f)
      }
    })
    "putAll".which(does {
      "ok" when {
        "io error does not occur" in {
          val cache = redisClient()
          cache
            .putAll(Map("1" -> 1, "2" -> 2))
            .andThen(cache.getAll[Int](Seq("1", "2")))
            .unsafeRun() shouldBe Map("1" -> 1, "2" -> 2)
        }
      }
    })
    "get".which(returns {
      "ZIOCache.Error" when passed {
        "non-existing key" in {
          val cache = redisClient()
          val result = cache.get[Int]("noname").unsafeRun()
          result should not be defined
        }
      }
      "associated value" when passed {
        "existing key" in {
          val cache = redisClient()
          cache
            .put("test", 666)
            .andThen(cache.get[Int]("test"))
            .unsafeRun()
            .get shouldBe 666
        }
      }
      "CorruptedDataException" when {
        "data in storage does not satisfy our requirements" in {
          val cache = redisClient()
          cache
            .put("test", 666)
            .andThen(cache.put("test", "string"))
            .andThen(cache.get[Int]("test"))
            .unsafeRunToTry() should not be succeed
        }
      }
    })
    "getAll".which(returns {
      "an empty map" when passed {
        "empty seq" in {
          redisClient()
            .getAll[Int](Seq())
            .unsafeRun() shouldBe Map()
        }
        "non-existing keys" in {
          redisClient()
            .getAll[Int](Seq("noname1", "noname2"))
            .unsafeRun() shouldBe Map()
        }
      }
      "map: map.length = args.length > 0" when passed {
        "all existing keys" in {
          val cache = redisClient()
          cache
            .putAll(Map("1" -> 1, "2" -> 2, "3" -> 3))
            .unsafeRun()
          cache
            .getAll[Int](Seq("1", "2", "3"))
            .unsafeRun() shouldBe Map("1" -> 1, "2" -> 2, "3" -> 3)
        }
      }
      "map: 0 < map.length != args.length" when passed {
        "some existing keys, but some are not" in {
          val cache = redisClient()
          cache
            .putAll(Map("1" -> 1, "2" -> 2))
            .unsafeRun()
          cache
            .getAll[Int](Seq("1", "2", "3", "4"))
            .unsafeRun() shouldBe Map("1" -> 1, "2" -> 2)
        }
      }
    })
    "remove".which(does {
      "ok" in {
        val cache = redisClient()
        cache
          .putAll(Map("1" -> 1, "2" -> 2))
          .andThen(cache.remove("1"))
          .unsafeRun()
        cache
          .getAll[Int](Seq("1", "2"))
          .unsafeRun() shouldBe Map("2" -> 2)
      }
      "nothing" when passed {
        "non-existing keys" in {
          val cache = redisClient()
          cache.put("1", 1).andThen(cache.remove("666")).unsafeRun()
          cache.getAll[Int](Seq("1")).unsafeRun() shouldBe Map("1" -> 1)
        }
      }
    })
    "removeAll".which(does {
      "deleting multiple keys" in {
        val cache = redisClient()
        cache
          .putAll(Map("1" -> 1, "2" -> 2))
          .andThen(cache.removeAll(Seq("1", "2")))
          .unsafeRun()
        cache.getAll[Int](Seq("1", "2")).unsafeRun() shouldBe Map()
      }
      "the same as delete(key) when " when passed {
        "only arg" in {
          val first = redisClient()
          first.putAll(Map("1" -> 1, "2" -> 2)).unsafeRun()
          val second = redisClient()
          second.putAll(Map("1" -> 1, "2" -> 2)).unsafeRun()
          first.removeAll(Seq("2")).andThen(second.remove("2")).unsafeRun()
          first
            .getAll[Int](Seq("1", "2"))
            .unsafeRun()
            .shouldBe(second.getAll[Int](Seq("1", "2")).unsafeRun())
        }
      }
    })
  }

  private def redisClient() = zioRedisClient().unsafeRun()

  private def zioRedisClient() = for {
    map <- TMap.empty[String, Array[Byte]].commit
  } yield new Redis[String](
    new InMemoryRedisSentinelClient(map),
    KryoCodec
  )
}
