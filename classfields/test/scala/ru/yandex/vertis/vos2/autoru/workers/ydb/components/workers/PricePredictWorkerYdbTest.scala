package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.joda.time.{DateTime, LocalDate}
import org.mockito.Mockito.{times, verify}
import org.mockito.{ArgumentCaptor, Mockito}
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.Category
import ru.auto.api.StatsModel.PredictPrice
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.SellerType
import ru.yandex.vos2.BasicsModel.Currency
import ru.yandex.vos2.OfferModel
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.autoru.services.pricepredict._
import ru.yandex.vos2.commonfeatures.{FeatureRegistryFactory, FeaturesManager}

import scala.jdk.CollectionConverters._
import scala.util.Success

class PricePredictWorkerYdbTest
  extends AnyWordSpec
  with InitTestDbs
  with MockitoSupport
  with Matchers
  with BeforeAndAfter {

  private val rid = 213
  private val kmAge = 40000
  private val color = "ffffff"
  private val owningTime = 2
  private val ownersCount = 1
  private val year = 2014

  private val pricePredictClient = mock[PricePredictClient]

  private val realPrecictClient = {
    val host = "prediction-api-int.vrts-slb.test.vertis.yandex.net"
    val port = 80
    new HttpPricePredictClient(host, port, components.operational, components.offerFormConverter)
  }

  before {
    Mockito.reset(pricePredictClient)

    when(pricePredictClient.predict(?, ?)(?)).thenReturn(Success(Option(PredictPrice.getDefaultInstance)))
    when(pricePredictClient.getPredicts(?, ?)(?)).thenCallRealMethod()
    when(pricePredictClient.requiredParams(?)).thenReturn(true)
  }

  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {
    val featureRegistry = FeatureRegistryFactory.inMemory()
    val featuresManager = new FeaturesManager(featureRegistry)

    val worker = new PricePredictWorkerYdb(
      pricePredictClient
    ) with YdbWorkerTestImpl {
      override def features: FeaturesManager = featuresManager
    }
  }

  private def checkProcessed(offer: Offer, worker: PricePredictWorkerYdb): Unit = {
    worker.shouldProcess(offer, None).shouldProcess shouldBe true
    worker.process(offer, None)

    val offerCaptor: ArgumentCaptor[OfferModel.Offer] = ArgumentCaptor.forClass(classOf[OfferModel.Offer])
    val modelCaptor: ArgumentCaptor[Option[PricePredictModel]] =
      ArgumentCaptor.forClass(classOf[Option[PricePredictModel]])
    verify(pricePredictClient, times(3)).predict(offerCaptor.capture(), modelCaptor.capture())(?)
    offerCaptor.getValue shouldBe offer
    modelCaptor.getAllValues.asScala.toList shouldBe List(None, Some(Q10Q90PredictModel), Some(Q25Q75PredictModel))
  }

  private def checkIgnored(offer: Offer, worker: PricePredictWorkerYdb): Unit = {
    worker.shouldProcess(offer, None).shouldProcess shouldBe false

  }

  private def checkProcessIgnored(offer: Offer, worker: PricePredictWorkerYdb): Unit = {
    worker.shouldProcess(offer, None).shouldProcess shouldBe true

    val result = worker.process(offer, None)
    result.updateOfferFunc shouldBe None
  }

  private def testOffer(sellerType: SellerType = SellerType.PRIVATE) = {
    val now = DateTime.now
    val offer = TestUtils.createOffer()
    offer.getOfferAutoruBuilder
      .setCategory(Category.CARS)
      .setSellerType(sellerType)
    offer.getOfferAutoruBuilder.getSellerBuilder.getPlaceBuilder
      .setGeobaseId(rid)
    offer.getOfferAutoruBuilder.getCarInfoBuilder
      .setSuperGenId(2305474)
      .setConfigurationId(6143425)
      .setTechParamId(6143500)
    offer.getOfferAutoruBuilder.getStateBuilder
      .setMileage(kmAge)
    offer.getOfferAutoruBuilder
      .setColorHex(color)
    offer.getOfferAutoruBuilder.getOwnershipBuilder
      .setPurchaseDate(LocalDate.now.minusYears(owningTime).toDate.getTime)
      .setPtsOwnersCount(ownersCount)
    offer.getOfferAutoruBuilder.getEssentialsBuilder
      .setYear(year)
    offer
  }

  ("test real predictor") in new Fixture {
    val dealerOffer = testOffer(sellerType = SellerType.COMMERCIAL).build()
    val userOffer = testOffer().build()
    val dealerRes = realPrecictClient.predict(dealerOffer)
    val userRes = realPrecictClient.predict(userOffer)
    userRes
  }

  ("success set first time predict price with all parameters") in new Fixture {
    val offer = testOffer()
    checkProcessed(offer.build(), worker)
  }

  ("success update predict price with all parameters") in new Fixture {
    val offer = testOffer()

    val predictBuilder = offer.getOfferAutoruBuilder.getPredictPriceBuilder
      .setVersion(components.featuresManager.PricePredictYdb.value.generation)
      .setTimestamp(DateTime.now.minusHours(25).toDate.getTime)
    predictBuilder.getAutoruBuilder
      .setCurrency(Currency.RUB)
      .setFrom(450000)
      .setTo(500000)
    checkProcessed(offer.build(), worker)
  }

  ("process offer if version was changed") in new Fixture {
    val offer = testOffer()
    offer.getOfferAutoruBuilder.getPredictPriceBuilder
      .setVersion(components.featuresManager.PricePredictYdb.value.generation - 1)

    checkProcessed(offer.build(), worker)
  }

  ("repeat update predict price with delay") in new Fixture {
    val offer = testOffer()

    val predictBuilder = offer.getOfferAutoruBuilder.getPredictPriceBuilder
      .setVersion(components.featuresManager.PricePredictYdb.value.generation)
      .setTimestamp(DateTime.now.minusHours(1).toDate.getTime)
    predictBuilder.getAutoruBuilder
      .setCurrency(Currency.RUB)
      .setFrom(450000)
      .setTo(500000)
    checkProcessIgnored(offer.build(), worker)
  }

  ("check ignored moto offers") in new Fixture {
    val offer = testOffer()

    offer.getOfferAutoruBuilder
      .setCategory(Category.MOTO)
    checkIgnored(offer.build(), worker)
  }

  ("calculate correct market price") in new Fixture {

    val offer = {
      val b = testOffer(SellerType.COMMERCIAL)
      b.getOfferAutoruBuilder.getPredictPriceBuilder.setVersion(0)
      b.build()
    }

    val predictPrice = {
      val builder = PredictPrice.newBuilder
      builder.getAutoruBuilder.setFrom(100).setTo(300)
      builder.build
    }

    when(pricePredictClient.requiredParams(?)).thenReturn(true)

    val offerWithoutMarketPrice = worker.process(offer, None).updateOfferFunc.get(offer)
    assert(offerWithoutMarketPrice.getOfferAutoru.getPredictPrice.getMarket.getPrice == 0)

    when(pricePredictClient.predict(?, ?)(?)).thenReturn(Success(Some(predictPrice)))

    val offerWithMarketPrice = worker.process(offer, None).updateOfferFunc.get(offer)
    assert(offerWithMarketPrice.getOfferAutoru.getPredictPrice.getMarket.getPrice == 200)
  }

}
