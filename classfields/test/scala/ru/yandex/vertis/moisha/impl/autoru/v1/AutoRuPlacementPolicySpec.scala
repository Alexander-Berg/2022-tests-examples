package ru.yandex.vertis.moisha.impl.autoru.v1

import ru.yandex.vertis.moisha.impl.autoru.AutoRuPolicy.AutoRuRequest
import ru.yandex.vertis.moisha.impl.autoru.SingleProductDailyPolicySpec
import ru.yandex.vertis.moisha.impl.autoru.gens._
import ru.yandex.vertis.moisha.impl.autoru.model._
import ru.yandex.vertis.moisha.model.FundsConversions._
import ru.yandex.vertis.moisha.model._
import ru.yandex.vertis.moisha.model.gens.Producer

/**
  * Specs on AutoRu policy for [[Products.Placement]]
  * Rates are described in project https://planner.yandex-team.ru/projects/31629/
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
trait AutoRuPlacementPolicySpec extends SingleProductDailyPolicySpec {

  import AutoRuPlacementPolicySpec._

  def product: Products.Value = Products.Placement

  "Placement policy" should {
    "support Products.Placement" in {
      policy.products.contains(Products.Placement.toString) should be(true)
    }

    "return correct price for Moscow in first day" in {
      checkPolicy(correctInterval, placement(250.rubles), priceIn(0L, Long.MaxValue), todaysOffer, inMoscow)
    }

    "return correct price for SPb in first day" in {
      checkPolicy(correctInterval, placement(200.rubles), priceIn(0L, Long.MaxValue), todaysOffer, inSPb)
    }

    "return correct prices for Moscow in other days" in {
      checkPolicy(correctInterval, placement(10.rubles), priceIn(0L, 300.thousands), oldOffer, inMoscow)

      checkPolicy(correctInterval, placement(20.rubles), priceIn(300.thousands, 500.thousands), oldOffer, inMoscow)

      checkPolicy(correctInterval, placement(35.rubles), priceIn(500.thousands, 1500.thousands), oldOffer, inMoscow)

      checkPolicy(correctInterval, placement(50.rubles), priceIn(1500.thousands, Long.MaxValue), oldOffer, inMoscow)
    }

    "return correct prices for SPb in other days" in {
      checkPolicy(correctInterval, placement(10.rubles), priceIn(0L, 300.thousands), oldOffer, inSPb)

      checkPolicy(correctInterval, placement(15.rubles), priceIn(300.thousands, 500.thousands), oldOffer, inSPb)

      checkPolicy(correctInterval, placement(15.rubles), priceIn(500.thousands, 1500.thousands), oldOffer, inSPb)

      checkPolicy(correctInterval, placement(20.rubles), priceIn(1500.thousands, Long.MaxValue), oldOffer, inSPb)
    }

    "fail if region is undefined" in {
      checkPolicyFailure(
        correctInterval,
        Products.Placement,
        priceIn(0L, Long.MaxValue),
        inRegion(0)
      )
    }

    "return empty points if interval is incorrect" in {
      checkPolicyEmpty(
        incorrectInterval,
        Products.Placement,
        priceIn(0L, Long.MaxValue),
        inMoscow
      )
    }
  }
}

object AutoRuPlacementPolicySpec {

  val TestIterations = 50

  def placement(price: Funds): AutoRuProduct =
    AutoRuProduct(
      Products.Placement,
      Set(
        AutoRuGood(Goods.Custom, Costs.PerIndexing, price)
      ),
      duration = DefaultDuration
    )

  import ru.yandex.vertis.moisha.environment._

  def oneDayPlacement: AutoRuRequest =
    RequestGen.next.copy(product = Products.Placement, interval = wholeDay(now()))

}
