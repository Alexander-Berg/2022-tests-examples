package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.mockito.Mockito.verify
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.auto.api.vin.VinResolutionEnums
import ru.auto.api.vin.VinResolutionEnums.{ResolutionPart, Status}
import ru.auto.api.vin.VinResolutionModel.ResolutionEntry
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.VinResolutionWorkerYdb.{offerHashCode, ResolutionTags}
import ru.yandex.vertis.ydb.skypper.YdbWrapper
import ru.yandex.vos2.AutoruModel.AutoruOffer.SellerType
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.catalog.cars.CarsCatalog
import ru.yandex.vos2.autoru.dao.offers.AutoruOfferDao
import ru.yandex.vos2.autoru.model.{TestUtils, VinResolutionRequest}
import ru.yandex.vos2.autoru.services.vindecoder.VinDecoderClient
import ru.yandex.vos2.commonfeatures.{FeatureRegistryFactory, FeaturesManager}

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class VinResolutionWorkerYdbTest extends AnyWordSpec with Matchers with MockitoSupport with VinIndexResolutionUtils {

  implicit val traced: Traced = Traced.empty

  private val testVin = "XW8ZZZ61ZEG061733"
  private val testId = "11111-test"
  private val testMark = "AUDI"
  private val testModel = "A4"
  private val testHp = 100
  private val testDisp = 0
  private val testYear = 2012
  private val testOwners = 3

  private val featureRegistry = FeatureRegistryFactory.inMemory()
  private val featuresManager = new FeaturesManager(featureRegistry)
  private val feature = featuresManager.VinDecoderResolutionYdb
  featureRegistry.updateFeature(feature.name, true)

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
    val catalog = mockStrict[CarsCatalog]

    val worker = new VinResolutionWorkerYdb(
      client,
      catalog
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

  "ignore if nothing changed" in new Fixture {
    val offerBuilder = testOffer
    offerBuilder.getOfferAutoruBuilder.getVinResolutionBuilder
      .setOfferHash(offerHashCode(offerBuilder.getOfferAutoru))
      .setVersion(version)
      .setResolution(resolutionNotEmpty)
    val offer = offerBuilder.build

    assert(!worker.shouldProcess(offer, None).shouldProcess)
  }

  "skip if offer has invalid vin and no vin resolution" in new Fixture {
    val offerBuilder = testOffer

    offerBuilder.getOfferAutoruBuilder.getDocumentsBuilder.setVin("kek")
    val offer = offerBuilder.build

    assert(!worker.shouldProcess(offer, None).shouldProcess)
  }

  "process if offer has invalid vin but has vin resolution" in new Fixture {
    val offerBuilder = testOffer

    offerBuilder.getOfferAutoruBuilder.getVinResolutionBuilder
      .setResolution(resolutionNotEmpty)

    offerBuilder.getOfferAutoruBuilder.getDocumentsBuilder.setVin("kek")
    val offer = offerBuilder.build
    assert(worker.shouldProcess(offer, None).shouldProcess)

  }

  "skip if offer has invalid vin but has vin resolution by licenseplate" in new Fixture {
    val offerBuilder = testOffer

    offerBuilder.getOfferAutoruBuilder.getVinResolutionBuilder
      .setResolution(resolutionWithSummary(VinResolutionEnums.Status.UNTRUSTED))

    offerBuilder.getOfferAutoruBuilder.getDocumentsBuilder
      .setVin("kek")
      .setLicensePlate("X555XX77")
    val offer = offerBuilder.build

    assert(!worker.shouldProcess(offer, None).shouldProcess)
  }

  "process if version changed" in new Fixture {
    val offerBuilder = testOffer

    offerBuilder.getOfferAutoruBuilder.getVinResolutionBuilder
      .setOfferHash(offerHashCode(testOffer.getOfferAutoru))
      .setVersion(0)
      .setResolution(resolutionNotEmpty)
    val offer = offerBuilder.build

    assert(worker.shouldProcess(offer, None).shouldProcess)

  }

  "process if important offer part changed" in new Fixture {
    val offerBuilder = testOffer

    offerBuilder.getOfferAutoruBuilder.getVinResolutionBuilder
      .setOfferHash(offerHashCode(offerBuilder.getOfferAutoru))
      .setVersion(version)
      .setResolution(resolutionNotEmpty)
    offerBuilder.getOfferAutoruBuilder.getCarInfoBuilder.setModel("A5")
    val offer = offerBuilder.build

    assert(worker.shouldProcess(offer, None).shouldProcess)

  }

  "process if vin changed" in new Fixture {
    val offerBuilder = testOffer

    offerBuilder.getOfferAutoruBuilder.getVinResolutionBuilder
      .setOfferHash(offerHashCode(offerBuilder.getOfferAutoru))
      .setVersion(version)
      .setTimestamp(ru.yandex.vos2.getNow)
      .setResolution(resolutionNotEmpty)
    offerBuilder.getOfferAutoruBuilder.getDocumentsBuilder
      .setVin("XW8ZZZ61ZEG061734")
    val offer = offerBuilder.build

    assert(worker.shouldProcess(offer, None).shouldProcess)

  }

  "process if force update" in new Fixture {
    val offerBuilder = testOffer

    offerBuilder.getOfferAutoruBuilder.getVinResolutionBuilder
      .setOfferHash(offerHashCode(offerBuilder.getOfferAutoru))
      .setVersion(version)
      .setResolution(resolutionNotEmpty)
      .setForceUpdate(true)
    val offer = offerBuilder.build

    assert(worker.shouldProcess(offer, None).shouldProcess)

  }

  "send vin resolution request to vin-decoder on offer processing and save resolution" in new Fixture {

    when(client.getResolution(?, ?)(?)).thenReturn(Success(resolutionNotEmpty))
    when(catalog.getCardByTechParamId(?)).thenReturn(None)

    val offerBuilder = testOffer

    offerBuilder.addTag(ResolutionTags(Status.UNKNOWN))
    offerBuilder.addTag("some_other_tag")
    val offer = offerBuilder.build

    val result = worker.process(offer, None)

    val resultOffer = result.updateOfferFunc.get(offer)

    val resultOfferAutoRu = resultOffer.getOfferAutoru

    result.nextCheck shouldBe None
    resultOfferAutoRu.getVinResolution.hasResolution shouldBe true
    resultOfferAutoRu.getVinResolution.getVersion shouldBe version
    resultOfferAutoRu.getVinResolution.getResolution shouldBe resolutionNotEmpty
    resultOffer.getTagList.asScala.toSet shouldBe Set(ResolutionTags(Status.OK), "some_other_tag")

    verify(client).getResolution(testVin, VinResolutionRequest(offer.getOfferID, offer.getOfferAutoru, None))
  }

  "not update summary change time if summary not changed" in new Fixture {

    val biggerResolution = resolutionNotEmpty.toBuilder
      .addEntries(
        ResolutionEntry
          .newBuilder()
          .setPart(ResolutionPart.RP_ACCIDENTS)
          .setStatus(VinResolutionEnums.Status.ERROR)
      )
      .build()

    when(client.getResolution(?, ?)(?)).thenReturn(Success(biggerResolution))
    when(catalog.getCardByTechParamId(?)).thenReturn(None)

    val offerBuilder = testOffer

    offerBuilder.getOfferAutoruBuilder.getVinResolutionBuilder
      .setOfferHash(offerHashCode(offerBuilder.getOfferAutoru))
      .setVersion(version)
      .setTimestamp(ru.yandex.vos2.getNow)
      .setSummaryChangeTimestamp(5)
      .setResolution(resolutionNotEmpty)

    offerBuilder.addTag(ResolutionTags(Status.UNKNOWN))
    offerBuilder.addTag("some_other_tag")
    val offer = offerBuilder.build

    val result = worker.process(offer, None)

    val resultOffer = result.updateOfferFunc.get(offer)

    val resultOfferAutoRu = resultOffer.getOfferAutoru
    result.nextCheck shouldBe None

    resultOfferAutoRu.getVinResolution.hasResolution shouldBe true
    resultOfferAutoRu.getVinResolution.getVersion shouldBe version
    resultOfferAutoRu.getVinResolution.getResolution shouldBe biggerResolution
    resultOffer.getTagList.asScala.toSet shouldBe Set(ResolutionTags(Status.OK), "some_other_tag")
    resultOfferAutoRu.getVinResolution.getSummaryChangeTimestamp shouldBe 5
    resultOfferAutoRu.getNotificationsCount shouldBe 0

    verify(client).getResolution(testVin, VinResolutionRequest(offer.getOfferID, offer.getOfferAutoru, None))
  }

  "update summary change time if summary changed" in new Fixture {

    val biggerResolution = resolutionNotEmpty.toBuilder
      .clearEntries()
      .addEntries(
        ResolutionEntry
          .newBuilder()
          .setPart(ResolutionPart.SUMMARY)
          .setStatus(VinResolutionEnums.Status.ERROR)
      )
      .build()

    when(client.getResolution(?, ?)(?)).thenReturn(Success(biggerResolution))
    when(catalog.getCardByTechParamId(?)).thenReturn(None)

    val offerBuilder = testOffer

    offerBuilder.getOfferAutoruBuilder.getVinResolutionBuilder
      .setOfferHash(offerHashCode(offerBuilder.getOfferAutoru))
      .setVersion(version)
      .setTimestamp(ru.yandex.vos2.getNow)
      .setResolution(resolutionNotEmpty)

    offerBuilder.addTag(ResolutionTags(Status.UNKNOWN))
    offerBuilder.addTag("some_other_tag")
    val offer = offerBuilder.build

    val result = worker.process(offer, None)

    val resultOffer = result.updateOfferFunc.get(offer)

    val resultOfferAutoRu = resultOffer.getOfferAutoru
    result.nextCheck shouldBe None

    resultOfferAutoRu.getVinResolution.hasResolution shouldBe true
    resultOfferAutoRu.getVinResolution.getVersion shouldBe version
    resultOfferAutoRu.getVinResolution.getResolution shouldBe biggerResolution
    resultOffer.getTagList.asScala.toSet shouldBe Set(ResolutionTags(Status.ERROR), "some_other_tag")
    resultOfferAutoRu.getVinResolution.getSummaryChangeTimestamp should not be 0
    resultOfferAutoRu.getNotificationsCount shouldBe 1

    verify(client).getResolution(testVin, VinResolutionRequest(offer.getOfferID, offer.getOfferAutoru, None))
  }

  "send vin resolution request to vin-decoder if more then 5 hours passed" in new Fixture {

    when(client.getResolution(?, ?)(?)).thenReturn(Success(resolutionNotEmpty))
    when(catalog.getCardByTechParamId(?)).thenReturn(None)

    val offerBuilder = testOffer

    offerBuilder.getOfferAutoruBuilder.getVinResolutionBuilder
      .setVersion(version)
      .setOfferHash(offerHashCode(offerBuilder.getOfferAutoru))
      .setTimestamp(ru.yandex.vos2.getNow - 10 * 60 * 60 * 1000)
    val offer = offerBuilder.build

    val result = worker.process(offer, None)

    val resultOffer = result.updateOfferFunc.get(offer)

    val resultOfferAutoRu = resultOffer.getOfferAutoru
    result.nextCheck shouldBe None

    resultOfferAutoRu.getVinResolution.hasResolution shouldBe true
    resultOfferAutoRu.getVinResolution.getVersion shouldBe version
    resultOfferAutoRu.getVinResolution.getResolution shouldBe resolutionNotEmpty
    resultOfferAutoRu.getVinResolution.getSummaryChangeTimestamp should not be 0

    verify(client).getResolution(testVin, VinResolutionRequest(offer.getOfferID, offer.getOfferAutoru, None))
  }

  "reschedule offer is have been less than 24 hours since creation and resolution is empty" in new Fixture {
    when(client.getResolution(?, ?)(?)).thenReturn(Success(resolutionEmpty))
    when(catalog.getCardByTechParamId(?)).thenReturn(None)

    val offerBuilder = testOffer
    val offer = offerBuilder.build
    val result = worker.process(offer, None)

    val resultOffer = (offer)

    val resultOfferAutoRu = resultOffer.getOfferAutoru
    result.nextCheck.nonEmpty shouldBe true
    resultOfferAutoRu.getVinResolution.getResolution.getEntriesCount shouldBe 0
    result.updateOfferFunc shouldBe None
    verify(client).getResolution(testVin, VinResolutionRequest(offer.getOfferID, offer.getOfferAutoru, None))
  }

  //ru.yandex.vos2.getNow - offer.getTimestampCreate > 24 * 60 * 1000
  "do not reschedule offer is have been more than 24 hours since creation and resolution is empty" in new Fixture {

    when(client.getResolution(?, ?)(?)).thenReturn(Success(resolutionEmpty))
    when(catalog.getCardByTechParamId(?)).thenReturn(None)

    val offerBuilder = testOffer

    offerBuilder.setTimestampCreate(ru.yandex.vos2.getNow - 25 * 60 * 60 * 1000)
    val offer = offerBuilder.build

    val result = worker.process(offer, None)

    val resultOffer = result.updateOfferFunc.get(offer)

    val resultOfferAutoRu = resultOffer.getOfferAutoru
    result.nextCheck shouldBe None

    resultOfferAutoRu.getVinResolution.getResolution.getEntriesCount shouldBe 0

    verify(client).getResolution(testVin, VinResolutionRequest(offer.getOfferID, offer.getOfferAutoru, None))
  }

  "set empty resolution if vin changed to empty" in new Fixture {

    val offerBuilder = testOffer

    offerBuilder.getOfferAutoruBuilder.getVinResolutionBuilder
      .setVersion(version)
      .setOfferHash(0) //hash changed
      .setTimestamp(ru.yandex.vos2.getNow - 10 * 60 * 60 * 1000)
      .setResolution(resolutionNotEmpty)
    offerBuilder.getOfferAutoruBuilder.getDocumentsBuilder.clearVin()
    val offer = offerBuilder.build

    val result = worker.process(offer, None)

    val resultOffer = result.updateOfferFunc.get(offer)

    val resultOfferAutoRu = resultOffer.getOfferAutoru
    result.nextCheck shouldBe None

    resultOfferAutoRu.getVinResolution.getResolution.getEntriesCount shouldBe 0
    resultOfferAutoRu.getVinResolution.getOfferHash shouldBe offerHashCode(offer.getOfferAutoru)
  }

  "reschedule offer is vin decoder has failed" in new Fixture {

    when(client.getResolution(?, ?)(?)).thenReturn(Failure(new RuntimeException))
    when(catalog.getCardByTechParamId(?)).thenReturn(None)

    val offerBuilder = testOffer
    val offer = offerBuilder.build

    val result = worker.process(offer, None)

    val resultOfferAutoRu = offer.getOfferAutoru
    result.nextCheck.nonEmpty shouldBe true

    resultOfferAutoRu.getVinResolution.hasResolution shouldBe false

    verify(client).getResolution(testVin, VinResolutionRequest(offer.getOfferID, offer.getOfferAutoru, None))
  }

  "reschedule offer is is too young" in new Fixture {

    val offerBuilder = testOffer

    offerBuilder.setTimestampCreate(ru.yandex.vos2.getNow)
    val offer = offerBuilder.build

    val result = worker.process(offer, None)

    val resultOfferAutoRu = offer.getOfferAutoru
    result.nextCheck.nonEmpty shouldBe true
    result.updateOfferFunc shouldBe None

    resultOfferAutoRu.getVinResolution.hasResolution shouldBe false
  }

  "reschedule if dealer offer have legal problems" in new Fixture {

    when(client.getResolution(?, ?)(?)).thenReturn(Success(resolutionLegalError()))
    when(catalog.getCardByTechParamId(?)).thenReturn(None)

    val offerBuilder = testOffer.setTimestampCreate(ru.yandex.vos2.getNow - 10 * 60 * 60 * 1000)

    offerBuilder
      .setUserRef("ac_123")
      .getOfferAutoruBuilder
      .setSellerType(SellerType.COMMERCIAL)
    val offer = offerBuilder.build

    val result = worker.process(offer, None)

    val resultOffer = result.updateOfferFunc.get(offer)

    val resultOfferAutoRu = resultOffer.getOfferAutoru
    result.nextCheck.nonEmpty shouldBe true

    resultOfferAutoRu.getVinResolution.hasResolution shouldBe true
  }

  "vin_service_history tag added" in new Fixture {

    when(client.getResolution(?, ?)(?)).thenReturn(Success(resolutionServiceHistory))
    when(catalog.getCardByTechParamId(?)).thenReturn(None)

    val offerBuilder = testOffer
    val offer = offerBuilder.build

    val result = worker.process(offer, None)

    val resultOffer = result.updateOfferFunc.get(offer)

    val resultOfferAutoRu = resultOffer.getOfferAutoru
    result.nextCheck shouldBe None

    resultOfferAutoRu.getVinResolution.hasResolution shouldBe true
    resultOfferAutoRu.getVinResolution.getVersion shouldBe version
    resultOfferAutoRu.getVinResolution.getResolution shouldBe resolutionServiceHistory
    resultOffer.getTagList.asScala.toSet shouldBe Set("vin_resolution_ok", "vin_service_history")

    verify(client).getResolution(testVin, VinResolutionRequest(offer.getOfferID, offer.getOfferAutoru, None))
  }

  "vin_offers_history tag added" in new Fixture {

    when(client.getResolution(?, ?)(?)).thenReturn(Success(resolutionOffersHistory))
    when(catalog.getCardByTechParamId(?)).thenReturn(None)

    val offerBuilder = testOffer
    val offer = offerBuilder.build

    val result = worker.process(offer, None)

    val resultOffer = result.updateOfferFunc.get(offer)

    val resultOfferAutoRu = resultOffer.getOfferAutoru
    result.nextCheck shouldBe None

    resultOfferAutoRu.getVinResolution.hasResolution shouldBe true
    resultOfferAutoRu.getVinResolution.getVersion shouldBe version
    resultOfferAutoRu.getVinResolution.getResolution shouldBe resolutionOffersHistory
    resultOffer.getTagList.asScala.toSet shouldBe Set("vin_resolution_ok", "vin_offers_history")

    verify(client).getResolution(testVin, VinResolutionRequest(offer.getOfferID, offer.getOfferAutoru, None))
  }

  "vin_offers_bad_mileage tag added" in new Fixture {

    when(client.getResolution(?, ?)(?)).thenReturn(Success(resolutionOffersHistoryBadMileage))
    when(catalog.getCardByTechParamId(?)).thenReturn(None)

    val offerBuilder = testOffer
    val offer = offerBuilder.build

    val result = worker.process(offer, None)

    val resultOffer = result.updateOfferFunc.get(offer)

    val resultOfferAutoRu = resultOffer.getOfferAutoru
    result.nextCheck shouldBe None

    resultOfferAutoRu.getVinResolution.hasResolution shouldBe true
    resultOfferAutoRu.getVinResolution.getVersion shouldBe version
    resultOfferAutoRu.getVinResolution.getResolution shouldBe resolutionOffersHistoryBadMileage
    resultOffer.getTagList.asScala.toSet shouldBe
      Set("vin_resolution_ok", "vin_offers_history", "vin_offers_bad_mileage")

    verify(client).getResolution(testVin, VinResolutionRequest(offer.getOfferID, offer.getOfferAutoru, None))
  }

  "vin_extended_warranty tag added" in new Fixture {

    when(client.getResolution(?, ?)(?)).thenReturn(Success(resolutionExtendedWarranty))
    when(catalog.getCardByTechParamId(?)).thenReturn(None)

    val offerBuilder = testOffer
    val offer = offerBuilder.build

    val result = worker.process(offer, None)

    val resultOffer = result.updateOfferFunc.get(offer)

    val resultOfferAutoRu = resultOffer.getOfferAutoru
    result.nextCheck shouldBe None

    resultOfferAutoRu.getVinResolution.hasResolution shouldBe true
    resultOfferAutoRu.getVinResolution.getVersion shouldBe version
    resultOfferAutoRu.getVinResolution.getResolution shouldBe resolutionExtendedWarranty
    resultOffer.getTagList.asScala.toSet shouldBe Set("vin_resolution_ok", "vin_extended_warranty")

    verify(client).getResolution(testVin, VinResolutionRequest(offer.getOfferID, offer.getOfferAutoru, None))
  }

  "do not add history tags if resolution is in progress" in new Fixture {

    val resolution = resolutionWithSummary(Status.IN_PROGRESS)
    when(client.getResolution(?, ?)(?)).thenReturn(Success(resolution))
    when(catalog.getCardByTechParamId(?)).thenReturn(None)

    val offerBuilder = testOffer
    val offer = offerBuilder.build

    val result = worker.process(offer, None)

    val resultOffer = result.updateOfferFunc.get(offer)

    val resultOfferAutoRu = resultOffer.getOfferAutoru
    result.nextCheck shouldBe None

    resultOfferAutoRu.getVinResolution.hasResolution shouldBe true
    resultOfferAutoRu.getVinResolution.getVersion shouldBe version
    resultOfferAutoRu.getVinResolution.getResolution shouldBe resolution
    resultOffer.getTagList.asScala should contain noneOf ("vin_service_history", "vin_offers_history", "vin_offers_bad_mileage", "vin_extended_warranty")

    verify(client).getResolution(testVin, VinResolutionRequest(offer.getOfferID, offer.getOfferAutoru, None))
  }

}
