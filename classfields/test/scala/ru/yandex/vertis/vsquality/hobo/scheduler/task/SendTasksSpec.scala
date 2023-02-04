package ru.yandex.vertis.vsquality.hobo.scheduler.task

import ru.yandex.vertis.vsquality.hobo.util.ExponentialBackoff.backoff
import ru.yandex.vertis.vsquality.hobo.util.{DateTimeUtil, SpecBase}

import scala.concurrent.duration._

/**
  * @author akhazhoyan 02/2019
  */

class SendTasksSpec extends SpecBase {

  private val waitFrom = DateTimeUtil.fromMillis(0)
  private val baseWaitDuration = 90.seconds
  private val maxWaitDuration = 10.days

  "backoff" should {
    "wait exactly baseWaitDuration for the first try" in {
      val firstWait = backoff(0, waitFrom, baseWaitDuration, maxWaitDuration)

      val wholeWaitMillis = firstWait.getMillis - waitFrom.getMillis
      wholeWaitMillis should equal(baseWaitDuration.toMillis)
    }

    "handle negative retry number" in {
      val wait = backoff(-10, waitFrom, baseWaitDuration, maxWaitDuration)

      val wholeWaitMillis = wait.getMillis - waitFrom.getMillis
      wholeWaitMillis should equal(baseWaitDuration.toMillis)
    }

    "sum to about one-two days if invoked sequentially several times" in {
      val lastWaitTo =
        Range(0, 10)
          .foldLeft(waitFrom) { (w, i) =>
            backoff(i, w, baseWaitDuration, maxWaitDuration)
          }

      val wholeWaitMillis = lastWaitTo.getMillis - waitFrom.getMillis
      wholeWaitMillis should be >= 1.days.toMillis
      wholeWaitMillis should be <= 2.days.toMillis
    }
  }
}
