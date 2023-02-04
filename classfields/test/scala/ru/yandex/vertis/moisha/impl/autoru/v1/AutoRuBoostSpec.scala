package ru.yandex.vertis.moisha.impl.autoru.v1

import ru.yandex.vertis.moisha.impl.autoru.SingleProductDailyPolicySpec
import ru.yandex.vertis.moisha.impl.autoru.model._
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

    "return correct price for Moscow" in {
      checkPolicy(correctInterval, boost(100.rubles), priceIn(0L, Long.MaxValue), inMoscow)
    }

    "return correct price for SPb" in {
      checkPolicy(correctInterval, boost(50.rubles), priceIn(0L, Long.MaxValue), inSPb)
    }

    "fail if region is undefined" in {
      checkPolicyFailure(
        correctInterval,
        Products.Boost,
        priceIn(0L, Long.MaxValue),
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
