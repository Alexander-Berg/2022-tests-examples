package ru.yandex.vertis.moisha.impl.autoru.v1

import ru.yandex.vertis.moisha.impl.autoru.SingleProductDailyPolicySpec
import ru.yandex.vertis.moisha.impl.autoru.model._
import ru.yandex.vertis.moisha.model.FundsConversions._
import ru.yandex.vertis.moisha.model._

/**
  * Specs on AutoRu policy for [[Products.Premium]]
  * Rates are described in project https://planner.yandex-team.ru/projects/31761/
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
trait AutoRuPremiumSpec extends SingleProductDailyPolicySpec {

  import AutoRuPremiumSpec._

  "Premium policy" should {
    "support product PremiumOffer" in {
      policy.products.contains(Products.Premium.toString) should be(true)
    }

    "return correct price for Moscow" in {
      checkPolicy(correctInterval, premium(250.rubles), priceIn(0L, Long.MaxValue), inMoscow)
    }

    "return correct price for SPb" in {
      checkPolicy(correctInterval, premium(125.rubles), priceIn(0L, Long.MaxValue), inSPb)
    }

    "fail if region is undefined" in {
      checkPolicyFailure(
        correctInterval,
        Products.Premium,
        priceIn(0L, Long.MaxValue),
        inRegion(0)
      )
    }

    "return empty points if interval is incorrect" in {
      checkPolicyEmpty(
        incorrectInterval,
        Products.Premium,
        priceIn(0L, Long.MaxValue),
        inMoscow
      )
    }
  }
}

object AutoRuPremiumSpec {

  def premium(price: Funds): AutoRuProduct =
    AutoRuProduct(
      Products.Premium,
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
