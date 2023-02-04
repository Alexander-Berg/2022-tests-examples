package ru.yandex.vertis.billing.model_core

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.util.DateTimeUtils.now
import ru.yandex.vertis.billing.model_core.FixPrice.unitsToFixPrice
import ru.yandex.vertis.billing.model_core.WithdrawUtils.{
  calculateRebateAmounts,
  isProductWellForType,
  toRevenue,
  TotalAndRebate
}

/**
  * Specs on [[WithdrawUtils]]
  *
  * @author alesavin
  */
class WithdrawUtilsSpec extends AnyWordSpec with Matchers {

  "WithdrawUtils" should {
    val clickProduct = Product(`Raise+Highlighting`(CostPerClick(1L)))
    val showProduct = Product(Raising(CostPerMille(2L)))
    val dayProduct = Product(Highlighting(CostPerDay(3L)))
    val indexingProduct = Product(Highlighting(CostPerIndexing(4L)))
    val dynamicPriceIndexingProduct =
      Product(Highlighting(CostPerIndexing(DynamicPrice(value = Some(5L)))))

    val setOfGoods: Set[Good] =
      Set(`Raise+Highlighting`(CostPerDay(4L)), Highlighting(CostPerClick(5L)))
    val clickDayProduct = Product(setOfGoods)

    "be correct on isProductWellForType" in {
      assert(isProductWellForType(clickProduct, EventTypes.Click))
      assert(!isProductWellForType(showProduct, EventTypes.Click))
      assert(!isProductWellForType(dayProduct, EventTypes.Click))
      assert(isProductWellForType(clickDayProduct, EventTypes.Click))

      assert(!isProductWellForType(clickProduct, EventTypes.Show))
      assert(isProductWellForType(showProduct, EventTypes.Show))
      assert(!isProductWellForType(dayProduct, EventTypes.Show))
      assert(!isProductWellForType(clickDayProduct, EventTypes.Show))

      assert(!isProductWellForType(clickProduct, EventTypes.UniqueShow))
      assert(!isProductWellForType(showProduct, EventTypes.UniqueShow))
      assert(isProductWellForType(dayProduct, EventTypes.UniqueShow))
      assert(isProductWellForType(clickDayProduct, EventTypes.UniqueShow))

      assert(!isProductWellForType(clickProduct, EventTypes.AveragePositionShow))
      assert(!isProductWellForType(showProduct, EventTypes.AveragePositionShow))
      assert(!isProductWellForType(dayProduct, EventTypes.AveragePositionShow))
      assert(!isProductWellForType(clickDayProduct, EventTypes.AveragePositionShow))

      assert(isProductWellForType(clickProduct, EventTypes.ClicksRevenue))
      assert(!isProductWellForType(showProduct, EventTypes.ClicksRevenue))
      assert(!isProductWellForType(dayProduct, EventTypes.ClicksRevenue))
      assert(isProductWellForType(clickDayProduct, EventTypes.ClicksRevenue))

      assert(isProductWellForType(indexingProduct, EventTypes.Indexing))
      assert(isProductWellForType(indexingProduct, EventTypes.IndexingRevenue))
      assert(!isProductWellForType(indexingProduct, EventTypes.Click))
      assert(!isProductWellForType(indexingProduct, EventTypes.ClicksRevenue))
      assert(!isProductWellForType(indexingProduct, EventTypes.Show))
      assert(!isProductWellForType(indexingProduct, EventTypes.AveragePositionShow))

      assert(isProductWellForType(dynamicPriceIndexingProduct, EventTypes.Indexing))
      assert(isProductWellForType(dynamicPriceIndexingProduct, EventTypes.IndexingRevenue))
      assert(!isProductWellForType(dynamicPriceIndexingProduct, EventTypes.Click))
      assert(!isProductWellForType(dynamicPriceIndexingProduct, EventTypes.ClicksRevenue))
      assert(!isProductWellForType(dynamicPriceIndexingProduct, EventTypes.Show))
      assert(!isProductWellForType(dynamicPriceIndexingProduct, EventTypes.AveragePositionShow))
    }

    "be correct on toRevenue" in {
      toRevenue(EventStat(EventTypes.Click, 19), clickProduct) should be(None)
      toRevenue(EventStat(EventTypes.Click, 19), showProduct) should be(None)
      toRevenue(EventStat(EventTypes.Click, 19), dayProduct) should be(None)
      toRevenue(EventStat(EventTypes.Click, 19), clickDayProduct) should be(None)
      toRevenue(EventStat(EventTypes.Click, 19), indexingProduct) should be(None)
      toRevenue(EventStat(EventTypes.Click, 19), dynamicPriceIndexingProduct) should be(None)

      toRevenue(EventStat(EventTypes.Show, 1900), clickProduct) should be(None)
      toRevenue(EventStat(EventTypes.Show, 1900), showProduct) should be(Some(2 * 1900 / 1000))
      toRevenue(EventStat(EventTypes.Show, 1900), dayProduct) should be(None)
      toRevenue(EventStat(EventTypes.Show, 1900), clickDayProduct) should be(None)
      toRevenue(EventStat(EventTypes.Show, 1900), indexingProduct) should be(None)
      toRevenue(EventStat(EventTypes.Show, 1900), dynamicPriceIndexingProduct) should be(None)

      toRevenue(EventStat(EventTypes.UniqueShow, 19), clickProduct) should be(None)
      toRevenue(EventStat(EventTypes.UniqueShow, 19), showProduct) should be(None)
      toRevenue(EventStat(EventTypes.UniqueShow, 19), dayProduct) should be(Some(19 * 3))
      toRevenue(EventStat(EventTypes.UniqueShow, 19), clickDayProduct) should be(Some(19 * 4))
      toRevenue(EventStat(EventTypes.UniqueShow, 19), indexingProduct) should be(None)
      toRevenue(EventStat(EventTypes.UniqueShow, 19), dynamicPriceIndexingProduct) should be(None)

      toRevenue(EventStat(EventTypes.AveragePositionShow, 19), clickProduct) should be(None)
      toRevenue(EventStat(EventTypes.AveragePositionShow, 19), showProduct) should be(None)
      toRevenue(EventStat(EventTypes.AveragePositionShow, 19), dayProduct) should be(None)
      toRevenue(EventStat(EventTypes.AveragePositionShow, 19), clickDayProduct) should be(None)
      toRevenue(EventStat(EventTypes.AveragePositionShow, 19), indexingProduct) should be(None)
      toRevenue(EventStat(EventTypes.AveragePositionShow, 19), dynamicPriceIndexingProduct) should be(None)

      toRevenue(EventStat(EventTypes.ClicksRevenue, 37), clickProduct) should be(Some(37))
      toRevenue(EventStat(EventTypes.ClicksRevenue, 37), showProduct) should be(None)
      toRevenue(EventStat(EventTypes.ClicksRevenue, 37), dayProduct) should be(None)
      toRevenue(EventStat(EventTypes.ClicksRevenue, 37), clickDayProduct) should be(Some(37))
      toRevenue(EventStat(EventTypes.ClicksRevenue, 37), indexingProduct) should be(None)
      toRevenue(EventStat(EventTypes.ClicksRevenue, 37), dynamicPriceIndexingProduct) should
        be(None)

      toRevenue(EventStat(EventTypes.Indexing, 19), clickProduct) should be(None)
      toRevenue(EventStat(EventTypes.Indexing, 19), showProduct) should be(None)
      toRevenue(EventStat(EventTypes.Indexing, 19), dayProduct) should be(None)
      toRevenue(EventStat(EventTypes.Indexing, 19), clickDayProduct) should be(None)
      toRevenue(EventStat(EventTypes.Indexing, 19), indexingProduct) should be(Some(19 * 4))
      toRevenue(EventStat(EventTypes.Indexing, 19), dynamicPriceIndexingProduct) should
        be(Some(19 * 5))

      toRevenue(EventStat(EventTypes.IndexingRevenue, 19), clickProduct) should be(None)
      toRevenue(EventStat(EventTypes.IndexingRevenue, 19), showProduct) should be(None)
      toRevenue(EventStat(EventTypes.IndexingRevenue, 19), dayProduct) should be(None)
      toRevenue(EventStat(EventTypes.IndexingRevenue, 19), clickDayProduct) should be(None)
      toRevenue(EventStat(EventTypes.IndexingRevenue, 19), indexingProduct) should be(Some(19))
      toRevenue(EventStat(EventTypes.IndexingRevenue, 19), dynamicPriceIndexingProduct) should
        be(Some(19))
    }

    "be correct on calculateRebateAmounts" in {
      val product = Product(Placement(CostPerDay(1L)))
      val target = Target.ForProductType(GoodTypes.Placement, CostTypes.CostPerDay)
      val snapshot =
        CampaignSnapshot(now(), "testCampaign", 1, product, FingerprintImpl(Fingerprint.ofProduct(product)))
      val w1 = Withdraw2("1", snapshot, 300, None)
      val w2 = Withdraw2("2", snapshot, 200, None)
      val owner = CustomerId(1, None)

      val d1 = Discount(owner, target, DiscountSourceTypes.Amount, now(), PercentDiscount(10 * 1000))
      val d2 = Discount(owner, target, DiscountSourceTypes.Loyalty, now(), PercentDiscount(20 * 1000))
      val d3 = Discount(owner, target, DiscountSourceTypes.Manually, now(), PercentDiscount(30 * 1000))
      val exclusiveDiscount =
        Discount(owner, target, DiscountSourceTypes.ExclusiveManually, now(), PercentDiscount(50000))

      val single = EffectiveDiscounts(owner, Iterable(d1))
      val usual = EffectiveDiscounts(owner, Iterable(d1, d2, d3))
      val exclusive = EffectiveDiscounts(owner, Iterable(exclusiveDiscount))
      val withdraws = Iterable(w1, w2)

      val total = withdraws.map(_.amount).sum
      val ids = withdraws.map(_.id).toList
      calculateRebateAmounts(withdraws, single).get should be(Map(d1 -> TotalAndRebate(total, 50, ids)))
      calculateRebateAmounts(withdraws, usual).get should be(
        Map(
          d1 -> TotalAndRebate(total, 50, ids),
          d2 -> TotalAndRebate(total, 100, ids),
          d3 -> TotalAndRebate(total, 150, ids)
        )
      )
      calculateRebateAmounts(withdraws, exclusive).get should be(
        Map(exclusiveDiscount -> TotalAndRebate(total, 250, ids))
      )
    }

    "be correct for costs with DynamicPrice" in {
      val dynWithValue = Product(Raising(CostPerMille(DynamicPrice(Some(10L)))))
      val dynWithoutValue = Product(Raising(CostPerMille(DynamicPrice())))
      toRevenue(EventStat(EventTypes.Show, 1800), dynWithValue) should be(Some(18))
      intercept[IllegalArgumentException] {
        toRevenue(EventStat(EventTypes.Show, 1700), dynWithoutValue)
      }
    }
  }

}
