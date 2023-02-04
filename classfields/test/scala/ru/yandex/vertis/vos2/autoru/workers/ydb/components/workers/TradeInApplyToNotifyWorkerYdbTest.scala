package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.mockito.Mockito.verify
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import ru.auto.api.ApiOfferModel
import ru.auto.api.ApiOfferModel.{TradeInInfo, TradeInType}
import ru.yandex.vertis.feature.impl.BasicFeatureTypes.BooleanFeatureType
import ru.yandex.vertis.feature.model.FeatureRegistry
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.worker.{YdbShouldProcessResult, YdbWorkerResult}
import ru.yandex.vos2.OfferModel.{Offer => VosOffer}
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.dao.proxy.{AdditionalDataForReading, AdditionalDataLoader}
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.autoru.services.trade_in_notifier.TradeInNotifierClient
import ru.yandex.vos2.autoru.utils.converters.offerform.OfferFormConverter
import ru.yandex.vos2.commonfeatures.{FeatureRegistryFactory, FeaturesManager}

import scala.util.Success

class TradeInApplyToNotifyWorkerYdbTest
  extends AnyWordSpec
  with MockitoSupport
  with InitTestDbs
  with Matchers
  with BeforeAndAfterEach
  with BeforeAndAfterAll {

  implicit val traced: Traced = Traced.empty

  val featureRegistry: FeatureRegistry = FeatureRegistryFactory.inMemory()
  val featuresManager: FeaturesManager = new FeaturesManager(featureRegistry)

  featureRegistry.updateFeature(featuresManager.TradeInApplyToNotifyYdb.name, true)

  val offerFormConverter: OfferFormConverter = new OfferFormConverter(
    components.mdsPhotoUtils,
    components.regionTree,
    components.mdsPanoramasUtils,
    components.offerValidator,
    components.salonConverter,
    components.currencyRates,
    components.featuresManager,
    components.banReasons,
    components.carsCatalog,
    components.trucksCatalog,
    components.motoCatalog
  )

  val additionalDataLoader = new AdditionalDataLoader(
    components.autoruSalonsDao,
    components.autoruUsersDao,
    components.userDao,
    components.passportClient,
    components.featuresManager
  )

  override protected def beforeEach(): Unit = {
    featureRegistry.updateFeature(featuresManager.TradeInApplyToNotifyYdb.name, false)
  }

  "Trade in apply to notifier worker" should {
    "put offer into grpc TradeInNotifierService in TradeInApplyRequest" in new Worker {
      import TradeInApplyToNotifyWorkerYdb._

      featureRegistry.updateFeature(featuresManager.TradeInApplyToNotifyYdb.name, true)

      override val offer: VosOffer =
        TestUtils
          .createOffer(now = System.currentTimeMillis(), withEquipment = true, withDescription = true)
          .build()

      when(tradeInNotificationClient.tradeInApply(?)(?)).thenReturn(Success(()))

      val processResult: YdbWorkerResult = worker.process(offer, None)

      val ad: AdditionalDataForReading = additionalDataLoader.loadAdditionalDataForReading(offer)

      val apiOffer: ApiOfferModel.Offer = offerFormConverter.convert(ad, offer)

      val expectedState: String = LetterState(wasSent = true).toJsonString

      processResult.nextState shouldBe Some(expectedState)

      verify(tradeInNotificationClient).tradeInApply(apiOffer)
    }

    "block further satisfaction shouldProcess" in new Worker {
      import TradeInApplyToNotifyWorkerYdb._

      featureRegistry.updateFeature(featuresManager.TradeInApplyToNotifyYdb.name, true)

      override val offer: VosOffer = {
        val offerBuilder =
          TestUtils.createOffer(now = System.currentTimeMillis(), withEquipment = true, withDescription = true)

        offerBuilder.getOfferAutoruBuilder.setTradeInInfo(
          TradeInInfo.newBuilder().setTradeInType(TradeInType.FOR_MONEY).build()
        )

        offerBuilder.build()
      }

      when(tradeInNotificationClient.tradeInApply(?)(?)).thenReturn(Success(()))

      val shouldProcessBeforeSend: YdbShouldProcessResult = worker.shouldProcess(offer, None)
      val processResult: YdbWorkerResult = worker.process(offer, None)
      val shouldProcessAfterSend: YdbShouldProcessResult = worker.shouldProcess(offer, processResult.nextState)

      val ad: AdditionalDataForReading = additionalDataLoader.loadAdditionalDataForReading(offer)

      val apiOffer: ApiOfferModel.Offer = offerFormConverter.convert(ad, offer)

      verify(tradeInNotificationClient).tradeInApply(apiOffer)

      val expectedState: String = LetterState(wasSent = true).toJsonString

      processResult.nextState shouldBe Some(expectedState)
      shouldProcessBeforeSend shouldBe YdbShouldProcessResult(shouldProcess = true)
      shouldProcessAfterSend shouldBe YdbShouldProcessResult(shouldProcess = false)
    }
  }

  private trait Worker {

    def offer: VosOffer

    val tradeInNotificationClient: TradeInNotifierClient = mock[TradeInNotifierClient]

    val worker: TradeInApplyToNotifyWorkerYdb = new TradeInApplyToNotifyWorkerYdb(
      offerFormConverter,
      additionalDataLoader,
      tradeInNotificationClient
    ) with YdbWorkerTestImpl
  }
}
