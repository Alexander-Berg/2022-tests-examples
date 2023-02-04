package ru.auto.api.managers.enrich.enrichers

import org.mockito.Mockito.{reset, verify, verifyNoMoreInteractions}
import org.mockito.ArgumentMatchers.argThat
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.auto.api.ApiOfferModel.{Offer, SellerType, SharkInfo}
import ru.auto.api.BaseSpec
import ru.auto.api.auth.Application
import ru.auto.api.extdata.DataService
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.enrich.EnrichOptions
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.{RequestParams, UserRef}
import ru.auto.api.model.shark._
import ru.auto.api.services.shark.SharkClient
import ru.auto.api.util.RequestImpl
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.shark.proto.Api.{CreditProductsRequest, CreditProductsResponse, PreconditionsRequest, PreconditionsResponse}
import ru.yandex.vertis.tracing.Traced
import ru.auto.api.model.bunker.shark.{CalculatorStatic, CalculatorStaticContainer, AmountRange => CalculatorAmountRange, InterestRateRange => CalculatorInterestRateRange, TermMonthsRange => CalculatorTermMonthsRange}
import ru.auto.api.CommonModel.{CreditConfiguration, PriceInfo}
import ru.auto.api.GeneratorUtils._
import ru.yandex.vertis.shark.proto.{AmountRange, CreditProduct, EcreditPrecondition, InterestRateRange, TermMonthsRange}

import scala.jdk.CollectionConverters._

class SharkInfoEnricherSpec extends BaseSpec with MockitoSupport with ScalaCheckPropertyChecks {

  import SharkInfoEnricherSpec._

  private val sharkClient = mock[SharkClient]
  private val featureManager = mock[FeatureManager]
  private val dataService = mock[DataService]

  private val allowSharkInfoEnrichment = mock[Feature[Boolean]]
  private val preconditionsFromSharkApi = mock[Feature[Boolean]]
  private val sharkInfoDefaultPrecondition = mock[Feature[Boolean]]

  when(allowSharkInfoEnrichment.value).thenReturn(false)
  when(featureManager.allowSharkInfoEnrichment).thenReturn(allowSharkInfoEnrichment)
  when(preconditionsFromSharkApi.value).thenReturn(false)
  when(featureManager.preconditionsFromSharkApi).thenReturn(preconditionsFromSharkApi)
  when(sharkInfoDefaultPrecondition.value).thenReturn(false)
  when(featureManager.sharkInfoDefaultPrecondition).thenReturn(sharkInfoDefaultPrecondition)

  when(dataService.calculatorStatic).thenReturn(CalculatorStaticContainer(Some(calculatorStatic)))
  when(sharkClient.preconditions(?)(?)).thenReturnF(preconditionsResponse)
  when(sharkClient.creditProductList(?)(?)).thenReturnF(creditProductsResponse)

  private val enricher = new SharkInfoEnricher(sharkClient, featureManager, dataService)

  private def enrich(offer: Offer, options: EnrichOptions = DefaultEnrichOptions.withSharkInfo): Offer =
    enricher.getFunction(Seq(offer), options).futureValue.apply(offer)

