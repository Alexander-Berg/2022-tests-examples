package ru.yandex.common.throttling

import java.util.concurrent.CountDownLatch

import akka.actor.ActorSystem
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.Ordering.Long
import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Testing of [[RateLimiter]]
  */
class RateLimiterSpec extends FlatSpec with ScalaFutures with Matchers with BeforeAndAfterAll {

  implicit val as = ActorSystem()
  implicit val pc = PatienceConfig(timeout = scaled(Span(10, Seconds)))

  behavior of "RateLimiter"

  it should "works fine when queue size limit is not exceed" in {
    val acquireCount = 5
    val timePerAcquire = 300.millis
    val tick = System.nanoTime()
    val rl = RateLimiter(1, timePerAcquire)

    for (_ <- 1 to acquireCount + 1) {
      rl.acquireFuture().futureValue
    }

    val elapsed = System.nanoTime() - tick

    elapsed should be > acquireCount * timePerAcquire.toNanos
    rl.close()
  }

  it should "acquire without timeouts when the rate is not fit" in {
    val acquireCount = 10
    val tick = System.nanoTime()
    val rl = RateLimiter(acquireCount)

    for (_ <- 1 until acquireCount - 1) {
      rl.acquireFuture()
    }

    val cdl = new CountDownLatch(1)
    rl.throttle(Future.successful(cdl.countDown()))
    cdl.await()

    val tock = System.nanoTime()
    tock - tick should be < 1.second.toNanos
    rl.close()
  }

  "throttled code" should "be executed" in {
    val rl = RateLimiter(1)
    rl.throttle(Future.successful(1)).futureValue shouldBe 1
    rl.close()
  }

  "acquire futures" should "be completed even when queue size is exceed" in {
    val rl = RateLimiter(1, bufferSize = 2)

    // completed immediately
    rl.acquireFuture()
    rl.acquireFuture()

    // fill buffer
    rl.acquireFuture()
    val lastFillBufferFuture = rl.acquireFuture()

    // this future should be failed
    rl.acquireFuture().failed.futureValue

    // wait for free space in the rate limiter queue
    lastFillBufferFuture.futureValue

    // even after failure it should works fine
    rl.acquireFuture().futureValue
    rl.close()
  }

  override protected def afterAll(): Unit = {
    as.terminate()
  }
}
