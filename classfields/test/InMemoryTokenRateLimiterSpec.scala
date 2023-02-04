package vertis.zio.ratelimiter

import zio.duration._
import zio.{clock, ZIO}
import zio.test._
import zio.test.environment._
import zio.test.Assertion._

object InMemoryTokenRateLimiterSpec extends DefaultRunnableSpec {

  // scalastyle:off method.length cyclomatic.complexity
  override def spec: ZSpec[TestEnvironment, Assertion.type] = suite("InMemoryTokenRateLimiter")(
    testM("`check` should not modify current balance") {
      for {
        startTime <- clock.nanoTime
        startDuration = Duration.Finite(startTime)
        _ <- TestClock.setTime(startDuration)
        limiter <- InMemoryTokenRateLimiter.create(RateLimiterConfig(1.minute.asScala, 30))
        res1 <- limiter.check(20)
        res2 <- limiter.check(20)
      } yield {
        assert(res1)(equalTo(RateLimiter.Allow(10))) &&
        assert(res2)(equalTo(RateLimiter.Allow(10)))
      }
    },
    testM("`check` should die on expensive (greater than balance) calls") {
      for {
        startTime <- clock.nanoTime
        startDuration = Duration.Finite(startTime)
        _ <- TestClock.setTime(startDuration)
        limiter <- InMemoryTokenRateLimiter.create(RateLimiterConfig(1.minute.asScala, 30))
        res1 <- limiter.check(0)
        res2 <- limiter.check(40).run
        _ <- TestClock.adjust(5.minutes)
        res3 <- limiter.check(40).run
      } yield {
        assert(res1)(equalTo(RateLimiter.Allow(30))) &&
        assert(res2)(dies(isSubtype[IllegalArgumentException](anything))) &&
        assert(res3)(dies(isSubtype[IllegalArgumentException](anything)))
      }
    },
    testM("`tryAcquire` should die on forbid expensive (greater than balance) calls") {
      for {
        startTime <- clock.nanoTime
        startDuration = Duration.Finite(startTime)
        _ <- TestClock.setTime(startDuration)
        limiter <- InMemoryTokenRateLimiter.create(RateLimiterConfig(1.minute.asScala, 30))
        res1 <- limiter.check(0)
        res2 <- limiter.tryAcquire(40).run
        _ <- TestClock.adjust(5.minutes)
        res3 <- limiter.tryAcquire(40).run
      } yield {
        assert(res1)(equalTo(RateLimiter.Allow(30))) &&
        assert(res2)(dies(isSubtype[IllegalArgumentException](anything))) &&
        assert(res3)(dies(isSubtype[IllegalArgumentException](anything)))
      }
    },
    testM("`check` should schedule expensive (less than balance) calls") {
      for {
        startTime <- clock.nanoTime
        startDuration = Duration.Finite(startTime)
        _ <- TestClock.setTime(startDuration)
        limiter <- InMemoryTokenRateLimiter.create(RateLimiterConfig(1.minute.asScala, 30))
        res1 <- limiter.check(0)
        res2 <- limiter.tryAcquire(20)
        res3 <- limiter.check(20)
        _ <- TestClock.adjust(20.seconds)
        res4 <- limiter.check(20)
      } yield {
        assert(res1)(equalTo(RateLimiter.Allow(30))) &&
        assert(res2)(equalTo(RateLimiter.Allow(10))) &&
        assert(res3)(equalTo(RateLimiter.Later(20.seconds))) &&
        assert(res4)(equalTo(RateLimiter.Allow(0)))
      }
    },
    testM("`tryAcquire` should schedule expensive (less than balance) calls") {
      for {
        startTime <- clock.nanoTime
        startDuration = Duration.Finite(startTime)
        _ <- TestClock.setTime(startDuration)
        limiter <- InMemoryTokenRateLimiter.create(RateLimiterConfig(1.minute.asScala, 30))
        res1 <- limiter.check(0)
        res2 <- limiter.tryAcquire(20)
        res3 <- limiter.tryAcquire(20)
        _ <- TestClock.adjust(20.seconds)
        res4 <- limiter.tryAcquire(20)
      } yield {
        assert(res1)(equalTo(RateLimiter.Allow(30))) &&
        assert(res2)(equalTo(RateLimiter.Allow(10))) &&
        assert(res3)(equalTo(RateLimiter.Later(20.seconds))) &&
        assert(res4)(equalTo(RateLimiter.Allow(0)))
      }
    },
    testM("should restore current balance with initial balance limit") {
      for {
        startTime <- clock.nanoTime
        startDuration = Duration.Finite(startTime)
        _ <- TestClock.setTime(startDuration)
        limiter <- InMemoryTokenRateLimiter.create(RateLimiterConfig(1.minute.asScala, 60))
        res1 <- limiter.tryAcquire(60)
        _ <- TestClock.adjust(20.seconds)
        res2 <- limiter.check(0)
        _ <- TestClock.adjust(20.seconds)
        res3 <- limiter.check(0)
        _ <- TestClock.adjust(20.seconds)
        res4 <- limiter.check(0)
        _ <- TestClock.adjust(20.seconds)
        res5 <- limiter.check(0)
      } yield {
        assert(res1)(equalTo(RateLimiter.Allow(0))) &&
        assert(res2)(equalTo(RateLimiter.Allow(20))) &&
        assert(res3)(equalTo(RateLimiter.Allow(40))) &&
        assert(res4)(equalTo(RateLimiter.Allow(60))) &&
        assert(res5)(equalTo(RateLimiter.Allow(60)))
      }
    },
    testM("should throttle actions (unordered)") {
      for {
        startTime <- clock.nanoTime
        startDuration = Duration.Finite(startTime)
        _ <- TestClock.setTime(startDuration)
        limiter <- InMemoryTokenRateLimiter.create(RateLimiterConfig(1.minute.asScala, 60))
        res0 <- limiter.throttled(60)(clock.nanoTime).orDie
        res1Fiber <- limiter.throttled(60)(clock.nanoTime).orDie.fork
        res2Fiber <- limiter.throttled(60)(clock.nanoTime).orDie.fork
        _ <- TestClock.adjust(60.seconds)
        expectedActionTime1 <- clock.nanoTime
        _ <- TestClock.adjust(60.seconds)
        expectedActionTime2 <- clock.nanoTime
        res1 <- res1Fiber.join
        res2 <- res2Fiber.join
      } yield {
        assert(res0)(equalTo(startTime)) &&
        assert(res1.min(res2))(equalTo(expectedActionTime1)) &&
        assert(res1.max(res2))(equalTo(expectedActionTime2))
      }
    },
    testM("`throttle` should fails with Forbidden immediately if cost greater then balance") {
      for {
        startTime <- clock.nanoTime
        startDuration = Duration.Finite(startTime)
        _ <- TestClock.setTime(startDuration)
        limiter <- InMemoryTokenRateLimiter.create(RateLimiterConfig(1.minute.asScala, 60))
        res <- limiter.throttled(100)(ZIO.unit).run
      } yield assert(res)(dies(isSubtype[IllegalArgumentException](anything)))
    }
  )
  // scalastyle:on

}
