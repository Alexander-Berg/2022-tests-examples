package ru.auto.salesman.service.impl.user.autoru.price.service

import org.scalacheck.Gen
import ru.auto.salesman.model.user.Prolongable
import ru.auto.salesman.model.{DeprecatedDomain, DeprecatedDomains}
import ru.auto.salesman.service.impl.user.ExperimentSelectServiceImpl
import ru.auto.salesman.service.user.autoru.price.service.ContextEnricher
import ru.auto.salesman.service.user.{
  ModifyPriceService,
  ProlongIntervalService,
  UserFeatureService
}
import ru.auto.salesman.service.{ProductDescriptionService, ProlongableExtractor}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.{
  BunkerModelGenerator,
  ServiceModelGenerators
}

class ProductPriceCalculatorSpec
    extends BaseSpec
    with ServiceModelGenerators
    with BunkerModelGenerator {

  implicit override def domain: DeprecatedDomain = DeprecatedDomains.AutoRu

  val prolongableExtractor: ProlongableExtractor = mock[ProlongableExtractor]

  val productDescriptionService: ProductDescriptionService =
    mock[ProductDescriptionService]
  val priceCalculator: PriceCalculator = mock[PriceCalculator]
  val productEnricher: ProductEnricher = mock[ProductEnricher]
  val contextEnricher: ContextEnricher = mock[ContextEnricher]
  val userFeatureService: UserFeatureService = mock[UserFeatureService]

  val prolongIntervalService: ProlongIntervalService =
    mock[ProlongIntervalService]

  val modifyPriceService: ModifyPriceService = mock[ModifyPriceService]

  val productDurationCalculator = new ProductDurationCalculatorImpl

  private val prolongationExtractor =
    new ProlongationExtractorImpl(prolongableExtractor)

  val experimentSelectServiceMock = new ExperimentSelectServiceImpl()

  private val paymentInputCalculator =
    new PaymentInputCalculatorImpl(
      prolongationExtractor,
      priceCalculator,
      modifyPriceService,
      productDurationCalculator,
      prolongIntervalService,
      experimentSelectServiceMock
    )

  private val productPriceInfoCalculator =
    new ProductPriceInfoCalculatorImpl(
      productDescriptionService,
      priceCalculator
    )

  private val productPriceCalculator =
    new ProductPriceCalculatorImpl(
      productEnricher,
      paymentInputCalculator,
      productPriceInfoCalculator
    )

  val enrichedOffer = EnrichedOfferGen.next
  "calculateProductPrice" should {

    "return duration from moisha (originalPrice)" in {
      forAll(productDurationGen) { duration =>
        forAll(
          EnrichedProductGen,
          ProductInfoGen.map(_.copy(duration = Some(duration))),
          patchedPriceGen(),
          ProductGen,
          EnrichedPriceRequestContextGen.map(
            _.copy(offer = Some(enrichedOffer))
          ),
          Gen.option(DescriptionGenerator)
        ) {
          (
              enrichedProduct,
              productInfo,
              patchedPrice,
              product,
              context,
              productDescription
          ) =>
            (productEnricher.enrichProduct _)
              .expects(*, *)
              .returningZ(enrichedProduct)
              .anyNumberOfTimes()

            (priceCalculator.calculateBasePrice _)
              .expects(*, context)
              .returningZ(productInfo)

            (priceCalculator.calculateAutoApplyPrice _)
              .expects(*, context)
              .returningZ(None)

            (prolongIntervalService.applyProlongInterval _)
              .expects(*, *, *, *)
              .returningZ(None)
              .anyNumberOfTimes()

            (prolongableExtractor.prolongationAllowed _)
              .expects(*, *, *, *)
              .returningZ(Prolongable(true))
            (prolongableExtractor.prolongationForced _)
              .expects(*, *)
              .returningZ(Prolongable(true))
            (prolongableExtractor.prolongationForcedNotTogglable _)
              .expects(*, *, *)
              .returningZ(Prolongable(true))

            (modifyPriceService.buildPatchedPrice _)
              .expects(*, *, *, *, *, *, *, *, *, *)
              .returningZ(patchedPrice)

            (productDescriptionService.userDescription _)
              .expects(*)
              .returningZ(productDescription)
              .anyNumberOfTimes()

            (productDescriptionService.offerDescription _)
              .expects(*, *)
              .returningZ(productDescription)
              .anyNumberOfTimes()

            val res = productPriceCalculator
              .calculateProductPrice(product, context)
              .success
              .value
            res.price.basePrice shouldBe patchedPrice.basePrice
            res.price.effectivePrice shouldBe patchedPrice.effectivePrice
            res.price.prolongPrice shouldBe productInfo.prolongPrice
            res.price.modifier shouldBe patchedPrice.modifier
            res.duration shouldBe productInfo.duration.get
        }
      }
    }
  }
}
