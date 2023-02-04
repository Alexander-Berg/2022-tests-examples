package ru.yandex.vertis.moisha.impl.autoru.v2

import ru.yandex.vertis.moisha.impl.autoru.SingleProductDailyPolicySpec
import ru.yandex.vertis.moisha.impl.autoru.model._
import ru.yandex.vertis.moisha.impl.autoru.utils._
import ru.yandex.vertis.moisha.model.Funds
import ru.yandex.vertis.moisha.model.FundsConversions._

/**
  * Specs on AutoRu policy for [[Products.Special]]
  * Rates are described in project https://planner.yandex-team.ru/projects/31761/
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
trait AutoRuSpecialPolicySpec extends SingleProductDailyPolicySpec {

  import AutoRuSpecialPolicySpec._

  "Special policy" should {
    "support product Special" in {
      policy.products.contains(Products.Special.toString) should be(true)
    }

    "return correct price for Moscow" in {
      checkPolicy(correctInterval, special(50.rubles), priceIn(0L, 500.thousands), inMoscow)

      checkPolicy(correctInterval, special(75.rubles), priceIn(500.thousands, 1500.thousands), inMoscow)

      checkPolicy(correctInterval, special(100.rubles), priceIn(1500.thousands, Long.MaxValue), inMoscow)
    }

    "return correct price for SPb" in {
      checkPolicy(correctInterval, special(25.rubles), priceIn(0L, 500.thousands), inSPb)

      checkPolicy(correctInterval, special(37.rubles + 50.cents), priceIn(500.thousands, 1500.thousands), inSPb)

      checkPolicy(correctInterval, special(50.rubles), priceIn(1500.thousands, Long.MaxValue), inSPb)
    }

    "return correct price for Krasnodar" in {
      checkPolicy(correctInterval, special(10.rubles), priceIn(0L, Long.MaxValue), inRegion(RegKrasnodar))
    }

    "return correct price for Voronezh" in {
      checkPolicy(correctInterval, special(10.rubles), priceIn(0L, Long.MaxValue), inRegion(RegVoronezh))
    }

    "return correct price for NNov" in {
      checkPolicy(correctInterval, special(10.rubles), priceIn(0L, Long.MaxValue), inRegion(RegNNovgorod))
    }

    "return correct price for Ryazan" in {
      checkPolicy(correctInterval, special(10.rubles), priceIn(0L, Long.MaxValue), inRegion(RegRyazan))
    }

    "fail if region is undefined" in {
      checkPolicyFailure(
        correctInterval,
        Products.Special,
        priceIn(0L, Long.MaxValue),
        inRegion(0)
      )
    }

    "return empty points if interval is incorrect" in {
      checkPolicyEmpty(
        incorrectInterval,
        Products.Special,
        priceIn(0L, Long.MaxValue),
        inMoscow
      )
    }
  }
}

object AutoRuSpecialPolicySpec {

  def special(price: Funds): AutoRuProduct =
    AutoRuProduct(
      Products.Special,
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
