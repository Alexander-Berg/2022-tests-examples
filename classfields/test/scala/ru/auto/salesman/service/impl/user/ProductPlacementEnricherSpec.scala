package ru.auto.salesman.service.impl.user

import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Placement
import ru.auto.salesman.model.user.periodical_discount_exclusion.Product.NoActiveDiscount
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains, OfferTypes, ProductId}
import ru.auto.salesman.service.impl.user.autoru.price.service.placement.{
  PlacementQuotaCalculatorImpl,
  ProductPlacementEnricher
}
import ru.auto.salesman.service.user.PriceService.{EnrichedProduct, ProductQuota}
import ru.auto.salesman.service.user.autoru.price.service.placement.PlacementInfoExtractor
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.ServiceModelGenerators

class ProductPlacementEnricherSpec extends BaseSpec with ServiceModelGenerators {
  val placementInfoExtractor = mock[PlacementInfoExtractor]

  val placementQuotaCalculator: PlacementQuotaCalculatorImpl =
    mock[PlacementQuotaCalculatorImpl]

  val productPlacementEnricher =
    new ProductPlacementEnricher(
      placementInfoExtractor,
      placementQuotaCalculator
    )

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  "enrichPlacement" should {

    val paymentNotRequired = EnrichedProduct(
      Placement,
      ProductId.withAlias(Placement.name),
      isFreeByQuota = true,
      baseQuota = 0,
      quotaDuration = None,
      limitReason = None,
      features = Nil,
      productPeriodicalDiscount = NoActiveDiscount,
      offerType = Some(OfferTypes.Regular)
    )

    def enrichedProduct(quota: ProductQuota): EnrichedProduct =
      EnrichedProduct(
        Placement,
        ProductId.withAlias(Placement.name),
        isFreeByQuota = false,
        baseQuota = quota.baseQuota,
        quotaDuration = quota.duration,
        limitReason = quota.reason,
        features = Nil,
        productPeriodicalDiscount = NoActiveDiscount,
        offerType = quota.offerType
      )

    "for not active and paid offer" in {
      forAll(enrichedOfferGen(NotActiveOfferGen), UserSellerTypeGen) {
        (offer, userSellerType) =>
          (placementInfoExtractor.alreadyPaidPlacement _)
            .expects(*)
            .returningZ(true)

          productPlacementEnricher
            .enrichPlacement(offer, userSellerType)
            .success
            .value shouldBe paymentNotRequired
      }
    }

    "for active offer, when quota size = 0" in {
      forAll(
        enrichedOfferGen(ActiveOfferGen),
        UserSellerTypeGen,
        ProductQuotaGen.map(_.copy(size = 0))
      ) { (offer, userSellerType, quota) =>
        (placementQuotaCalculator.placementQuota _)
          .expects(*, *)
          .returningZ(quota)

        productPlacementEnricher
          .enrichPlacement(offer, userSellerType)
          .success
          .value shouldBe enrichedProduct(quota)
      }
    }

    "for active offer, when quota size > 0" in {
      forAll(
        enrichedOfferGen(ActiveOfferGen),
        UserSellerTypeGen,
        ProductQuotaGen.map(_.copy(size = 1))
      ) { (offer, userSellerType, quota) =>
        (placementQuotaCalculator.placementQuota _)
          .expects(*, *)
          .returningZ(quota)

        productPlacementEnricher
          .enrichPlacement(offer, userSellerType)
          .success
          .value shouldBe paymentNotRequired
      }
    }

    "for not paid offer, when quota size = 0" in {
      //not paid offer need to reach placementPaymentNotRequired check, because of && operator
      forAll(
        enrichedOfferGen(NotActiveOfferGen),
        UserSellerTypeGen,
        ProductQuotaGen.map(_.copy(size = 0))
      ) { (offer, userSellerType, quota) =>
        (placementInfoExtractor.alreadyPaidPlacement _)
          .expects(*)
          .returningZ(false)

        (placementQuotaCalculator.placementQuota _)
          .expects(*, *)
          .returningZ(quota)

        productPlacementEnricher
          .enrichPlacement(offer, userSellerType)
          .success
          .value shouldBe enrichedProduct(quota)
      }
    }

    "for not paid offer, when quota size > 0" in {
      //not paid offer need to reach placementPaymentNotRequired check, because of && operator
      forAll(
        enrichedOfferGen(NotActiveOfferGen),
        UserSellerTypeGen,
        ProductQuotaGen.map(_.copy(size = 1))
      ) { (offer, userSellerType, quota) =>
        (placementInfoExtractor.alreadyPaidPlacement _)
          .expects(*)
          .returningZ(false)

        (placementQuotaCalculator.placementQuota _)
          .expects(*, *)
          .returningZ(quota)

        productPlacementEnricher
          .enrichPlacement(offer, userSellerType)
          .success
          .value shouldBe paymentNotRequired
      }
    }

  }

}
