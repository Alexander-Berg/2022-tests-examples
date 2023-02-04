package ru.yandex.vertis.moisha.impl.autoru.v5

import ru.yandex.vertis.moisha.impl.autoru.SingleProductDailyPolicySpec
import ru.yandex.vertis.moisha.impl.autoru.model._
import ru.yandex.vertis.moisha.impl.autoru.utils._
import ru.yandex.vertis.moisha.model.FundsConversions._
import ru.yandex.vertis.moisha.model._

/**
  * Specs on AutoRu policy for [[Products.Boost]]
  * Rates are described in project https://planner.yandex-team.ru/projects/31761/
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
trait AutoRuBoostSpec extends SingleProductDailyPolicySpec {

  import AutoRuBoostSpec._

  "Boost policy" should {
    "support product PremiumOffer" in {
      policy.products.contains(Products.Boost.toString) should be(true)
    }

    "return correct price for Moscow with cars" in {
      checkPolicy(
        correctInterval,
        boost(150.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(Transports.Cars),
        inMoscow
      )
    }

    "return correct price for Moscow with ots" in {
      checkPolicy(
        correctInterval,
        boost(50.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(Transports.Moto),
        inMoscow
      )
    }

    "return correct price for SPb with cars" in {
      checkPolicy(correctInterval, boost(100.rubles), priceIn(0L, Long.MaxValue), withTransport(Transports.Cars), inSPb)
    }

    "return correct price for SPb with ots" in {
      checkPolicy(correctInterval, boost(20.rubles), priceIn(0L, Long.MaxValue), withTransport(Transports.Moto), inSPb)
    }

    "return correct price for RegOfHopeV5 with cars" in {
      for (region <- RegOfHopeV5) {
        checkPolicy(
          correctInterval,
          boost(20.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(Transports.Cars),
          inRegion(region)
        )
      }
    }

    "return correct price for RegOfHopeV5 with ots" in {
      for (region <- RegOfHopeV5) {
        checkPolicy(
          correctInterval,
          boost(20.rubles),
          priceIn(0L, Long.MaxValue),
          withTransport(Transports.Moto),
          inRegion(region)
        )
      }
    }

    "return correct price for regions with cars" in {
      checkPolicy(
        correctInterval,
        boost(20.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(Transports.Cars),
        inRegion(0)
      )
    }

    "return correct price for regions with ots" in {
      checkPolicy(
        correctInterval,
        boost(10.rubles),
        priceIn(0L, Long.MaxValue),
        withTransport(Transports.Moto),
        inRegion(0)
      )
    }

    "return empty points if interval is incorrect" in {
      checkPolicyEmpty(
        incorrectInterval,
        Products.Boost,
        priceIn(0L, Long.MaxValue),
        inMoscow
      )
    }
  }
}

object AutoRuBoostSpec {

  def boost(price: Funds): AutoRuProduct =
    AutoRuProduct(
      Products.Boost,
      Set(
        AutoRuGood(
          Goods.Custom,
          Costs.PerIndexing,
          price
        )
      ),
      duration = DefaultDuration
    )

}
