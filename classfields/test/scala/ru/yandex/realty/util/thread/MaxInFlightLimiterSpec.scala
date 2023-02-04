package ru.yandex.realty.util.thread

import java.util.concurrent.CountDownLatch

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.AsyncSpecBase

import scala.concurrent.Future

/**
  * Runnable specs on [[MaxInFlightLimiter]].
  *
  * @author dimas
  */
@RunWith(classOf[JUnitRunner])
class MaxInFlightLimiterSpec extends AsyncSpecBase {

  "MaxInFlightLimiter" should {
    "limit async actions" in {
      val limiter = new MaxInFlightLimiter(1)

      val latch = new CountDownLatch(1)

      val latchFuture = limiter.async {
        Future {
          latch.await()
        }
      }

      interceptCause[InFlightLimitExceeded] {
        limiter.async {
          Future {
            1 + 2
          }
        }.futureValue
      }

      latch.countDown()

      latchFuture.futureValue shouldBe ()

      limiter.async {
        Future {
          1 + 2
        }
      }.futureValue shouldBe 3
    }
  }

}
