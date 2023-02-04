package ru.yandex.hydra.profile.cache

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.slf4j.LoggerFactory
import ru.yandex.hydra.profile.cache.LocalCacheSpec.TestSuite

import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{ExecutionContext, Future}

/** @author @logab
  */
class LocalCacheSpec extends TestKit(ActorSystem("LocalCacheSpec")) with AnyWordSpecLike with Matchers {
  val log = LoggerFactory.getLogger(classOf[LocalCacheSpec])

  "LocalCache" should {
    "not fire excessive requests" in {
      val key = "User"
      new TestSuite(1000, 1.minute, 400.millis)(system, scala.concurrent.ExecutionContext.global) {
        (1 to 100).foreach(_ => localCache.cached(key))
        probe.expectNoMessage(200.millis)
        probe.receiveN(2, 2.seconds)
        probe.expectNoMessage(500.millis)
      }
    }
  }

}

object LocalCacheSpec {

  class TestSuite(
      cs: Int,
      ce: FiniteDuration,
      throttle: FiniteDuration
    )(implicit arf: ActorSystem,
      ec: ExecutionContext) {
    lazy val probe = TestProbe()

    lazy val localCache = new LocalCache {

      val value = new AtomicInteger(0)

      override def cacheSize: Int = cs

      override def load(key: String): Future[Int] =
        Future {
          Thread.sleep(throttle.toMillis)
          val get: Int = value.incrementAndGet
          probe.ref ! get
          get
        }

      override def cacheExpire: FiniteDuration = ce

      override type V = Int
      override type K = String
    }
  }
}
