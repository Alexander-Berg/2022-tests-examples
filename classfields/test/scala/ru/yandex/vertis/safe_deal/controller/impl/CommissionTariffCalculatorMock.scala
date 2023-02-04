package ru.yandex.vertis.safe_deal.controller.impl

import cats.implicits.catsSyntaxOptionId
import ru.yandex.vertis.common.Domain
import com.softwaremill.tagging.Tagger
import ru.yandex.vertis.safe_deal.controller.CommissionTariffCalculator
import ru.yandex.vertis.safe_deal.model.{CommissionTariff, Tag}
import zio.Task
import ru.yandex.vertis.safe_deal.model.CommissionTariff.{MaxRub, ZeroRub}

class CommissionTariffCalculatorMock() extends CommissionTariffCalculator.Service {
  import CommissionTariffCalculatorMock._

  override def getBySellingPrice(domain: Domain, sellingPriceRub: Long): Task[CommissionTariff.RangeItem] = {
    if (domain != Domain.DOMAIN_AUTO) {
      Task.fail(new IllegalStateException("Commission tariff not found"))
    } else {
      val range = ranges.filter { r =>
        r.fromRub.getOrElse(ZeroRub).toLong <= sellingPriceRub && sellingPriceRub <= r.toRub.getOrElse(MaxRub).toLong
      }.headOption
      range match {
        case Some(value) => Task.succeed(value)
        case None => Task.fail(new IllegalStateException("Commission tariff not found"))
      }
    }
  }
}

object CommissionTariffCalculatorMock {

  val ranges = List(
    CommissionTariff.RangeItem(
      ZeroRub.some,
      200_000L.taggedWith[Tag.MoneyRub].some,
      2_000L.taggedWith[Tag.MoneyRub],
      None
    ),
    CommissionTariff.RangeItem(
      200_001L.taggedWith[Tag.MoneyRub].some,
      500_000L.taggedWith[Tag.MoneyRub].some,
      4_000L.taggedWith[Tag.MoneyRub],
      5_000L.taggedWith[Tag.MoneyRub].some
    ),
    CommissionTariff.RangeItem(
      500_001L.taggedWith[Tag.MoneyRub].some,
      1_000_000L.taggedWith[Tag.MoneyRub].some,
      8_000L.taggedWith[Tag.MoneyRub],
      10_000L.taggedWith[Tag.MoneyRub].some
    ),
    CommissionTariff.RangeItem(
      1_000_001L.taggedWith[Tag.MoneyRub].some,
      MaxRub.some,
      15_000L.taggedWith[Tag.MoneyRub],
      20_000L.taggedWith[Tag.MoneyRub].some
    )
  )
}
