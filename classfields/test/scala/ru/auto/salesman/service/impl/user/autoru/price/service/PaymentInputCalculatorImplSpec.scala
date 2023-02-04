package ru.auto.salesman.service.impl.user.autoru.price.service

import org.joda.time.Days
import ru.auto.salesman.model.user.periodical_discount_exclusion.Product.ProductPeriodicalDiscount
import ru.auto.salesman.model.user.product.AutoruProduct
import ru.auto.salesman.model.user.product.ProductProvider.AutoruGoods.Placement
import ru.auto.salesman.model.user.product.ProductProvider.AutoruSubscriptions.OffersHistoryReports
import ru.auto.salesman.model.user.{
  ExperimentInfo,
  Experiments,
  PriceModifier,
  ProlongIntervalInfo,
  Prolongable,
  UserTariff
}
import ru.auto.salesman.model.{
  DeprecatedDomain,
  DeprecatedDomains,
  ExperimentId,
  FeatureInstance,
  Funds,
  ProductDuration,
  ProductId,
  RegionId
}
import ru.auto.salesman.service.PriceExtractor.ProductInfo
import ru.auto.salesman.service.user.ModifyPriceService.PatchedPrice
import ru.auto.salesman.service.user.PriceService.{
  EnrichedPriceRequestContext,
  EnrichedProduct,
  MoneyFeatureInstance,
  Prolongation
}
import ru.auto.salesman.service.user.autoru.price.service.{
  ProductDurationCalculator,
  ProlongationExtractor
}
import ru.auto.salesman.service.user.{PriceService, _}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.model.gens.user.ServiceModelGenerators
import ru.auto.salesman.util.money.Money.Kopecks

class PaymentInputCalculatorImplSpec extends BaseSpec with ServiceModelGenerators {

  private val prolongationExtractorMock = mock[ProlongationExtractor]
  private val priceCalculatorMock = mock[PriceCalculator]
  private val modifyPriceServiceMock = mock[ModifyPriceService]
  private val productDurationCalculatorMock = mock[ProductDurationCalculator]
  private val prolongIntervalServiceMock = mock[ProlongIntervalService]
  private val experimentSelectServiceMock = mock[ExperimentSelectService]

  val paymentInputCalculator = new PaymentInputCalculatorImpl(
    prolongationExtractor = prolongationExtractorMock,
    priceCalculator = priceCalculatorMock,
    modifyPriceService = modifyPriceServiceMock,
    productDurationCalculator = productDurationCalculatorMock,
    prolongIntervalService = prolongIntervalServiceMock,
    experimentSelectService = experimentSelectServiceMock
  )

  "PaymentInputCalculatorImpl" should {
    "check select valid experiment from geoId offer if productId == placement" in {
      forAll(
        enrichedProduct(Placement),
        EnrichedOfferGen.next,
        EnrichedPriceRequestContextGen,
        ExperimentInfoGen
      ) { (enrichedProduct, offer, enrichedPriceRequestContext, experimentTest) =>
        val enrichedPriceRequestContextWithOffer = enrichedPriceRequestContext
          .copy(offer = Some(offer))

        val productInfo = ProductInfo(
          product = ProductId.Placement,
          price = Kopecks(1000),
          prolongPrice = Some(100),
          duration = None,
          tariff = None,
          appliedExperiment = experimentTest.activeExperimentId,
          policyId = None
        )
        mockPriceCalculator(productInfo)
        mockProductDurationCalculator()
        mockProlongIntervalService()
        mockExperimentServiceForOffer(offer, experimentTest)
        mockModifyPrice(Some(experimentTest))
        mockProlongationExtraction()

        val res1 = paymentInputCalculator
          .calculatePaymentInput(
            enrichedProduct = enrichedProduct,
            context = enrichedPriceRequestContextWithOffer
          )
        val res = res1.success
        res.value.price.modifier.get.experimentInfo shouldBe Some(
          experimentTest
        )
      }
    }

    "check select valid experiment from geoId offer if productId == OffersHistoryReports" in {
      forAll(
        enrichedProduct(OffersHistoryReports(1)),
        EnrichedOfferGen.next,
        EnrichedPriceRequestContextGen,
        ExperimentInfoGen
      ) { (enrichedProduct, offer, enrichedPriceRequestContext, experimentTest) =>
        val enrichedPriceRequestContextWithOffer = enrichedPriceRequestContext
          .copy(offer = Some(offer))

        val productInfo = ProductInfo(
          product = ProductId.VinHistory,
          price = Kopecks(1000),
          prolongPrice = Some(100),
          duration = None,
          tariff = None,
          appliedExperiment = experimentTest.activeExperimentId,
          policyId = None
        )
        mockPriceCalculator(productInfo)
        mockProductDurationCalculator()
        mockProlongIntervalService()
        mockExperimentServiceForOfferHistory(experimentTest)
        mockModifyPrice(Some(experimentTest))
        mockProlongationExtraction()

        val res = paymentInputCalculator
          .calculatePaymentInput(
            enrichedProduct = enrichedProduct,
            context = enrichedPriceRequestContextWithOffer
          )
          .success
        res.value.price.modifier.get.experimentInfo shouldBe Some(
          experimentTest
        )
      }
    }
  }

