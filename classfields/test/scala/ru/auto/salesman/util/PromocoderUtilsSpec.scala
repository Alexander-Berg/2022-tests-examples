package ru.auto.salesman.util

import ru.auto.salesman.environment._
import ru.auto.salesman.model.{
  FeatureCount,
  FeatureInstance,
  FeatureOrigin,
  FeaturePayload,
  FeatureUnits,
  ModifiedPrice,
  PriceModifierFeature,
  ProductId
}
import ru.auto.salesman.service.impl.DeciderUtilsSpec.{
  feature,
  loyaltyDiscountFeature,
  loyaltyFeature
}
import ru.auto.salesman.test.BaseSpec

class PromocoderUtilsSpec extends BaseSpec {

  import PromocoderUtils._

  "finalPrice" should {
    "return zero price for non empty feature" in {
      val payload = FeaturePayload(FeatureUnits.Items)
      val featureCount = FeatureCount(1L, FeatureUnits.Items)
      val origin = FeatureOrigin("f_orig")
      val feature = FeatureInstance(
        "1",
        origin,
        "boost",
        "1",
        featureCount,
        now(),
        now().plusDays(1),
        payload
      )
      val price = 123L

      priceWithFeature(price, Some(feature)) should be(0)
    }

    "return actual goods price for none feature" in {
      val price = 123L

      priceWithFeature(price, None) should be(price)
    }
  }

  "featureCountForSpent" should {
    "return price for money unit payload" in {
      val payload = FeaturePayload(FeatureUnits.Money)
      val featureCount = FeatureCount(1L, FeatureUnits.Money)
      val origin = FeatureOrigin("f_orig")
      val feature = FeatureInstance(
        "1",
        origin,
        "boost",
        "1",
        featureCount,
        now(),
        now().plusDays(1),
        payload
      )
      val price = 123L

      feature.createCountForPaying(1L, price).count should be(price)
    }

    "return unit for unit payload" in {
      val payload = FeaturePayload(FeatureUnits.Items)
      val featureCount = FeatureCount(1L, FeatureUnits.Items)
      val origin = FeatureOrigin("f_orig")
      val feature = FeatureInstance(
        "1",
        origin,
        "boost",
        "1",
        featureCount,
        now(),
        now().plusDays(1),
        payload
      )
      val price = 123L

      feature.createCountForPaying(1L, price).count should be(1L)
    }
  }

  "filterFeatureInstanceBySpentCount" should {
    "return feature for feature count more than spent count" in {
      val payload = FeaturePayload(FeatureUnits.Money)
      val featureCount = FeatureCount(124L, FeatureUnits.Money)
      val origin = FeatureOrigin("f_orig")
      val feature = FeatureInstance(
        "1",
        origin,
        "boost",
        "1",
        featureCount,
        now(),
        now().plusDays(1),
        payload
      )

      filterFeatureInstanceBySpentCount(Some(feature), 123L) should be(
        Some(feature)
      )
    }

    "return empty feature for feature count less than spent count" in {
      val payload = FeaturePayload(FeatureUnits.Money)
      val featureCount = FeatureCount(122L, FeatureUnits.Money)
      val origin = FeatureOrigin("f_orig")
      val feature = FeatureInstance(
        "1",
        origin,
        "boost",
        "1",
        featureCount,
        now(),
        now().plusDays(1),
        payload
      )

      filterFeatureInstanceBySpentCount(Some(feature), 123L) should be(None)
    }

    "return empty feature for empty feature" in {
      val feature = Option.empty[FeatureInstance]

      filterFeatureInstanceBySpentCount(feature, 123L) should be(None)
    }

    "return empty feature for empty spent count" in {
      val payload = FeaturePayload(FeatureUnits.Money)
      val featureCount = FeatureCount(122L, FeatureUnits.Money)
      val origin = FeatureOrigin("f_orig")
      val feature = FeatureInstance(
        "1",
        origin,
        "boost",
        "1",
        featureCount,
        now(),
        now().plusDays(1),
        payload
      )

      filterFeatureInstanceBySpentCount(Some(feature), 0) should be(None)
    }
  }

  "extractDiscountPercent" should {
    "return Some(percent) from suitable feature" in {
      loyaltyDiscountFeature.extractDiscountPercent shouldBe Some(
        loyaltyDiscountFeature.payload.discount.get.value
      )
    }

    "return None from other feature type" in {
      loyaltyFeature.extractDiscountPercent shouldBe None
    }
  }

  "isLoyaltyDiscount" should {
    "return true for suitable feature" in {
      loyaltyDiscountFeature.isLoyaltyDiscount shouldBe true
    }

    "return false for other feature" in {
      loyaltyFeature.isLoyaltyDiscount shouldBe false
    }
  }

  "PriceWithFeaturesApplier" should {
    "apply discount and do ceil rounding" in {
      val price = ModifiedPrice(price = 300L)
      val priceWithDiscount = 200L
      val discountAmount = 100L
      val expected = ModifiedPrice(
        price = priceWithDiscount,
        List(
          PriceModifierFeature(
            loyaltyDiscountFeature,
            FeatureCount(count = 1, FeatureUnits.Items),
            discountAmount
          )
        )
      )
      price.withFeature(
        itemsCount = 1,
        loyaltyDiscountFeature
      ) shouldBe expected
    }

    "apply other loyalty feature and return zero price" in {
      val originalPrice = 300L
      val price = ModifiedPrice(originalPrice)
      val expected = ModifiedPrice(
        price = 0L,
        List(
          PriceModifierFeature(
            loyaltyFeature,
            FeatureCount(originalPrice, FeatureUnits.Money),
            originalPrice
          )
        )
      )
      price.withFeature(itemsCount = 1, loyaltyFeature) shouldBe expected
    }

    "apply item feature and return zero price" in {
      val originalPrice = 300L
      val itemFeature = feature(ProductId.Placement).copy(
        count = FeatureCount(1000L, FeatureUnits.Items),
        payload = FeaturePayload(FeatureUnits.Items)
      )
      val price = ModifiedPrice(originalPrice)
      val expected = ModifiedPrice(
        price = 0L,
        List(
          PriceModifierFeature(
            itemFeature,
            FeatureCount(count = 1, FeatureUnits.Items),
            originalPrice
          )
        )
      )
      price.withFeature(itemsCount = 1, itemFeature) shouldBe expected
    }

    "apply discount then right apply money feature" in {
      val originalPrice = 300L
      val priceWithDiscount = 200L
      val discountAmount = 100L
      val moneyFeature = feature(ProductId.Placement)
      val price = ModifiedPrice(originalPrice)
      val expected = ModifiedPrice(
        price = 0L,
        List(
          PriceModifierFeature(
            loyaltyDiscountFeature,
            FeatureCount(count = 1, FeatureUnits.Items),
            discountAmount
          ),
          PriceModifierFeature(
            moneyFeature,
            FeatureCount(200L, FeatureUnits.Money),
            priceWithDiscount
          )
        )
      )
      price
        .withFeature(itemsCount = 1, loyaltyDiscountFeature)
        .withFeature(itemsCount = 1, moneyFeature) shouldBe expected
    }
  }

}
