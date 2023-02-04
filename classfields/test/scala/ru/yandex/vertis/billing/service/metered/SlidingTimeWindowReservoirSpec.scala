package ru.yandex.vertis.billing.service.metered

import java.util.concurrent.TimeUnit
import com.codahale.metrics.{Clock, SlidingTimeWindowReservoir}
import org.scalatest.Ignore
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.vertis.billing.service.metered.Metered.ClockLargeTickFixWrapper
import ru.yandex.vertis.billing.service.metered.SlidingTimeWindowReservoirSpec._

import scala.concurrent.duration._

/**
  * Test for [[Metered]].histogram() and [[SlidingTimeWindowReservoir]] in particular
  * The reason we have this test - bug in metrics library [[https://github.com/dropwizard/metrics/issues/515]]
  * and as a result - our bug VSBILLING-1460
  *
  * @author zvez
  */
@Ignore //todo(darl) этот тест никогда не запускался, и похоже после обновления библиотеки metrics проблема больше не воспроизводится
class SlidingTimeWindowReservoirSpec extends AnyFreeSpec with Matchers {

  def testSlidingWindow(shouldFail: Boolean)(clockModify: Clock => Clock): Unit = {
    val bigLeap = Long.MaxValue / 256
    val clock = new TestClock(bigLeap / 2)
    val reservoir = new SlidingTimeWindowReservoir(30, TimeUnit.MINUTES, clockModify(clock))

    "measurements should be still there after 15 minutes" in {
      reservoir.update(1)
      reservoir.update(2)
      reservoir.update(3)
      clock.advance(15.minutes)
      reservoir.size() shouldEqual 3
    }

    "more measurements" in {
      reservoir.update(4)
      reservoir.update(5)
      reservoir.size() shouldEqual 5
    }

    "old measurements should get removed after 30 minutes" in {
      clock.advance(20.minutes)
      reservoir.size() shouldEqual 2
      clock.advance(15.minutes)
      reservoir.size() shouldEqual 0
    }

    "large clock ticks' values shouldn't affect reservoir" in {
      reservoir.update(1)
      reservoir.update(2)
      clock.advance((bigLeap / 2).nanos)
      reservoir.update(3)
      if (shouldFail) (reservoir.size() should not).equal(1)
      else reservoir.size() shouldEqual 1
    }

  }

  def testReservoir(clock: Clock) = new SlidingTimeWindowReservoir(30, TimeUnit.MINUTES, clock)

  "SlidingTimeWindowReservoir" - {
    "still has overflow bug (if this test fails - time to remove ClockFixWrapper)" - {
      testSlidingWindow(shouldFail = true)(identity)
    }

    "should work with ClockFixWrapper" - {
      testSlidingWindow(shouldFail = false)(new ClockLargeTickFixWrapper(_))
    }
  }

}

object SlidingTimeWindowReservoirSpec {

  class TestClock(initialTick: Long) extends Clock {
    private var _currentValue = initialTick

    def advance(v: Duration): Unit = {
      _currentValue += v.toNanos
    }

    def getTick = _currentValue
  }
}
