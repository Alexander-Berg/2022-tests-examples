package ru.auto.api.app.redis

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.app.redis.RedisCodecs._
import ru.auto.api.auth.Application
import ru.auto.api.model.RequestParams
import ru.auto.api.util.RequestImpl
import ru.auto.api.{AsyncTasksSupport, BaseSpec}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.Future
import scala.concurrent.duration._

class CachingServiceSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks with AsyncTasksSupport {

  trait mocks {
    val redisCache = mock[RedisCache]
    val service = new CachingService(redisCache)
    val testKey = "test-key"
    val testCachedValue = 10
    val testActualValue = 5

    implicit val trace: Traced = Traced.empty

    implicit val request: RequestImpl = {
      val req = new RequestImpl
      req.setApplication(Application.desktop)
      req.setTrace(trace)
      req.setRequestParams(RequestParams.empty)
      req
    }

    def testFutureValue(shouldFetch: Boolean): Future[Int] = {
      if (shouldFetch) {
        Future.successful(testActualValue)
      } else {
        fail()
      }
    }
  }

  "CachingService" should {

    "get value from cache" in new mocks {
      when(redisCache.get[Int](?)(?, ?)).thenReturnF(Some(testCachedValue))
      val result = service.cached(testKey, testFutureValue(false), 10.seconds).await
      result shouldBe testCachedValue
    }

    "put value to cache" in new mocks {
      when(redisCache.get[Int](?)(?, ?)).thenReturnF(None)
      when(redisCache.set(?, ?, ?)(?, ?)).thenReturnF(())
      val result = service.cached(testKey, testFutureValue(true), 10.seconds).await
      result shouldBe testActualValue
    }

    "get value from service if redis get fails" in new mocks {
      when(redisCache.get[Int](?)(?, ?)).thenReturn(Future.failed(new RuntimeException))
      when(redisCache.set(?, ?, ?)(?, ?)).thenReturnF(())
      val result = service.cached(testKey, testFutureValue(true), 10.seconds).await
      result shouldBe testActualValue
    }

    "get value from service if redis fails" in new mocks {
      when(redisCache.get[Int](?)(?, ?)).thenReturnF(None)
      when(redisCache.set(?, ?, ?)(?, ?)).thenReturn(Future.failed(new RuntimeException))
      val result = service.cached(testKey, testFutureValue(true), 10.seconds).await
      result shouldBe testActualValue
    }

  }

}
