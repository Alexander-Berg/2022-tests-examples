package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.joda.time.DateTime
import org.mockito.Mockito.verify
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.SellerType
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.model.{TestUtils, VinResolutionReloadRequest}
import ru.yandex.vos2.autoru.services.vindecoder.VinDecoderClient
import ru.yandex.vos2.commonfeatures.{FeatureRegistryFactory, FeaturesManager}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

class VinResolutionReloadWorkerYdbTest
  extends AnyWordSpec
  with Matchers
  with MockitoSupport
  with VinIndexResolutionUtils {

  implicit val traced: Traced = Traced.empty

  private val testVin = "XW8ZZZ61ZEG061733"
  private val testId = "11111-test"
  private val testMark = "AUDI"
  private val testModel = "A4"
  private val testHp = 100
  private val testDisp = 0
  private val testYear = 2012
  private val testOwners = 3
  private val DealerUserRef: String = "ac_123"

  private val featureRegistry = FeatureRegistryFactory.inMemory()
  private val featuresManager = new FeaturesManager(featureRegistry)
  private val onlyImportantReports = featuresManager.ReloadOnlyImportantReports
  private val feature = featuresManager.VinDecoderResolutionYdb
  featureRegistry.updateFeature(feature.name, true)
  private val minute = 60 * 1000L
  private val hour = 60 * minute
  private val day = 24 * hour

  private def testOffer = {
    val offer = TestUtils.createOffer()
    offer
      .setOfferID(testId)
      .setTimestampCreate(ru.yandex.vos2.getNow - 10 * 60 * 60 * 1000)
    offer.getOfferAutoruBuilder
      .setCategory(Category.CARS)
      .setSection(Section.USED)
      .getDocumentsBuilder
      .setVin(testVin)
    offer.getOfferAutoruBuilder.getCarInfoBuilder
      .setMark(testMark)
      .setModel(testModel)
      .setHorsePower(testHp)
      .setDisplacement(testDisp)
    offer.getOfferAutoruBuilder.getEssentialsBuilder
      .setYear(testYear)
    offer.getOfferAutoruBuilder.getOwnershipBuilder
      .setPtsOwnersCount(testOwners)
    offer.getOfferAutoruBuilder.getVinResolutionBuilder
      .setVersion(version)
      .setTimestamp(ru.yandex.vos2.getNow)
    offer
  }

  abstract private class Fixture {
    val offer: Offer

    val client = mockStrict[VinDecoderClient]

    val worker = new VinResolutionReloadWorkerYdb(
      client
    ) with YdbWorkerTestImpl {
      override def features: FeaturesManager = featuresManager
    }
  }

  "ignore offer with category != CARS" in new Fixture {
    val offerBuilder = testOffer
    offerBuilder.getOfferAutoruBuilder
      .setCategory(Category.TRUCKS)

    val offer = offerBuilder.build
    assert(!worker.shouldProcess(offer, None).shouldProcess)
  }

  "ignore new offers" in new Fixture {
    val offerBuilder = testOffer

    offerBuilder.getOfferAutoruBuilder
      .setSection(Section.NEW)

    val offer = offerBuilder.build
    assert(!worker.shouldProcess(offer, None).shouldProcess)
  }

  "ignore offer without vin" in new Fixture {
    val offerBuilder = testOffer
    offerBuilder.getOfferAutoruBuilder.getDocumentsBuilder.clearVin()

    val offer = offerBuilder.build
    assert(!worker.shouldProcess(offer, None).shouldProcess)
  }

  "ignore offers with resolution younger than 30 days and valid legal part" in new Fixture {
    val ts = Some(ru.yandex.vos2.getNow - 29 * day)
    val offerBuilder = testOffer

    offerBuilder.getOfferAutoruBuilder.getVinResolutionBuilder
      .setVersion(version)
      .setResolution(resolutionLegalOk(ts))

    val offer = offerBuilder.build
    assert(!worker.shouldProcess(offer, None).shouldProcess)
  }

  "ignore offers with resolution younger than 5 days and error in legal part" in new Fixture {
    val ts = Some(ru.yandex.vos2.getNow - 4 * day)
    val offerBuilder = testOffer
    offerBuilder.getOfferAutoruBuilder.getVinResolutionBuilder
      .setVersion(version)
      .setResolution(resolutionLegalError(ts))

    val offer = offerBuilder.build
    assert(!worker.shouldProcess(offer, None).shouldProcess)
  }

  "ignore offers with reload request younger than a day" in new Fixture {
    val ts = Some(ru.yandex.vos2.getNow - 30 * day - 2 * hour)
    val offerBuilder = testOffer
    offerBuilder.getOfferAutoruBuilder.getVinResolutionBuilder
      .setVersion(version)
      .setReloadTimestamp(ru.yandex.vos2.getNow)
      .setResolution(resolutionLegalError(ts))

    val offer = offerBuilder.build
    assert(!worker.shouldProcess(offer, None).shouldProcess)
  }

  "skip if offer has invalid vin" in new Fixture {
    val offerBuilder = testOffer
    offerBuilder.getOfferAutoruBuilder.getDocumentsBuilder.setVin("kek")
    val offer = offerBuilder.build
    assert(!worker.shouldProcess(offer, None).shouldProcess)
  }

  "skip if offer has valid vin and no vin resolution" in new Fixture {
    val offerBuilder = testOffer
    offerBuilder.getOfferAutoruBuilder.getDocumentsBuilder.setVin("Z8T4C5FS9BM005269")
    val offer = offerBuilder.build
    assert(!worker.shouldProcess(offer, None).shouldProcess)
  }

  "process if offer resolution is 30 days old and legal part is OK" in new Fixture {
    val ts = Some(ru.yandex.vos2.getNow - 30 * day - 12 * hour)
    val offerBuilder = testOffer
    offerBuilder.getOfferAutoruBuilder.getVinResolutionBuilder
      .setVersion(version)
      .setResolution(resolutionLegalOk(ts))

    val offer = offerBuilder.build
    assert(worker.shouldProcess(offer, None).shouldProcess)
  }

  "process if offer resolution is 20 days old and legal part has errors" in new Fixture {
    val ts = Some(ru.yandex.vos2.getNow - 20 * day - 12 * hour)
    val offerBuilder = testOffer
    offerBuilder.getOfferAutoruBuilder.getVinResolutionBuilder
      .setVersion(version)
      .setResolution(resolutionLegalError(ts))
    offerBuilder.getOfferAutoruBuilder.getCarInfoBuilder.setModel("A5")

    val offer = offerBuilder.build
    assert(worker.shouldProcess(offer, None).shouldProcess)
  }

  "process if dealer offer resolution is 2 day old and legal part has errors" in new Fixture {
    val ts = Some(ru.yandex.vos2.getNow - 2 * day - 12 * hour)
    val offerBuilder = testOffer
    offerBuilder
      .setUserRef(DealerUserRef)
      .getOfferAutoruBuilder
      .setSellerType(SellerType.COMMERCIAL)
    offerBuilder.getOfferAutoruBuilder.getVinResolutionBuilder
      .setVersion(version)
      .setResolution(resolutionLegalError(ts))
    offerBuilder.getOfferAutoruBuilder.getCarInfoBuilder.setModel("A5")

    val offer = offerBuilder.build
    assert(worker.shouldProcess(offer, None).shouldProcess)
  }

  "process if dealer offer resolution is 5 minutes old and legal part has errors" in new Fixture {
    val offerBuilder = testOffer
    val ts = Some(offerBuilder.getTimestampCreate - 5 * minute)
    offerBuilder
      .setUserRef(DealerUserRef)
      .getOfferAutoruBuilder
      .setSellerType(SellerType.COMMERCIAL)
    offerBuilder.getOfferAutoruBuilder.getVinResolutionBuilder
      .setVersion(version)
      .setResolution(resolutionLegalError(ts))
    offerBuilder.getOfferAutoruBuilder.getCarInfoBuilder.setModel("A5")

    val offer = offerBuilder.build
    assert(worker.shouldProcess(offer, None).shouldProcess)
  }

  "ignore if dealer offer resolution is 5 minutes old and only pledge part has errors" in new Fixture {
    val offerBuilder = testOffer
    val ts = Some(offerBuilder.getTimestampCreate - 5 * minute)
    offerBuilder
      .setUserRef(DealerUserRef)
      .getOfferAutoruBuilder
      .setSellerType(SellerType.COMMERCIAL)
    offerBuilder.getOfferAutoruBuilder.getVinResolutionBuilder
      .setVersion(version)
      .setResolution(resolutionOnlyPledgesError(ts))
    offerBuilder.getOfferAutoruBuilder.getCarInfoBuilder.setModel("A5")

    val offer = offerBuilder.build
    assert(!worker.shouldProcess(offer, None).shouldProcess)
  }

  "ignore if dealer offer resolution is younger than 5 minutes and legal part has errors" in new Fixture {
    val offerBuilder = testOffer
    val ts = Some(offerBuilder.getTimestampCreate + 5 * minute)
    offerBuilder
      .setUserRef(DealerUserRef)
      .getOfferAutoruBuilder
      .setSellerType(SellerType.COMMERCIAL)
    offerBuilder.getOfferAutoruBuilder.getVinResolutionBuilder
      .setVersion(version)
      .setResolution(resolutionLegalError(ts))
    offerBuilder.getOfferAutoruBuilder.getCarInfoBuilder.setModel("A5")

    val offer = offerBuilder.build
    assert(!worker.shouldProcess(offer, None).shouldProcess)
  }

  "send reload request to vin-decoder on offer" in new Fixture {

    val ts = Some(ru.yandex.vos2.getNow - 90 * day - 12 * hour)
    when(client.reloadResolution(?, ?)(?)).thenReturn(Success(()))

    val offerBuilder = testOffer
    offerBuilder.getOfferAutoruBuilder.getVinResolutionBuilder
      .setVersion(version)
      .setResolution(resolutionLegalOk(ts))
    val offer = offerBuilder.build()
    val result = worker.process(offer, None)
    val resultOffer = result.updateOfferFunc.get(offer)
    val resultOfferAutoru = resultOffer.getOfferAutoru

    result.nextCheck.get.getMillis shouldBe (new DateTime().plus(1.hour.toMillis)).getMillis +- 1000
    resultOfferAutoru.getVinResolution.getReloadTimestamp > 0 shouldBe true
    verify(client).reloadResolution(testVin, VinResolutionReloadRequest(offer))
  }

  "reschedule if reload request failed" in new Fixture {
    when(client.reloadResolution(?, ?)(?)).thenReturn(Failure(new RuntimeException))

    val ts = Some(ru.yandex.vos2.getNow - 90 * day - 12 * hour)

    val offerBuilder = testOffer
    offerBuilder.getOfferAutoruBuilder.getVinResolutionBuilder
      .setVersion(version)
      .setResolution(resolutionLegalError(ts))

    val offer = offerBuilder.build()
    val result = worker.process(offer, None)
    val resultOfferAutoru = offer.getOfferAutoru

    result.nextCheck.nonEmpty shouldBe true
    resultOfferAutoru.getVinResolution.getReloadTimestamp shouldBe 0

    verify(client).reloadResolution(testVin, VinResolutionReloadRequest(offer))
  }
}
