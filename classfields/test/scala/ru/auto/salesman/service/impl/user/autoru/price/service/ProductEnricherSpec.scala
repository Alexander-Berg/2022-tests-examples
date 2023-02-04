package ru.auto.salesman.service.impl.user.autoru.price.service

import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Placement
import ru.auto.salesman.model.user.product.ProductProvider.AutoruSubscriptions.OffersHistoryReports
import ru.auto.salesman.model.user.periodical_discount_exclusion._
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains, ProductId}
import ru.auto.salesman.service.impl.user.autoru.price.service.placement.ProductPlacementEnricher
import ru.auto.salesman.service.impl.user.autoru.price.service.periodical_discount.ProductPeriodicalDiscount
import ru.auto.salesman.service.user.PriceService.EnrichedProduct
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.ServiceModelGenerators

class ProductEnricherSpec extends BaseSpec with ServiceModelGenerators {

  val productPlacementEnricher: ProductPlacementEnricher =
    mock[ProductPlacementEnricher]

  val productEnricher = new ProductEnricher(productPlacementEnricher)

  "productEnricher" should {
    "enrich product with default fields if product not placement" in {
      forAll(
        EnrichedPriceRequestContextGen.map(
          _.copy(
            userPeriodicalDiscount = User.NoActiveDiscount,
            promocoderFeatures = Map.empty
          )
        ),
        ProductNotPlacementGen
      ) { (context, notPlacement) =>
        val expected = EnrichedProduct(
          notPlacement,
          ProductId.withAlias(notPlacement.name),
          isFreeByQuota = false,
          baseQuota = 0,
          None,
          limitReason = None,
          Nil,
          Product.NoActiveDiscount,
          offerType = None
        )

        productEnricher
          .enrichProduct(notPlacement, context)
          .success
          .value shouldBe expected
      }
    }

    "enrich not placement product with features" in {
      forAll(
        EnrichedPriceRequestContextGen.map(
          _.copy(userPeriodicalDiscount = User.NoActiveDiscount)
        ),
        ProductNotPlacementGen
      ) { (context, notPlacement) =>
        val features = context.promocoderFeatures.getOrElse(notPlacement, Nil)

        val expected = EnrichedProduct(
          notPlacement,
          ProductId.withAlias(notPlacement.name),
          isFreeByQuota = false,
          baseQuota = 0,
          None,
          limitReason = None,
          features = features,
          Product.NoActiveDiscount,
          offerType = None
        )

        productEnricher
          .enrichProduct(notPlacement, context)
          .success
          .value shouldBe expected
      }
    }

    "enrich not placement product with periodical discount" in {
      forAll(
        EnrichedPriceRequestContextGen.map(
          _.copy(promocoderFeatures = Map.empty)
        ),
        ProductNotPlacementGen
      ) { (context, notPlacement) =>
        val productPeriodicalDiscount = ProductPeriodicalDiscount.forProduct(
          notPlacement,
          context.userPeriodicalDiscount
        )

        val expected = EnrichedProduct(
          notPlacement,
          ProductId.withAlias(notPlacement.name),
          isFreeByQuota = false,
          baseQuota = 0,
          None,
          limitReason = None,
          Nil,
          productPeriodicalDiscount = productPeriodicalDiscount,
          offerType = None
        )

        productEnricher
          .enrichProduct(notPlacement, context)
          .success
          .value shouldBe expected
      }
    }

    "enrich placement" in {
      forAll(
        enrichedPriceRequestContext(hasOffer = true, hasUser = true).map(
          _.copy(
            userPeriodicalDiscount = User.NoActiveDiscount,
            promocoderFeatures = Map.empty
          )
        ),
        EnrichedProductGen.map(
          _.copy(
            features = Nil,
            productPeriodicalDiscount = Product.NoActiveDiscount
          )
        )
      ) { (context, enrichedProduct) =>
        (productPlacementEnricher.enrichPlacement _)
          .expects(*, *)
          .returningZ(enrichedProduct)

        val features = context.promocoderFeatures.getOrElse(Placement, Nil)

        val productPeriodicalDiscount = ProductPeriodicalDiscount.forProduct(
          Placement,
          context.userPeriodicalDiscount
        )

        productEnricher
          .enrichProduct(Placement, context)
          .success
          .value shouldBe enrichedProduct.copy(
          features = features,
          productPeriodicalDiscount = productPeriodicalDiscount
        )
      }
    }

    "enrich placement with features" in {
      forAll(
        enrichedPriceRequestContext(hasOffer = true, hasUser = true)
          .map(_.copy(userPeriodicalDiscount = User.NoActiveDiscount)),
        EnrichedProductGen.map(
          _.copy(
            features = Nil,
            productPeriodicalDiscount = Product.NoActiveDiscount
          )
        )
      ) { (context, enrichedProduct) =>
        (productPlacementEnricher.enrichPlacement _)
          .expects(*, *)
          .returningZ(enrichedProduct)

        val features = context.promocoderFeatures.getOrElse(Placement, Nil)

        productEnricher
          .enrichProduct(Placement, context)
          .success
          .value shouldBe enrichedProduct.copy(features = features)
      }
    }

    "enrich with feature for offers-history-reports-1" in {
      forAll(featureInstanceGen) { feature =>
        forAll(
          enrichedPriceRequestContext(hasOffer = true, hasUser = true)
            .map(
              _.copy(promocoderFeatures = Map(OffersHistoryReports(1) -> List(feature)))
            )
        ) { context =>
          productEnricher
            .enrichProduct(OffersHistoryReports(1), context)
            .success
            .value
            .features shouldBe List(feature)
        }
      }
    }

    "enrich with feature for offers-history-reports-10" in {
      forAll(featureInstanceGen) { feature =>
        forAll(
          enrichedPriceRequestContext(hasOffer = true, hasUser = true)
            .map(
              _.copy(promocoderFeatures = Map(OffersHistoryReports(10) -> List(feature)))
            )
        ) { context =>
          productEnricher
            .enrichProduct(OffersHistoryReports(10), context)
            .success
            .value
            .features shouldBe List(feature)
        }
      }
    }

    "not enrich with feature for offers-history-reports-1" in {
      forAll(featureInstanceGen) { feature =>
        forAll(
          enrichedPriceRequestContext(hasOffer = true, hasUser = true)
            .map(
              _.copy(promocoderFeatures = Map(OffersHistoryReports(10) -> List(feature)))
            )
        ) { context =>
          productEnricher
            .enrichProduct(OffersHistoryReports(1), context)
            .success
            .value
            .features shouldBe Nil
        }
      }
    }

    "not enrich with feature for offers-history-reports-10" in {
      forAll(featureInstanceGen) { feature =>
        forAll(
          enrichedPriceRequestContext(hasOffer = true, hasUser = true)
            .map(
              _.copy(promocoderFeatures = Map(OffersHistoryReports(1) -> List(feature)))
            )
        ) { context =>
          productEnricher
            .enrichProduct(OffersHistoryReports(10), context)
            .success
            .value
            .features shouldBe Nil
        }
      }
    }

    "enrich placement with periodical discount" in {
      forAll(
        enrichedPriceRequestContext(hasOffer = true, hasUser = true)
          .map(_.copy(promocoderFeatures = Map.empty)),
        EnrichedProductGen.map(
          _.copy(
            features = Nil,
            productPeriodicalDiscount = Product.NoActiveDiscount
          )
        )
      ) { (context, enrichedProduct) =>
        (productPlacementEnricher.enrichPlacement _)
          .expects(*, *)
          .returningZ(enrichedProduct)

        val productPeriodicalDiscount = ProductPeriodicalDiscount.forProduct(
          Placement,
          context.userPeriodicalDiscount
        )

        val expected =
          enrichedProduct.copy(productPeriodicalDiscount = productPeriodicalDiscount)

        productEnricher
          .enrichProduct(Placement, context)
          .success
          .value shouldBe expected
      }
    }

  }

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
