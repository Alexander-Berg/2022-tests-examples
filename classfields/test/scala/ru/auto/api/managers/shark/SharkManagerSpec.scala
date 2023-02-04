package ru.auto.api.managers.shark

import cats.implicits.catsSyntaxOptionId
import org.mockito.Mockito.reset
import ru.auto.api.ApiOfferModel.OfferStatus
import ru.auto.api.{ApiOfferModel, BaseSpec}
import ru.auto.api.extdata.DataService
import ru.auto.api.features.FeatureManager
import ru.auto.api.managers.offers.EnrichedOfferLoader
import ru.auto.api.model.ModelGenerators._
import ru.auto.api.model.bunker.shark.{AmountRange, CalculatorStatic, CalculatorStaticContainer, InterestRateRange => IntInterestRateRange, TermMonthsRange => IntTermMonthsRange}
import ru.auto.api.services.settings.SettingsClient
import ru.auto.api.services.shark.SharkClient
import ru.auto.api.shark.SharkModel.CalculatorResponse
import ru.auto.api.util.RequestImpl
import ru.yandex.vertis.feature.model.Feature
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.shark.proto._
import ru.yandex.vertis.tracing.Traced

class SharkManagerSpec extends BaseSpec with MockitoSupport {

  private val sharkClient = mock[SharkClient]
  private val offerLoader = mock[EnrichedOfferLoader]
  private val settingsClient = mock[SettingsClient]
  private val decayManager = mock[SharkDecayManager]
  private val featureManager = mock[FeatureManager]
  private val dataService = mock[DataService]

  implicit val trace: Traced = Traced.empty
  implicit val request: RequestImpl = new RequestImpl

  private val manager =
    new SharkManager(sharkClient, offerLoader, settingsClient, decayManager, featureManager, dataService)

  private val calculatorStatic = CalculatorStatic(
    amountRange = AmountRange(100000L, 1000000L),
    interestRateRange = IntInterestRateRange(5.0f, 15.0f),
    termMonthsRange = IntTermMonthsRange(12, 36),
    minInitialFeeRate = 5.0f
  )

  after {
    reset(sharkClient)
    reset(offerLoader)
    reset(settingsClient)
    reset(decayManager)
    reset(featureManager)
    reset(dataService)
  }

  "SharkManager.creditProductCalculator" should {
    "by static config" in {
      when(dataService.calculatorStatic).thenReturn(CalculatorStaticContainer(calculatorStatic.some))
      val feature = mock[Feature[Boolean]]
      when(feature.value).thenReturn(true)
      when(featureManager.sharkCalculatorStatic).thenReturn(feature)

      val creditProductsRequest = {
        val builder = Api.CreditProductsRequest.newBuilder
        builder.getAllBuilder
        builder.build
      }
      val result = manager.creditProductCalculator(creditProductsRequest).await
      val expected = {
        val builder = CalculatorResponse.newBuilder
          .setMinInitialFeeRate(5.0f)
        builder.getAmountRangeBuilder
          .setFrom(100000L)
          .setTo(1000000L)
        builder.getInterestRateRangeBuilder
          .setFrom(5.0f)
          .setTo(15.0f)
        builder.getTermMonthsRangeBuilder
          .setFrom(12)
          .setTo(36)
        builder.build
      }
      result shouldBe expected
    }
    "by dealer offer with ecredit precondition" in {
      val creditApplication = {
        val builder = CreditApplication.newBuilder
        val offer = CreditApplication.Payload.Autoru.Offer.newBuilder
          .setSection(ApiOfferModel.Section.USED)
          .setCategory(ApiOfferModel.Category.CARS)
          .setId("123-hash")
        builder.getPayloadBuilder.getAutoruBuilder.addOffers(offer)
        builder.build
      }
      val creditApplicationResponse =
        Api.CreditApplicationResponse.newBuilder.setCreditApplication(creditApplication).build

      val offer = {
        val builder = DealerOfferGen.next.toBuilder
        builder.setStatus(OfferStatus.ACTIVE)
        val epb = builder.getEcreditPreconditionBuilder
          .setMinInitialFeeRate(20.0f)
          .setInterestRate(30.0f)
        epb.getAmountRangeBuilder
          .setFrom(50000L)
          .setTo(3000000L)
        epb.getTermMonthsRangeBuilder
          .setFrom(12)
          .setTo(80)
        builder.build
      }

      when(dataService.calculatorStatic).thenReturn(CalculatorStaticContainer(calculatorStatic.some))
      val feature = mock[Feature[Boolean]]
      when(feature.value).thenReturn(true)
      when(featureManager.sharkCalculatorStatic).thenReturn(feature)
      when(sharkClient.creditApplicationGet(?, ?)(?)).thenReturnF(creditApplicationResponse)
      when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)

      val creditProductsRequest = {
        val builder = Api.CreditProductsRequest.newBuilder
        builder.getByCreditApplicationBuilder.setCreditApplicationId("some-ca-id")
        builder.build
      }

      val result = manager.creditProductCalculator(creditProductsRequest).await

      val expected = {
        val builder = CalculatorResponse.newBuilder
          .setMinInitialFeeRate(20.0f)
        builder.getAmountRangeBuilder
          .setFrom(50000L)
          .setTo(3000000L)
        builder.getInterestRateRangeBuilder
          .setFrom(30.0f)
          .setTo(30.0f)
        builder.getTermMonthsRangeBuilder
          .setFrom(12)
          .setTo(80)
        builder.build
      }
      result shouldBe expected
    }
    "by dealer offer with empty ecredit precondition of inactive offer" in {
      val creditApplication = {
        val builder = CreditApplication.newBuilder
        val offer = CreditApplication.Payload.Autoru.Offer.newBuilder
          .setSection(ApiOfferModel.Section.USED)
          .setCategory(ApiOfferModel.Category.CARS)
          .setId("123-hash")
        builder.getPayloadBuilder.getAutoruBuilder.addOffers(offer)
        builder.build
      }
      val creditApplicationResponse =
        Api.CreditApplicationResponse.newBuilder.setCreditApplication(creditApplication).build

      val offer = {
        val builder = DealerOfferGen.next.toBuilder
        builder.getEcreditPreconditionBuilder
          .setMinInitialFeeRate(0f)
          .setInterestRate(0f)
        builder.setStatus(OfferStatus.INACTIVE)
        builder.build
      }

      when(dataService.calculatorStatic).thenReturn(CalculatorStaticContainer(calculatorStatic.some))
      val feature = mock[Feature[Boolean]]
      when(feature.value).thenReturn(true)
      when(featureManager.sharkCalculatorStatic).thenReturn(feature)
      when(sharkClient.creditApplicationGet(?, ?)(?)).thenReturnF(creditApplicationResponse)
      when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)

