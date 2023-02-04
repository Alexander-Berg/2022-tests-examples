package ru.yandex.realty.model.time

import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase

@RunWith(classOf[JUnitRunner])
class QuarterUnitSpec extends SpecBase with Matchers {

  "FinishQuarter" should {
    "accept valid input (year > 0, 1 <= quarter <= 4)" in {
      QuarterUnit(2017, 4)
      QuarterUnit(2018, 1)
    }

    "reject invalid input (year = 0)" in {
      an[IllegalArgumentException] should be thrownBy QuarterUnit(0, 3)
    }

    "reject invalid input (year < 0)" in {
      an[IllegalArgumentException] should be thrownBy QuarterUnit(-1, 2)
    }

    "reject invalid input (quarter < 0)" in {
      an[IllegalArgumentException] should be thrownBy QuarterUnit(2018, -1)
    }

    "reject invalid input (quarter = 0)" in {
      an[IllegalArgumentException] should be thrownBy QuarterUnit(2016, 0)
    }

    "reject invalid input (quarter = 5)" in {
      an[IllegalArgumentException] should be thrownBy QuarterUnit(1703, 5)
    }

    "reject invalid input (quarter > 5)" in {
      an[IllegalArgumentException] should be thrownBy QuarterUnit(1998, 215796789)
    }

    "allow conversions to/from 'quarter index'" in {
      val fq1 = QuarterUnit(2017, 4)
      val fq2 = QuarterUnit(2018, 1)

      QuarterUnit.ofQuarterIndex(fq1.toQuarterIndex) should contain(fq1)
      QuarterUnit.ofQuarterIndex(fq2.toQuarterIndex) should contain(fq2)
    }

    "allow comparisons" in {
      val fq1 = QuarterUnit(2017, 4)
      val fq2 = QuarterUnit(2018, 1)

      fq1 should not be fq2
      fq1 should be <= fq1
      fq2 should be <= fq2
      fq1 should be < fq2
    }

    "allow additions and subtractions" in {
      val fq = QuarterUnit(2017, 4)
      val fqi = fq.toQuarterIndex

      val halfAYearAgo = fq - 2
      val aQuarterAgo = halfAYearAgo + 1

      halfAYearAgo should be < aQuarterAgo
      aQuarterAgo should be < fq
      halfAYearAgo should be < fq

      aQuarterAgo + 1 should be(fq)
    }

    "appear from positive 'quarter index'" in {
      QuarterUnit.ofQuarterIndex(8066) should contain(QuarterUnit(2016, 3))
      QuarterUnit.ofQuarterIndex(8071) should contain(QuarterUnit(2017, 4))
      QuarterUnit.ofQuarterIndex(8072) should contain(QuarterUnit(2018, 1))
    }
  }

}
