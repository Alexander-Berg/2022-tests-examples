package common.cache.memory.test

import common.cache.memory.MemoryCache
import zio.ZIO
import zio.test._
import zio.test.Assertion._

object MemoryCacheSpec extends DefaultRunnableSpec {

  def spec =
    suite("MemoryCache")(
      testM("Чтение и запись") {
        for {
          cache <- MemoryCache.make[Int, String](MemoryCache.Config(1024, None))
          res1 <- cache.get(1)
          _ <- cache.put(1, "1")
          res2 <- cache.get(1)
        } yield assert(res1)(isNone) && assert(res2)(isSome(equalTo("1")))
      },
      testM("Удаление") {
        for {
          cache <- MemoryCache.make[Int, String](MemoryCache.Config(1024, None))
          _ <- cache.put(2, "2")
          res1 <- cache.get(2)
          _ <- cache.remove(2)
          res2 <- cache.get(2)
        } yield assert(res2)(isNone) && assert(res1)(isSome(equalTo("2")))
      }
    )
}