  "SharkInfoEnricher" should {

    "not enrich when featureManager.allowSharkInfoEnrichment is false" in {

      val enriched = enrich(offer)

      enriched.hasSharkInfo shouldBe false

      verifyNoMoreInteractions(dataService)
      verifyNoMoreInteractions(sharkClient)
    }

    "not enrich when options.sharkInfo is false" in {
      when(allowSharkInfoEnrichment.value).thenReturn(true)

      val enriched = enrich(offer, DefaultEnrichOptions)

      enriched.hasSharkInfo shouldBe false

      verifyNoMoreInteractions(dataService)
      verifyNoMoreInteractions(sharkClient)
    }

    "enrich when offer has EcreditPrecondition" in {
      when(allowSharkInfoEnrichment.value).thenReturn(true)

      val enriched = enrich(offer.withEcreditPrecondition)

      enriched.hasSharkInfo shouldBe true
      enriched.getSharkInfo shouldBe sharkInfo.forEcreditPrecondition

      verifyNoMoreInteractions(dataService)
    }

    "enrich when offer has empty EcreditPrecondition & has DealerCreditConfig" in {
      when(allowSharkInfoEnrichment.value).thenReturn(true)

      val enriched = enrich(offer.withEmptyEcreditPrecondition.withDealerCreditConfig)

      enriched.hasSharkInfo shouldBe true
      enriched.getSharkInfo shouldBe sharkInfo.forDealerCreditConfig

      verifyNoMoreInteractions(dataService)
    }

    "enrich when offer has DealerCreditConfig" in {
      when(allowSharkInfoEnrichment.value).thenReturn(true)

      val enriched = enrich(offer.withDealerCreditConfig)

      enriched.hasSharkInfo shouldBe true
      enriched.getSharkInfo shouldBe sharkInfo.forDealerCreditConfig

      verifyNoMoreInteractions(dataService)
    }

    "enrich when offer has empty DealerCreditConfig & has EcreditPrecondition" in {
      when(allowSharkInfoEnrichment.value).thenReturn(true)

      val enriched = enrich(offer.withEmptyDealerCreditConfig.withEcreditPrecondition)

      enriched.hasSharkInfo shouldBe true
      enriched.getSharkInfo shouldBe sharkInfo.forEcreditPrecondition

      verifyNoMoreInteractions(dataService)
    }

    "not enrich when offer hasn't allowed_for_credit tag" in {
      when(allowSharkInfoEnrichment.value).thenReturn(true)

      val enriched = enrich(offer.withoutAllowedForCreditTag)

      enriched.hasSharkInfo shouldBe false

      verifyNoMoreInteractions(dataService)
    }

    "enrich when featureManager.preconditionsFromSharkApi is false" in {
      when(allowSharkInfoEnrichment.value).thenReturn(true)
      when(preconditionsFromSharkApi.value).thenReturn(false)
      when(sharkInfoDefaultPrecondition.value).thenReturn(false)

      val enriched = enrich(offer)

      enriched.hasSharkInfo shouldBe true
      enriched.getSharkInfo shouldBe sharkInfo.forPublicApi
    }

    "enrich when featureManager.preconditionsFromSharkApi is true" in {
      when(allowSharkInfoEnrichment.value).thenReturn(true)
      when(preconditionsFromSharkApi.value).thenReturn(true)
      when(sharkInfoDefaultPrecondition.value).thenReturn(false)

      val enriched = enrich(offer)

      enriched.hasSharkInfo shouldBe true
      enriched.getSharkInfo shouldBe sharkInfo.forSharkApi

      verify(sharkClient).preconditions(
        argThat[PreconditionsRequest] { request =>
          val auto = request.getObjectInfo(0).getObjectPayload.getAuto
          auto.getOfferSellerType == offer.getSellerType.toAutoSellerType &&
          auto.getCategory == offer.getCategory &&
          auto.getSection == offer.getSection &&
          auto.getUserRef == offer.getUserRef
        }
      )(?)
    }

    "enrich when featureManager.sharkInfoDefaultPrecondition is true" in {
      when(allowSharkInfoEnrichment.value).thenReturn(true)
      when(preconditionsFromSharkApi.value).thenReturn(true)
      when(sharkInfoDefaultPrecondition.value).thenReturn(true)

      val enriched = enrich(offer)

      enriched.hasSharkInfo shouldBe true
      enriched.getSharkInfo shouldBe sharkInfo.forCalculatorStatic
    }

    "enrich with default geobases" in {
      reset(sharkClient)
      when(sharkClient.creditProductList(?)(?)).thenReturnF(creditProductsResponse)
      when(allowSharkInfoEnrichment.value).thenReturn(true)

      enrich(offer)

      verify(sharkClient).creditProductList(
        argThat[CreditProductsRequest] { request =>
          request.getByGeo.getGeobaseIdsList.asScala == DefaultGeobaseIds
        }
      )(?)
    }

    "enrich with geobases from options" in {
      reset(sharkClient)
      when(sharkClient.creditProductList(?)(?)).thenReturnF(creditProductsResponse)
      when(allowSharkInfoEnrichment.value).thenReturn(true)

      enrich(offer, DefaultEnrichOptions.withSharkInfo.withGeobaseIds)

      verify(sharkClient).creditProductList(
        argThat[CreditProductsRequest] { request =>
          request.getByGeo.getGeobaseIdsList.asScala == EnrichOptionsGeobaseIds
        }
      )(?)
    }
  }
}

object SharkInfoEnricherSpec {

  private[this] val AllowedForCreditTag: String = "allowed_for_credit"

  private[this] val DefaultUserRef: UserRef = UserRef.parse("user:123")