      val creditProductsRequest = {
        val builder = Api.CreditProductsRequest.newBuilder
        builder.getByCreditApplicationBuilder.setCreditApplicationId("some-ca-id")
        builder.build
      }

      val result = manager.creditProductCalculator(creditProductsRequest).await

      val expected = {
        val builder = CalculatorResponse.newBuilder
          .setMinInitialFeeRate(5.0f)
        builder.getAmountRangeBuilder
          .setFrom(100000L)
          .setTo(1000000L)
        builder.getInterestRateRangeBuilder
          .setFrom(5.0f)
          .setTo(15.0f)
        builder.getTermMonthsRangeBuilder
          .setFrom(12)
          .setTo(36)
        builder.build
      }
      result shouldBe expected
    }
    "by dealer offer with empty ecredit precondition of active offer" in {
      val creditApplication = {
        val builder = CreditApplication.newBuilder
        val offer = CreditApplication.Payload.Autoru.Offer.newBuilder
          .setSection(ApiOfferModel.Section.USED)
          .setCategory(ApiOfferModel.Category.CARS)
          .setId("123-hash")
        builder.getPayloadBuilder.getAutoruBuilder.addOffers(offer)
        builder.build
      }
      val creditApplicationResponse =
        Api.CreditApplicationResponse.newBuilder.setCreditApplication(creditApplication).build

      val offer = {
        val builder = DealerOfferGen.next.toBuilder
        builder.getEcreditPreconditionBuilder
          .setMinInitialFeeRate(0f)
          .setInterestRate(0f)
        builder.setStatus(OfferStatus.ACTIVE)
        builder.build
      }

      when(dataService.calculatorStatic).thenReturn(CalculatorStaticContainer(calculatorStatic.some))
      val feature = mock[Feature[Boolean]]
      when(feature.value).thenReturn(true)
      when(featureManager.sharkCalculatorStatic).thenReturn(feature)
      when(sharkClient.creditApplicationGet(?, ?)(?)).thenReturnF(creditApplicationResponse)
      when(offerLoader.getOffer(?, ?, ?, ?, ?, ?)(?)).thenReturnF(offer)

      val creditProductsRequest = {
        val builder = Api.CreditProductsRequest.newBuilder
        builder.getByCreditApplicationBuilder.setCreditApplicationId("some-ca-id")
        builder.build
      }

      val result = manager.creditProductCalculator(creditProductsRequest).await

      val expected = {
        val builder = CalculatorResponse.newBuilder
          .setMinInitialFeeRate(5.0f)
        builder.getAmountRangeBuilder
          .setFrom(100000L)
          .setTo(1000000L)
        builder.getInterestRateRangeBuilder
          .setFrom(5.0f)
          .setTo(15.0f)
        builder.getTermMonthsRangeBuilder
          .setFrom(12)
          .setTo(36)
        builder.build
      }
      result shouldBe expected
    }
  }
}
