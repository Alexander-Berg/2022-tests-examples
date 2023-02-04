package ru.yandex.common.monitoring.error

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{Matchers, WordSpec}


@RunWith(classOf[JUnitRunner])
class ErrorReservoirsSpec
  extends WordSpec
  with Matchers {

  "ErrorReservoirs" should {
    "register reservoir by name only once" in {
      val initial = ErrorReservoirs.register("foo", new ExpiringWarningErrorPercentileReservoir(5, 100))
      val returned = ErrorReservoirs.register("foo", new ExpiringWarningErrorPercentileReservoir(7, 100))
      returned should be(initial)
    }
  }

}