  private[this] val DefaultCreditProductId: String = "credit-product-1"
  private[this] val DefaultOfferPrice: Float = 1200000f
  private[this] val DefaultAmountRangeFrom: Long = 10000L
  private[this] val DefaultAmountRangeTo: Long = 2000000L
  private[this] val DefaultEcreditPreconditionInterestRateRangeFrom: Float = 1f
  private[this] val DefaultDealerCreditConfigInterestRateRangeFrom: Float = 2f
  private[this] val DefaultCalculatorStaticInterestRateRangeFrom: Float = 3f
  private[this] val DefaultSharkApiInterestRateRangeFrom: Float = 4f
  private[this] val DefaultPublicApiInterestRateRangeFrom: Float = 5f
  private[this] val DefaultInterestRateRangeTo: Float = 10f
  private[this] val DefaultTermMonthsRangeFrom: Int = 12
  private[this] val DefaultTermMonthsRangeTo: Int = 36
  private[this] val DefaultMinInitialFeeRate: Float = 10f
  private[this] val DefaultInitialFee: Long = 120000L
  private[this] val DefaultEcreditPreconditionMonthlyPayment: Long = 30463L
  private[this] val DefaultDealerCreditConfigMonthlyPayment: Long = 30934L
  private[this] val DefaultCalculatorStaticMonthlyPayment: Long = 31407L
  private[this] val DefaultSharkApiMonthlyPayment: Long = 31889L
  private[this] val DefaultPublicApiMonthlyPayment: Long = 32368L

  private val DefaultGeobaseIds: Seq[GeobaseId] = Seq(225L)
  private val EnrichOptionsGeobaseIds: Seq[GeobaseId] = Seq(213L, 36L)
  private val DefaultEnrichOptions: EnrichOptions = EnrichOptions()

  private val offer: Offer =
    offerGen(PrivateUserRefGen).next.toBuilder
      .setPriceInfo(PriceInfo.newBuilder.setPrice(DefaultOfferPrice))
      .addTags(AllowedForCreditTag)
      .build

  implicit private val userRequest: RequestImpl = {
    val req = new RequestImpl
    req.setApplication(Application.desktop)
    req.setTrace(Traced.empty)
    req.setRequestParams(RequestParams.empty)
    req.setUser(DefaultUserRef)
    req
  }

  private val calculatorStatic: CalculatorStatic =
    CalculatorStatic(
      amountRange = CalculatorAmountRange(DefaultAmountRangeFrom, DefaultAmountRangeTo),
      interestRateRange =
        CalculatorInterestRateRange(DefaultCalculatorStaticInterestRateRangeFrom, DefaultInterestRateRangeTo),
      termMonthsRange = CalculatorTermMonthsRange(DefaultTermMonthsRangeFrom, DefaultTermMonthsRangeTo),
      minInitialFeeRate = DefaultMinInitialFeeRate
    )

  private val preconditionsResponse: PreconditionsResponse =
    PreconditionsResponse.newBuilder
      .addPreconditions(
        PreconditionsResponse.Precondition.newBuilder
          .setObjectId(offer.getId)
          .addProductPreconditions(
            PreconditionsResponse.Precondition.ProductPrecondition.newBuilder
              .setProductId(DefaultCreditProductId)
              .setInitialFee(DefaultInitialFee)
              .setInterestRate(DefaultSharkApiInterestRateRangeFrom)
              .setTermsMonths(DefaultTermMonthsRangeTo)
              .setMonthlyPayment(DefaultSharkApiMonthlyPayment)
              .build
          )
          .build
      )
      .build

  private val creditProduct: CreditProduct =
    CreditProduct.newBuilder
      .setId(DefaultCreditProductId)
      .setAmountRange(
        AmountRange.newBuilder
          .setFrom(DefaultAmountRangeFrom)
          .setTo(DefaultAmountRangeTo)
          .build
      )
      .setInterestRateRange(
        InterestRateRange.newBuilder
          .setFrom(DefaultPublicApiInterestRateRangeFrom)
          .setTo(DefaultInterestRateRangeTo)
          .build
      )
      .setTermMonthsRange(
        TermMonthsRange.newBuilder
          .setFrom(DefaultTermMonthsRangeFrom)
          .setTo(DefaultTermMonthsRangeTo)
          .build
      )
      .setMinInitialFeeRate(DefaultMinInitialFeeRate)
      .build

  private val creditProductsResponse: CreditProductsResponse =
    CreditProductsResponse.newBuilder
      .addCreditProducts(creditProduct)
      .build

  private val sharkInfo: SharkInfo =
    SharkInfo.newBuilder
      .addSuitableCreditProductIds(DefaultCreditProductId)
      .setPrecondition(
        SharkInfo.Precondition.newBuilder
          .setInitialFee(DefaultInitialFee)
          .setTermsMonths(DefaultTermMonthsRangeTo)
          .build
      )
      .build

