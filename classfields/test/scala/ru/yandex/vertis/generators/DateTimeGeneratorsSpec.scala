package ru.yandex.vertis.generators

import org.joda.time.Instant
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import org.scalatest.{Matchers, WordSpec}

@RunWith(classOf[JUnitRunner])
class DateTimeGeneratorsSpec
  extends WordSpec
    with Matchers
    with DateTimeGenerators
    with PropertyChecks {

  "instantInPast" should {

    "respond with instant in past" in {
      forAll(instantInPast) { instant =>
        instant.isBeforeNow() shouldBe true
      }
    }

    "respond with instant that is later than now minus DefaultMaxDistance" in {
      // minus 10 seconds just to defend against test flaps
      val base = Instant.now().minusDays(7).minusSeconds(10)
      forAll(instantInPast) { instant =>
        instant.isAfter(base) shouldBe true
      }
    }
  }
}
