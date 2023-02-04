package vertis.zio.cache

import common.zio.logging.Logging
import org.scalatest.{Assertion, ParallelTestExecution}
import vertis.zio.cache.ExpirationTypes._
import vertis.zio.cache.ZioLoadingCacheSpec.{Test, TestSource}
import vertis.zio.test.ZioSpecBase
import vertis.zio.test.ZioSpecBase.TestBody
import zio.clock.Clock
import zio.{RIO, Schedule, UIO, URIO, ZIO}

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration.{DurationInt, FiniteDuration}

/** @author Ratskevich Natalia reimai@yandex-team.ru
  */
//noinspection ScalaStyle
class ZioLoadingCacheSpec extends ZioSpecBase with ParallelTestExecution {

  // should to be > than the test timeout
  private val expirationTimeout = Some(2.minutes)

  "LoadingCacheSource" should {

    "get values from source" in cacheTest { test =>
      import test._
      checkM("got value from source")(assertValue(1, "1"))
    }

    "get values from cache" in cacheTest { test =>
      import test._
      for {
        _ <- checkM("got value")(assertValue(5, "5"))
        _ <- checkM("value was from source")(assertAccess(1))
        _ <- checkM("got same result")(assertValue(5, "5"))
        _ <- checkM("value was from cache")(assertAccess(1))
      } yield ()
    }

    "expire values after write" in
      cacheCustomTest(LoadingCacheConf(expirationTimeout, Write), piggybacking = false)(testExpiration)
    "expire values after write with piggybacking" in
      cacheCustomTest(LoadingCacheConf(expirationTimeout, Write))(testExpiration)
    "expire values after access" in
      cacheCustomTest(LoadingCacheConf(expirationTimeout, Access), piggybacking = false)(testExpiration)
    "expire values after access with piggybacking" in
      cacheCustomTest(LoadingCacheConf(expirationTimeout, Access))(testExpiration)

    s"piggyback parallel requests" in cacheTest { test =>
      import test._
      for {
        _ <- requestStorm(2, 500.millis)
        _ <- checkM(s"only one of the requests hit source")(assertAccess(1))
      } yield ()
    }
  }

  private def testExpiration(test: Test[Int, String]) = {
    import test._
    for {
      _ <- checkM("got value")(assertValueAccess(3, "3", 1))
      _ <- checkM("got value from cache")(assertValueAccess(3, "3", 1))
    } yield ()
  }

  private def cacheTest(body: Test[Int, String] => TestBody): Unit =
    cacheCustomTest(LoadingCacheConf())(body)

  private def cacheCustomTest(
      conf: LoadingCacheConf,
      piggybacking: Boolean = true
    )(body: Test[Int, String] => TestBody): Unit =
    ioTest {
      val source = new TestSource(None)
      val cache = ZioLoadingCache[Int, String, Logging.Logging, Throwable](source, conf, piggybacking)
      body(new Test(cache, source))
    }
}

object ZioLoadingCacheSpec {
  import org.scalatest.matchers.should.Matchers._

  class Test[K, V](cache: ZioLoadingCache[K, V, Logging.Logging, Throwable], source: TestSource) {

    def requestStorm(key: K, duration: FiniteDuration): RIO[Logging.Logging with Clock, Int] =
      ZIO.access[Clock](_.get.nanoTime) >>= { start =>
        start.map(_ + duration.toNanos).map { deadline =>
          Schedule.recurWhileM[Clock, Any](_ => ZIO.access[Clock](_.get.nanoTime).flatMap(_.map(_ < deadline)))
        }
      } >>= { untilDeadline =>
        cache.get(key).repeat(Schedule.forever && untilDeadline).map(_._1.toInt + 1)
      }

    def assertAccess(expected: Int): UIO[Assertion] =
      source.countAccess.map(_ shouldBe expected)

    def checkAccess(check: Int => Assertion): UIO[Assertion] =
      source.countAccess.map(check)

    def assertValue(key: K, expected: V): RIO[Logging.Logging, Assertion] =
      cache.get(key).map(_ shouldBe expected)

    def assertValueAccess(key: K, expectedValue: V, expectedAccess: Int): RIO[Logging.Logging, Assertion] =
      assertValue(key, expectedValue) *> assertAccess(expectedAccess)
  }

  class TestSource(limit: Option[Int]) extends LoadingCacheSource[Int, String, Logging.Logging, Throwable] {

    private val accessCounter: AtomicInteger = new AtomicInteger(0)

    private val access: URIO[Logging.Logging, Unit] =
      UIO(accessCounter.incrementAndGet()).unit

    private val permits = limit.map(new java.util.concurrent.Semaphore(_))

    val countAccess: UIO[Int] = UIO(accessCounter.get())

    private def withLimit[T](f: => T): T =
      permits match {
        case None => f
        case Some(semaphore) =>
          if (!semaphore.tryAcquire()) throw new RuntimeException("Too many requests")
          else {
            try {
              f
            } finally {
              semaphore.release()
            }
          }
      }

    override def get(key: Int): ZIO[Logging.Logging, Throwable, String] =
      UIO(withLimit(key.toString)) <* access
  }
}
