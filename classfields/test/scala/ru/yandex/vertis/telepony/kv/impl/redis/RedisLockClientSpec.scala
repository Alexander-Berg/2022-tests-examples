package ru.yandex.vertis.telepony.kv.impl.redis

import org.redisson.api.{RLock, RedissonClient}
import org.redisson.misc.CompletableFutureWrapper
import org.scalamock.scalatest.MockFactory
import ru.yandex.vertis.telepony.SpecBase
import ru.yandex.vertis.telepony.kv.impl.redis.RedisLockClient.{LockAcquiringTimeoutException, LockAwaitingTimeoutException}
import ru.yandex.vertis.telepony.service.impl.SourceLastCallServiceImpl.DefaultRuntime
import ru.yandex.vertis.zio.util.ZioRunUtils
import zio.ZIO
import zio.clock.Clock
import zio.duration.durationInt

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

class RedisLockClientSpec extends SpecBase with MockFactory {

  val redissonClientMock = mock[RedissonClient]
  val rLockMock = mock[RLock]

  val redisClient = new RedisLockClient(redissonClientMock)

  "RedisLockClient" should {
    "acquire then release lock" in {
      val key = "key"
      val releaseTimeMs = 100L
      val waitTimeMs = 0L
      (redissonClientMock.getLock _)
        .expects(key)
        .returning(rLockMock)

      (rLockMock
        .tryLockAsync(_: Long, _: Long, _: TimeUnit))
        .expects(waitTimeMs, releaseTimeMs, TimeUnit.MILLISECONDS)
        .returning(new CompletableFutureWrapper(true))

      (rLockMock.forceUnlockAsync _)
        .expects()
        .returning(new CompletableFutureWrapper(true))

      val bool = new AtomicBoolean(false)

      val res = redisClient.doWithLock(key, 0, 100) {
        ZIO.effect(bool.compareAndSet(false, true))
      }

      ZioRunUtils.runAsync(DefaultRuntime)(res).futureValue

      bool.get() shouldBe true
    }

    "fail if cant acquire lock in time" in {
      val key = "key"

      val counter = new AtomicInteger(0)

      (redissonClientMock.getLock _)
        .expects(key)
        .returning(rLockMock)

      (rLockMock
        .tryLockAsync(_: Long, _: Long, _: TimeUnit))
        .expects(0L, 100000, TimeUnit.MILLISECONDS)
        .returning(new CompletableFutureWrapper(true))

      val endlessLock = redisClient.doWithLock(key, 0, 100000) {
        ZIO.effect(counter.incrementAndGet()) *>
          ZIO.sleep(100.seconds).provideLayer(Clock.live)
      }
      ZioRunUtils.runAsync(DefaultRuntime)(endlessLock)

      counter.get() shouldBe 1

      (redissonClientMock.getLock _)
        .expects(key)
        .returning(rLockMock)

      (rLockMock
        .tryLockAsync(_: Long, _: Long, _: TimeUnit))
        .expects(500, 500, TimeUnit.MILLISECONDS)
        .returning(new CompletableFutureWrapper(false))

      (rLockMock.forceUnlockAsync _)
        .expects()
        .never()

      val res1 = redisClient.doWithLock(key, 500, 500) {
        ZIO.effect(counter.incrementAndGet())
      }
      ZioRunUtils.runAsync(DefaultRuntime)(res1).failed.futureValue shouldBe an[LockAcquiringTimeoutException]
      counter.get() shouldBe 1
    }

    "return success if lock is free" in {
      val key = "key"
      (redissonClientMock.getLock _)
        .expects(key)
        .returning(rLockMock)

      (rLockMock.isLockedAsync _)
        .expects()
        .returning(new CompletableFutureWrapper(false))

      val res = redisClient.awaitForLockRelease(key, waitTimeMs = 1000)

      ZioRunUtils.runAsync(DefaultRuntime)(res).futureValue

    }

    "return success if lock is not free but becomes free" in {
      val key = "key"
      (redissonClientMock.getLock _)
        .expects(key)
        .returning(rLockMock)

      (rLockMock.isLockedAsync _)
        .expects()
        .returning(new CompletableFutureWrapper(true))

      (rLockMock.isLockedAsync _)
        .expects()
        .returning(new CompletableFutureWrapper(true))

      (rLockMock.isLockedAsync _)
        .expects()
        .returning(new CompletableFutureWrapper(false))

      val res = redisClient.awaitForLockRelease(key, waitTimeMs = 1000).timed

      val (duration, _) = ZioRunUtils.runAsync(DefaultRuntime)(res).futureValue
      duration.toMillis > 200 shouldBe true

    }

    "fail on timeout if lock wont be released it time" in {
      val key = "key"
      (redissonClientMock.getLock _)
        .expects(key)
        .returning(rLockMock)

      (rLockMock.isLockedAsync _)
        .expects()
        .anyNumberOfTimes()
        .returning(new CompletableFutureWrapper(true))

      val res = redisClient.awaitForLockRelease(key, waitTimeMs = 600)

      ZioRunUtils.runAsync(DefaultRuntime)(res).failed.futureValue shouldBe a[LockAwaitingTimeoutException]

    }

  }

}
