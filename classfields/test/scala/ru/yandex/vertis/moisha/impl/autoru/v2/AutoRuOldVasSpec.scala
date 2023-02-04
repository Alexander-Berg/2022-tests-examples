package ru.yandex.vertis.moisha.impl.autoru.v2

import ru.yandex.vertis.moisha.impl.autoru.SingleProductDailyPolicySpec
import ru.yandex.vertis.moisha.impl.autoru.model._
import ru.yandex.vertis.moisha.impl.autoru.utils._
import ru.yandex.vertis.moisha.model.FundsConversions._
import ru.yandex.vertis.moisha.model._

/**
  * Specs on AutoRu old VAS: [[Products.PremiumOffer]], [[Products.Highlighting]], [[Products.Top]]
  * Rates are described in project https://planner.yandex-team.ru/projects/31761/
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
trait AutoRuOldVasSpec extends SingleProductDailyPolicySpec {

  import AutoRuOldVasSpec._

  "PremiumOffer policy" should {
    "support product Premium" in {
      policy.products.contains(Products.PremiumOffer.toString) should be(true)
    }

    "return correct price for premium-offer in Moscow" in {
      checkPolicy(correctInterval, premiumOffer(30.rubles), priceIn(0L, Long.MaxValue), inMoscow)
    }

    "return correct price for premium-offer in Spb" in {
      checkPolicy(correctInterval, premiumOffer(30.rubles), priceIn(0L, Long.MaxValue), inSPb)
    }

    "return correct price for premium-offer in Krasondar" in {
      checkPolicy(correctInterval, premiumOffer(6.rubles), priceIn(0L, Long.MaxValue), inRegion(RegKrasnodar))
    }

    "return correct price for premium-offer in Voronezh" in {
      checkPolicy(correctInterval, premiumOffer(6.rubles), priceIn(0L, Long.MaxValue), inRegion(RegVoronezh))
    }

    "return correct price for premium-offer in Nnov" in {
      checkPolicy(correctInterval, premiumOffer(6.rubles), priceIn(0L, Long.MaxValue), inRegion(RegNNovgorod))
    }

    "return correct price for premium-offer in Ryazan" in {
      checkPolicy(correctInterval, premiumOffer(6.rubles), priceIn(0L, Long.MaxValue), inRegion(RegRyazan))
    }
  }

  "Highlighting policy" should {
    "support product Highlighting" in {
      policy.products.contains(Products.Highlighting.toString) should be(true)
    }

    "return correct price for highlighting in Moscow" in {
      checkPolicy(correctInterval, highlighting(10.rubles), priceIn(0L, Long.MaxValue), inMoscow)
    }

    "return correct price for highlighting in Spb" in {
      checkPolicy(correctInterval, highlighting(10.rubles), priceIn(0L, Long.MaxValue), inSPb)
    }

    "return correct price for highlighting in Krasondar" in {
      checkPolicy(correctInterval, highlighting(2.rubles), priceIn(0L, Long.MaxValue), inRegion(RegKrasnodar))
    }

    "return correct price for highlighting in Voronezh" in {
      checkPolicy(correctInterval, highlighting(2.rubles), priceIn(0L, Long.MaxValue), inRegion(RegVoronezh))
    }

    "return correct price for highlighting in Nnov" in {
      checkPolicy(correctInterval, highlighting(2.rubles), priceIn(0L, Long.MaxValue), inRegion(RegNNovgorod))
    }

    "return correct price for highlighting in Ryazan" in {
      checkPolicy(correctInterval, highlighting(2.rubles), priceIn(0L, Long.MaxValue), inRegion(RegRyazan))
    }
  }

  "Top policy" should {
    "support product Top" in {
      policy.products.contains(Products.Top.toString) should be(true)
    }

    "return correct price for top in Moscow" in {
      checkPolicy(correctInterval, top(333.rubles), priceIn(0L, Long.MaxValue), inMoscow)
    }

    "return correct price for top in Spb" in {
      checkPolicy(correctInterval, top(333.rubles), priceIn(0L, Long.MaxValue), inSPb)
    }

    "return correct price for top in Krasondar" in {
      checkPolicy(correctInterval, top(66.rubles), priceIn(0L, Long.MaxValue), inRegion(RegKrasnodar))
    }

    "return correct price for top in Voronezh" in {
      checkPolicy(correctInterval, top(66.rubles), priceIn(0L, Long.MaxValue), inRegion(RegVoronezh))
    }

    "return correct price for top in Nnov" in {
      checkPolicy(correctInterval, top(66.rubles), priceIn(0L, Long.MaxValue), inRegion(RegNNovgorod))
    }

    "return correct price for top in Ryazan" in {
      checkPolicy(correctInterval, top(66.rubles), priceIn(0L, Long.MaxValue), inRegion(RegRyazan))
    }
  }

}

object AutoRuOldVasSpec {

  def premiumOffer(price: Funds): AutoRuProduct =
    AutoRuProduct(
      Products.PremiumOffer,
      Set(
        AutoRuGood(
          Goods.Custom,
          Costs.PerIndexing,
          price
        )
      ),
      duration = DefaultDuration
    )

  def highlighting(price: Funds): AutoRuProduct =
    AutoRuProduct(
      Products.Highlighting,
      Set(
        AutoRuGood(
          Goods.Custom,
          Costs.PerIndexing,
          price
        )
      ),
      duration = DefaultDuration
    )

  def top(price: Funds): AutoRuProduct =
    AutoRuProduct(
      Products.Top,
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
