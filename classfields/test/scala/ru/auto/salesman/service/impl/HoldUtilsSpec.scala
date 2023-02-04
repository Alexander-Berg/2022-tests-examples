package ru.auto.salesman.service.impl

import org.joda.time.DateTime
import ru.auto.api.ApiOfferModel.Category
import ru.auto.salesman.model._
import ru.auto.salesman.test.BaseSpec

import scala.util.Success

class HoldUtilsSpec extends BaseSpec {

  "HoldUtils" should {

    import HoldUtils.getHoldId

    "generate hold ids" in {
      getHoldId(
        1L,
        Category.CARS,
        ProductId.Placement,
        None,
        ActivateDate(DateTime.parse("2016-08-22T16:26:44.702+03:00"))
      ) should be(Success("1:placement:1471872404"))
      getHoldId(
        1234567890L,
        Category.CARS,
        ProductId.Fresh,
        None,
        ActivateDate(DateTime.parse("2016-10-22T16:26:44.702+03:00"))
      ) should be(Success("1234567890:boost:1477142804"))
      getHoldId(
        1L,
        Category.CARS,
        ProductId.Badge,
        None,
        ActivateDate(DateTime.parse("2016-10-22T16:26:44.702+03:00"))
      ) should be(Success("1:badge:1477142804"))
      getHoldId(
        1L,
        Category.CARS,
        ProductId.Badge,
        Some("badge-label"),
        ActivateDate(DateTime.parse("2016-10-22T16:26:44.702+03:00"))
      ) should be(
        Success("1:badge:1c45bab14f1229c0a59bb0033e4b9c6e:1477142804")
      )
      getHoldId(
        567L,
        Category.CARS,
        ProductId.PremiumOffer,
        None,
        ActivateDate(DateTime.parse("2016-10-22T16:26:44.702+03:00"))
      ) should be(Success("567:premium-offer:1477142804"))
      getHoldId(
        567L,
        Category.CARS,
        ProductId.Add,
        None,
        ActivateDate(DateTime.parse("2016-10-22T16:26:44.702+03:00"))
      ).isFailure should be(true)
      getHoldId(
        11374L,
        Category.TRUCKS,
        ProductId.Fresh,
        None,
        ActivateDate(DateTime.parse("2016-08-22T16:26:44.702+03:00"))
      ) should be(Success("trucks:11374:boost:1471872404"))
      getHoldId(
        11374L,
        Category.TRUCKS,
        ProductId.Fresh,
        Some("fresh-tag"),
        ActivateDate(DateTime.parse("2016-08-22T16:26:44.702+03:00"))
      ) should be(
        Success(
          "trucks:11374:boost:cc095b54e5b9be37168123a1f10a97b5:1471872404"
        )
      )
      getHoldId(
        11L,
        Category.MOTO,
        ProductId.Top,
        None,
        ActivateDate(DateTime.parse("2016-08-22T16:26:44.702+03:00"))
      ) should be(Success("moto:11:top:1471872404"))
    }
  }
}
