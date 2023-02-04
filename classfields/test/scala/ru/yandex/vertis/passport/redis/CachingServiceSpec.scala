package ru.yandex.vertis.passport.redis

import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.{Inspectors, Matchers, WordSpec}
import org.scalatest.prop.PropertyChecks
import ru.yandex.vertis.passport.redis.RedisCodecs._
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class CachingServiceSpec extends WordSpec with Matchers with Inspectors with MockitoSupport with PropertyChecks {

  trait mocks {

    implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

    val redisCache = mock[RedisCache]
    val service = new CachingService(redisCache)
    val testKey = "test-key"
    val testCachedValue = 10
    val testActualValue = 5

    implicit val trace: Traced = Traced.empty

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
      when(redisCache.get[Int](?)(?, ?)).thenReturn(Future.successful(Some(testCachedValue)))
      val result = service.cached(testKey, testFutureValue(false), 10.seconds).futureValue
      result shouldBe testCachedValue
    }

    "put value to cache" in new mocks {
      when(redisCache.get[Int](?)(?, ?)).thenReturn(Future.successful(None))
      when(redisCache.set(?, ?, ?)(?, ?)).thenReturn(Future.unit)
      val result = service.cached(testKey, testFutureValue(true), 10.seconds).futureValue
      result shouldBe testActualValue
    }

    "get value from service if redis get fails" in new mocks {
      when(redisCache.get[Int](?)(?, ?)).thenReturn(Future.failed(new RuntimeException))
      when(redisCache.set(?, ?, ?)(?, ?)).thenReturn(Future.unit)
      val result = service.cached(testKey, testFutureValue(true), 10.seconds).futureValue
      result shouldBe testActualValue
    }

    "get value from service if redis fails" in new mocks {
      when(redisCache.get[Int](?)(?, ?)).thenReturn(Future.successful(None))
      when(redisCache.set(?, ?, ?)(?, ?)).thenReturn(Future.failed(new RuntimeException))
      val result = service.cached(testKey, testFutureValue(true), 10.seconds).futureValue
      result shouldBe testActualValue
    }

  }

}
