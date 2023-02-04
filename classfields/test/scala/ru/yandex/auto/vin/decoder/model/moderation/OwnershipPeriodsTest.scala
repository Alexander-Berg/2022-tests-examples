package ru.yandex.auto.vin.decoder.model.moderation

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.duration._

class OwnershipPeriodsTest extends AnyFlatSpec with MockitoSugar with Matchers {

  private val legal = "LEGAL"
  private val person = "PERSON"

  "ownership period" should "be valid" in {
    OwnershipPeriod(1, Some(100), legal).isValid shouldBe true
    OwnershipPeriod(1, None, person).isValid shouldBe true

    OwnershipPeriod(System.currentTimeMillis(), None, person).isValid shouldBe true
    OwnershipPeriod(System.currentTimeMillis(), Some(System.currentTimeMillis() + 1000), legal).isValid shouldBe true
  }

  "ownership period" should "be invalid" in {
    OwnershipPeriod(-1, Some(100), legal).isValid shouldBe false
    OwnershipPeriod(-1, None, person).isValid shouldBe false

    OwnershipPeriod(1, None, "XEX").isValid shouldBe false

    OwnershipPeriod(System.currentTimeMillis() + 2.day.toMillis, None, person).isValid shouldBe false
    OwnershipPeriod(System.currentTimeMillis(), Some(System.currentTimeMillis() - 1000), legal).isValid shouldBe false
  }

  "ownership periods" should "be valid" in {
    OwnershipPeriods(
      List(
        OwnershipPeriod(10, Some(20), legal),
        OwnershipPeriod(5, Some(10), legal)
      )
    ).isValid shouldBe true

    OwnershipPeriods(
      List(
        OwnershipPeriod(10, None, legal),
        OwnershipPeriod(1, Some(10), legal)
      )
    ).isValid shouldBe true
  }

  "ownership periods" should "be invalid" in {
    // intersect
    OwnershipPeriods(
      List(
        OwnershipPeriod(10, Some(15), legal),
        OwnershipPeriod(9, Some(12), legal)
      )
    ).isValid shouldBe false

    // one invalid
    OwnershipPeriods(
      List(
        OwnershipPeriod(12, Some(10), legal),
        OwnershipPeriod(10, Some(12), legal)
      )
    ).isValid shouldBe false

    // 2 open
    OwnershipPeriods(
      List(
        OwnershipPeriod(10, None, legal),
        OwnershipPeriod(1, None, legal)
      )
    ).isValid shouldBe false

    // not last open
    OwnershipPeriods(
      List(
        OwnershipPeriod(11, Some(12), legal),
        OwnershipPeriod(10, None, legal)
      )
    ).isValid shouldBe false

    // epmty
    OwnershipPeriods(List()).isValid shouldBe false
  }

}
