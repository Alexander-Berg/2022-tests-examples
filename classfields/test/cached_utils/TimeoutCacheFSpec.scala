package ru.yandex.vertis.vsquality.utils.cached_utils

import ru.yandex.vertis.caching.base.AsyncCache
import ru.yandex.vertis.vsquality.utils.cats_utils.Awaitable._
import ru.yandex.vertis.vsquality.utils.test_utils.SpecBase

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Promise, TimeoutException}
import scala.util.Success

class TimeoutCacheFSpec extends SpecBase {

  val asyncCache = mock[AsyncCache[String, String]]

  val cache: Cache[F, String, String] =
    new TimeoutCacheF[F, String, String](new CacheF(asyncCache))(500.millis)

  val k = "K"
  val v = "V"

  "CacheF with TimeoutCacheF" should {
    "not fail before timeout" in {
      val p = Promise[Unit]()
      when(asyncCache.set(k, v, expire = 10.minutes)).thenReturn(p.future)
      timer.sleep(200.millis).map(_ => p.complete(Success(()))).start.await
      cache.set(k, v, expire = 10.minutes).await
    }

    "fail after timeout" in {
      val p = Promise[Unit]()
      when(asyncCache.set(k, v, expire = 10.minutes)).thenReturn(p.future)
      timer.sleep(700.millis).map(_ => p.complete(Success(()))).start.await
      cache.set(k, v, expire = 10.minutes).shouldFailWith[TimeoutException]
    }
  }

}
