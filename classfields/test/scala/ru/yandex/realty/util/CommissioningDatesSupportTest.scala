package ru.yandex.realty.util

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.model.CommissioningDatesSupport
import ru.yandex.realty.model.sites.CommissioningDate

/**
  * Created by Anton Ivanov <antonio@yandex-team.ru> on 14.06.16
  */
@RunWith(classOf[JUnitRunner])
class CommissioningDatesSupportTest extends FlatSpec with Matchers {
  "CommissioningDatesSupport" should "correct merge dates" in {
    CommissioningDatesSupport.distance(new CommissioningDate(2016, 1, false), new CommissioningDate(2016, 2, false)) should be(
      1
    )

    CommissioningDatesSupport.mergeDates(
      new CommissioningDate(2016, 1, false),
      Seq(new CommissioningDate(2016, 2, false))
    ) should be(Some(new CommissioningDate(2016, 2, false)))

    CommissioningDatesSupport.mergeDates(
      new CommissioningDate(2016, 2, false),
      Seq(new CommissioningDate(2016, 1, false), new CommissioningDate(2016, 3, false))
    ) should be(Some(new CommissioningDate(2016, 3, false)))

    CommissioningDatesSupport.mergeDates(
      new CommissioningDate(2016, 2, false),
      Seq(new CommissioningDate(2016, 1, false), new CommissioningDate(2016, 4, false))
    ) should be(Some(new CommissioningDate(2016, 1, false)))
  }

}
