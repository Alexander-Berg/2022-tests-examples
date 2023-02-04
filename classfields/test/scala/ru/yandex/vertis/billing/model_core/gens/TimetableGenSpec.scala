package ru.yandex.vertis.billing.model_core.gens

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.DefaultPropertyChecks

/**
  * Specs on [[TimetableGen]]-related gens
  *
  * @author alex-kovalenko
  */
class TimetableGenSpec extends AnyWordSpec with Matchers with DefaultPropertyChecks {

  "LocalTimeInterval" in {
    forAll(LocalTimeIntervalGen) { timeInterval =>
      timeInterval.to.isBefore(timeInterval.from) shouldBe false
    }
  }

  "LocalDateInterval" in {
    forAll(LocalDateIntervalGen) { dateInterval =>
      dateInterval.to.isBefore(dateInterval.from) shouldBe false
    }
  }
}