  private def mockPriceCalculator(result: ProductInfo): Unit =
    (priceCalculatorMock
      .calculateBasePrice(
        _: EnrichedProduct,
        _: EnrichedPriceRequestContext
      ))
      .expects(*, *)
      .returningZ(result)

  private def mockProductDurationCalculator(): Unit =
    (productDurationCalculatorMock
      .getProductDuration(
        _: Option[ProductDuration],
        _: Option[ProductDuration],
        _: ProductDuration
      ))
      .expects(*, *, *)
      .returningZ(ProductDuration(Days.days(4)))

  private def mockProlongIntervalService(): Unit = (
    prolongIntervalServiceMock
      .applyProlongInterval(
        _: ProductInfo,
        _: ProductDuration,
        _: EnrichedProduct,
        _: EnrichedPriceRequestContext
      )
    )
    .expects(*, *, *, *)
    .returningZ(None)

  private def mockExperimentServiceForOffer(
      offer: PriceService.EnrichedOffer,
      resultExperiment: ExperimentInfo
  ): Unit =
    (experimentSelectServiceMock
      .getExperimentForOffer(
        _: Option[Experiments],
        _: AutoruProduct,
        _: Option[PriceService.EnrichedOffer]
      ))
      .expects(*, *, Some(offer))
      .returningZ(Some(resultExperiment))

  private def mockExperimentServiceForOfferHistory(
      resultExperiment: ExperimentInfo
  ): Unit =
    (experimentSelectServiceMock
      .getExperiment(
        _: Option[Experiments],
        _: AutoruProduct,
        _: Set[RegionId]
      ))
      .expects(*, *, *)
      .returningZ(Some(resultExperiment))

  private def mockModifyPrice(experimentInfo: Option[ExperimentInfo]): Unit = (
    modifyPriceServiceMock
      .buildPatchedPrice(
        _: Boolean,
        _: Funds,
        _: Option[FeatureInstance],
        _: List[FeatureInstance],
        _: ProductPeriodicalDiscount,
        _: Option[ExperimentInfo],
        _: Option[ExperimentId],
        _: Boolean,
        _: List[MoneyFeatureInstance],
        _: Option[ProlongIntervalInfo]
      )
    )
    .expects(
      *,
      *,
      *,
      *,
      *,
      experimentInfo,
      experimentInfo.flatMap(_.activeExperimentId),
      *,
      *,
      *
    )
    .returningZ(
      PatchedPrice(
        basePrice = 1000L,
        effectivePrice = 500L,
        modifier = Some(
          PriceModifier(
            feature = None,
            bundleId = None,
            experimentInfo = experimentInfo,
            appliedExperimentId = experimentInfo.flatMap(_.activeExperimentId),
            periodicalDiscount = None,
            prolongInterval = None,
            userQuotaChanged = None
          )
        ),
        periodicalDiscountExclusion = None
      )
    )

  private def mockProlongationExtraction(): Unit =
    (prolongationExtractorMock
      .extractProlongation(
        _: AutoruProduct,
        _: Option[UserTariff],
        _: EnrichedPriceRequestContext
      ))
      .expects(*, *, *)
      .returningZ(
        Prolongation(
          prolongationAllowed = Prolongable(true),
          prolongationForced = Prolongable(true),
          prolongationForcedNotTogglable = Prolongable(true)
        )
      )
  implicit def domain: DeprecatedDomain = DeprecatedDomains.AutoRu
}
