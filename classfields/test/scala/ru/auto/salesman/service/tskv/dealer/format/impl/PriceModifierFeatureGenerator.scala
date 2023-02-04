package ru.auto.salesman.service.tskv.dealer.format.impl

import org.joda.time.DateTime
import ru.auto.salesman.model.{
  FeatureCount,
  FeatureDiscount,
  FeatureDiscountTypes,
  FeatureInstance,
  FeatureOrigin,
  FeatureTypes,
  FeatureUnits,
  PriceModifierFeature
}

object PriceModifierFeatureGenerator {

  def generateLoyalityFeature(): PriceModifierFeature =
    PriceModifierFeature(
      feature = FeatureInstance(
        id = "loyality-4542323",
        origin = FeatureOrigin("loyality-feature-origin"),
        tag = "loyality-FeatureTag",
        user = "loyality-UserId",
        count = FeatureCount(4, FeatureUnits.Money),
        createTs = DateTime.now().minusDays(1),
        deadline = DateTime.now(),
        payload = ru.auto.salesman.model.FeaturePayload(
          unit = FeatureUnits.Items,
          featureType = FeatureTypes.Loyalty,
          constraint = None,
          discount = Some(FeatureDiscount(FeatureDiscountTypes.Percent, 43))
        )
      ),
      applyCount = FeatureCount(count = 4, unit = FeatureUnits.Money),
      discountAmount = 455554
    )

  def generateNonLoaylityFeature(): PriceModifierFeature =
    PriceModifierFeature(
      feature = FeatureInstance(
        id = "4542323",
        origin = FeatureOrigin("feature-origin"),
        tag = "FeatureTag",
        user = "UserId",
        count = FeatureCount(4, FeatureUnits.Money),
        createTs = DateTime.now().minusDays(1),
        deadline = DateTime.now(),
        payload = ru.auto.salesman.model.FeaturePayload(
          unit = FeatureUnits.Money,
          featureType = FeatureTypes.Promocode,
          constraint = None,
          discount = None
        )
      ),
      applyCount = FeatureCount(count = 4, unit = FeatureUnits.Money),
      discountAmount = 4
    )

}