  implicit private class RichOffer(val value: Offer) extends AnyVal {

    def withEmptyEcreditPrecondition: Offer =
      value.toBuilder
        .setSellerType(SellerType.COMMERCIAL)
        .setEcreditPrecondition(
          EcreditPrecondition.getDefaultInstance.toBuilder
            .setAmountRange(AmountRange.getDefaultInstance)
            .setTermMonthsRange(TermMonthsRange.getDefaultInstance)
            .build
        )
        .build

    def withEcreditPrecondition: Offer =
      value.toBuilder
        .setSellerType(SellerType.COMMERCIAL)
        .setEcreditPrecondition(
          EcreditPrecondition.newBuilder
            .setAmountRange(
              AmountRange.newBuilder
                .setFrom(DefaultAmountRangeFrom)
                .setTo(DefaultAmountRangeTo)
                .build
            )
            .setInterestRate(DefaultEcreditPreconditionInterestRateRangeFrom)
            .setTermMonthsRange(
              TermMonthsRange.newBuilder
                .setFrom(DefaultTermMonthsRangeFrom)
                .setTo(DefaultTermMonthsRangeTo)
                .build
            )
            .setMinInitialFeeRate(DefaultMinInitialFeeRate)
        )
        .build

    def withEmptyDealerCreditConfig: Offer =
      value.toBuilder
        .setSellerType(SellerType.COMMERCIAL)
        .setDealerCreditConfig(CreditConfiguration.getDefaultInstance)
        .build

    def withDealerCreditConfig: Offer =
      value.toBuilder
        .setSellerType(SellerType.COMMERCIAL)
        .setDealerCreditConfig(
          CreditConfiguration.newBuilder
            .setCreditMinAmount(DefaultAmountRangeFrom.toInt)
            .setCreditMaxAmount(DefaultAmountRangeTo.toInt)
            .setCreditMinRate(DefaultDealerCreditConfigInterestRateRangeFrom.toDouble)
            .addCreditTermValues(DefaultTermMonthsRangeFrom / 12)
            .addCreditTermValues(DefaultTermMonthsRangeTo / 12)
            .setCreditOfferInitialPaymentRate(DefaultMinInitialFeeRate.toDouble)
            .build
        )
        .build

    def withoutAllowedForCreditTag: Offer = value.toBuilder.clearTags.build
  }

  implicit private class RichSharkInfo(val value: SharkInfo) extends AnyVal {

    private def update(updatePreconditionFunc: SharkInfo.Precondition => SharkInfo.Precondition): SharkInfo =
      value.toBuilder.setPrecondition(updatePreconditionFunc(value.getPrecondition)).build

    def forEcreditPrecondition: SharkInfo = update { precondition =>
      precondition.toBuilder
        .setInterestRate(DefaultEcreditPreconditionInterestRateRangeFrom)
        .setMonthlyPayment(DefaultEcreditPreconditionMonthlyPayment)
        .build
    }

    def forDealerCreditConfig: SharkInfo = update { precondition =>
      precondition.toBuilder
        .setInterestRate(DefaultDealerCreditConfigInterestRateRangeFrom)
        .setMonthlyPayment(DefaultDealerCreditConfigMonthlyPayment)
        .build
    }

    def forCalculatorStatic: SharkInfo = update { precondition =>
      precondition.toBuilder
        .setInterestRate(DefaultCalculatorStaticInterestRateRangeFrom)
        .setMonthlyPayment(DefaultCalculatorStaticMonthlyPayment)
        .build
    }

    def forSharkApi: SharkInfo = update { precondition =>
      precondition.toBuilder
        .setInterestRate(DefaultSharkApiInterestRateRangeFrom)
        .setMonthlyPayment(DefaultSharkApiMonthlyPayment)
        .build
    }

    def forPublicApi: SharkInfo = update { precondition =>
      precondition.toBuilder
        .setInterestRate(DefaultPublicApiInterestRateRangeFrom)
        .setMonthlyPayment(DefaultPublicApiMonthlyPayment)
        .build
    }
  }

  implicit private class RichEnrichOptions(val value: EnrichOptions) extends AnyVal {

    def withSharkInfo: EnrichOptions = value.copy(sharkInfo = true)

    def withGeobaseIds: EnrichOptions =
      value.copy(
        offerCardAdditionalParams = value.offerCardAdditionalParams.copy(
          geoIds = EnrichOptionsGeobaseIds.map(_.toInt).toList
        )
      )
  }
}
