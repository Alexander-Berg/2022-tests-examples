package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.joda.time.DateTime
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.feature.impl.BasicFeatureTypes._
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => eqq}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.autoru.components.AutoruCoreComponents
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.autoru.services.parsing._
import ru.yandex.vos2.autoru.utils.docker.DockerAutoruCoreComponents
import ru.yandex.vos2.commonfeatures.{FeatureRegistryFactory, FeaturesManager}
import ru.yandex.vos2.services.mds.MdsPhotoData
import zio.duration.durationInt

class ParsingPhotosUploadWorkerYdbTest extends AnyWordSpec with MockitoSupport with Matchers with BeforeAndAfterAll {
  implicit val traced: Traced = Traced.empty

  private val coreComponents: AutoruCoreComponents = DockerAutoruCoreComponents
  private val parsingClient = mock[ParsingClient]

  private val featureRegistry = FeatureRegistryFactory.inMemory()
  private val featuresManager = new FeaturesManager(featureRegistry)

  abstract private class Fixture {

    val worker = new ParsingPhotosUploadWorkerYdb(
      parsingClient,
      coreComponents.regionTree
    ) with YdbWorkerTestImpl {
      override def features: FeaturesManager = featuresManager
    }
  }

  ("shouldProcess: cars") in new Fixture {
    val offer2 = TestUtils.createOffer(category = Category.CARS)
    assert(!worker.shouldProcess(offer2.build(), None).shouldProcess)

    offer2.getOfferAutoruBuilder.getSourceInfoBuilder.setIsCallcenter(true)
    assert(!worker.shouldProcess(offer2.build(), None).shouldProcess)

    offer2.getOfferAutoruBuilder.getSourceInfoBuilder.setParseUrl("http://example.com")
    assert(!worker.shouldProcess(offer2.build(), None).shouldProcess)

    offer2.getOfferAutoruBuilder.getSourceInfoBuilder.setRemoteUrl("http://example.com")
    assert(!worker.shouldProcess(offer2.build(), None).shouldProcess)

    featureRegistry.updateFeature(featuresManager.UploadParsedPhotosCars.name, true)
    assert(worker.shouldProcess(offer2.build(), None).shouldProcess)

    offer2.getOfferAutoruBuilder.getParsingInfoBuilder.setPhotosUploaded(true)
    assert(!worker.shouldProcess(offer2.build(), None).shouldProcess)

    val offer3 = TestUtils.createOffer(category = Category.CARS, now = System.currentTimeMillis() - 8.days.toMillis)
    offer3.getOfferAutoruBuilder.getSourceInfoBuilder.setIsCallcenter(true)
    offer3.getOfferAutoruBuilder.getSourceInfoBuilder.setParseUrl("http://example.com")
    offer3.getOfferAutoruBuilder.getSourceInfoBuilder.setRemoteUrl("http://example.com")
    assert(!worker.shouldProcess(offer3.build(), None).shouldProcess)

    featureRegistry.updateFeature(featuresManager.UploadParsedPhotosCars.name, false)
  }

  ("shouldProcess: trucks") in new Fixture {
    val offer2 = TestUtils.createOffer(category = Category.TRUCKS)
    assert(!worker.shouldProcess(offer2.build(), None).shouldProcess)

    offer2.getOfferAutoruBuilder.getSourceInfoBuilder.setIsCallcenter(true)
    assert(!worker.shouldProcess(offer2.build(), None).shouldProcess)

    offer2.getOfferAutoruBuilder.getSourceInfoBuilder.setParseUrl("http://example.com")
    assert(!worker.shouldProcess(offer2.build(), None).shouldProcess)

    offer2.getOfferAutoruBuilder.getSourceInfoBuilder.setRemoteUrl("http://example.com")
    assert(!worker.shouldProcess(offer2.build(), None).shouldProcess)

    featureRegistry.updateFeature(featuresManager.UploadParsedPhotosTrucks.name, true)
    assert(worker.shouldProcess(offer2.build(), None).shouldProcess)

    offer2.getOfferAutoruBuilder.getParsingInfoBuilder.setPhotosUploaded(true)
    assert(!worker.shouldProcess(offer2.build(), None).shouldProcess)

    val offer3 = TestUtils.createOffer(category = Category.TRUCKS, now = System.currentTimeMillis() - 8.days.toMillis)
    offer3.getOfferAutoruBuilder.getSourceInfoBuilder.setIsCallcenter(true)
    offer3.getOfferAutoruBuilder.getSourceInfoBuilder.setParseUrl("http://example.com")
    offer3.getOfferAutoruBuilder.getSourceInfoBuilder.setRemoteUrl("http://example.com")
    assert(!worker.shouldProcess(offer3.build(), None).shouldProcess)

    featureRegistry.updateFeature(featuresManager.UploadParsedPhotosTrucks.name, false)
  }

  ("process: not ready") in new Fixture {
    when(parsingClient.getPhotos(?, ?, ?)(?)).thenReturn(ParsedPhotosResponse.Photos(Seq("photo1", "photo2")))
    stub(parsingClient.mdsName(_: Category, _: String, _: String, _: String)(_: Traced)) {
      case (_, offerId, remoteUrl, _, _) => MdsNameNotReady
    }
    val offer = TestUtils.createOffer(category = Category.TRUCKS)
    offer.getOfferAutoruBuilder.getSourceInfoBuilder.setParseUrl("http://example.com")
    val res = worker.process(offer.build(), None)
    assert(res.updateOfferFunc.isEmpty)
    assert(!offer.build().getOfferAutoru.getParsingInfo.getPhotosUploaded)
    assert(res.nextCheck.get.isAfter(new DateTime()))
  }

  ("process: ready and error") in new Fixture {
    when(parsingClient.getPhotos(?, ?, ?)(?)).thenReturn(ParsedPhotosResponse.Photos(Seq("photo1", "photo2")))
    stub(parsingClient.mdsName(_: Category, _: String, _: String, _: String)(_: Traced)) {
      case (_, _, "photo1", namespace, _) =>
        MdsIdSuccess(MdsPhotoData(namespace, "3333-mdsphoto1"))
      case (_, _, "photo2", _, _) =>
        MdsNameError("error!")
    }

    val offer = TestUtils.createOffer(category = Category.TRUCKS)
    offer.getOfferAutoruBuilder.getSourceInfoBuilder.setParseUrl("http://example.com")

    val res = worker.process(offer.build(), None)
    val newOffer = res.updateOfferFunc.get(offer.build())
    assert(newOffer.getOfferAutoru.getPhotoCount == 1)
    assert(newOffer.getOfferAutoru.getPhoto(0).getName == "3333-mdsphoto1")
    assert(newOffer.getOfferAutoru.getParsingInfo.getPhotosUploaded)
    assert(newOffer.getOfferAutoru.getParsingInfo.getFailedPhotosUploadCount == 1)
    assert(newOffer.getOfferAutoru.getParsingInfo.getFailedPhotosUpload(0) == "photo2")

    /**
      *  state.withNewDelay(1.minute) // придем еще раз, чтобы синхронные стейджи подхватили новые фото
      *  не нужно тк изменение офера и так стриггерит остальных воркеров
      */
    assert(res.nextCheck.isEmpty)
  }

  ("process: cars: ready and error, provide phones") in new Fixture {
    when(parsingClient.getPhotos(?, ?, ?)(?)).thenReturn(ParsedPhotosResponse.Photos(Seq("photo1", "photo2")))
    stub(parsingClient.mdsName(_: Category, _: String, _: String, _: String)(_: Traced)) {
      case (_, _, "photo1", namespace, _) =>
        MdsIdSuccess(MdsPhotoData(namespace, "3333-mdsphoto1"))
      case (_, _, "photo2", _, _) =>
        MdsNameError("error!")
    }

    val offer = TestUtils.createOffer(category = Category.CARS)
    offer.getOfferAutoruBuilder.getSourceInfoBuilder.setParseUrl("http://example.com")
    val seller = offer.getOfferAutoruBuilder.getSellerBuilder
    seller.addPhoneBuilder().setNumber("phone1")
    seller.addPhoneBuilder().setNumber("phone2")

    val res = worker.process(offer.build(), None)
    val newOffer = res.updateOfferFunc.get(offer.build())
    assert(newOffer.getOfferAutoru.getPhotoCount == 1)
    assert(newOffer.getOfferAutoru.getPhoto(0).getName == "3333-mdsphoto1")
    assert(newOffer.getOfferAutoru.getParsingInfo.getPhotosUploaded)
    assert(newOffer.getOfferAutoru.getParsingInfo.getFailedPhotosUploadCount == 1)
    assert(newOffer.getOfferAutoru.getParsingInfo.getFailedPhotosUpload(0) == "photo2")
    assert(res.nextCheck.isEmpty)

    Mockito
      .verify(parsingClient)
      .getPhotos(eqq(offer.getOfferID), eqq("http://example.com"), eqq(Seq("phone1", "phone2")))(?)
  }

  ("process: exception") in new Fixture {
    when(parsingClient.getPhotos(?, ?, ?)(?)).thenThrow(new RuntimeException("error!"))
    val offer = TestUtils.createOffer(category = Category.TRUCKS)
    offer.getOfferAutoruBuilder.getSourceInfoBuilder.setParseUrl("http://example.com")

    val res = worker.process(offer.build(), None)
    assert(!offer.getOfferAutoru.getParsingInfo.getPhotosUploaded)
    assert(res.updateOfferFunc.isEmpty)
    assert(res.nextCheck.get.isAfter(new DateTime()))
  }

  ("process: incorrect url") in new Fixture {
    Mockito.reset(parsingClient)
    val remoteUrl = "http://example.com"
    when(parsingClient.getPhotos(?, ?, ?)(?)).thenReturn(ParsedPhotosResponse.IncorrectUrl(remoteUrl))
    val offer = TestUtils.createOffer(category = Category.CARS)
    offer.getOfferAutoruBuilder.getSourceInfoBuilder.setParseUrl(remoteUrl)

    val res = worker.process(offer.build(), None)
    val newOffer = res.updateOfferFunc.get(offer.build())
    assert(newOffer.getOfferAutoru.getParsingInfo.getPhotosUploaded)
    assert(res.nextCheck.isEmpty)
  }

  ("process: regions with disabpled photos upload") in new Fixture {
    Mockito.reset(parsingClient)
    val remoteUrl = "http://example.com"
    val offer = TestUtils.createOffer(category = Category.CARS)
    when(parsingClient.getPhotos(?, ?, ?)(?)).thenReturn(ParsedPhotosResponse.Photos(Seq("photo1", "photo2")))
    offer.getOfferAutoruBuilder.getSourceInfoBuilder.setParseUrl(remoteUrl)
    for {
      geoId <- Seq(21930, 21931, 98932, 98933, 98934, 98935, 98936, 98937, 98938, 98939, 98940, 98941, 98942, 98943)
    } {
      offer.getOfferAutoruBuilder.getSellerBuilder.getPlaceBuilder.setGeobaseId(geoId)
      val res = worker.process(offer.build(), None)
      val newOffer = res.updateOfferFunc.get(offer.build())
      assert(newOffer.getOfferAutoru.getParsingInfo.getPhotosUploaded)
      assert(newOffer.getOfferAutoru.getPhotoCount == 0)
      assert(res.nextCheck.isEmpty)
    }
  }

  ("process: upload as deleted") in new Fixture {
    Mockito.reset(parsingClient)
    featureRegistry.updateFeature(featuresManager.UploadParsedPhotosDeleted.name, true)
    val remoteUrl = "http://example.com"
    val offer = TestUtils.createOffer(category = Category.CARS)
    when(parsingClient.getPhotos(?, ?, ?)(?)).thenReturn(ParsedPhotosResponse.Photos(Seq("photo1", "photo2")))
    stub(parsingClient.mdsName(_: Category, _: String, _: String, _: String)(_: Traced)) {
      case (_, _, "photo1", namepsace, _) =>
        MdsIdSuccess(MdsPhotoData(namepsace, "3333-mdsphoto1"))
      case (_, _, "photo2", namespace, _) =>
        MdsIdSuccess(MdsPhotoData(namespace, "4444-mdsphoto2"))
    }
    offer.getOfferAutoruBuilder.getSourceInfoBuilder.setParseUrl(remoteUrl)
    val res = worker.process(offer.build(), None)
    val newOffer = res.updateOfferFunc.get(offer.build())
    assert(newOffer.getOfferAutoru.getParsingInfo.getPhotosUploaded)
    assert(newOffer.getOfferAutoru.getPhotoCount == 2)
    assert(newOffer.getOfferAutoru.getPhoto(0).getDeleted)
    assert(newOffer.getOfferAutoru.getPhoto(0).getDeletedTimestamp > 0)
    assert(newOffer.getOfferAutoru.getPhoto(1).getDeleted)
    assert(newOffer.getOfferAutoru.getPhoto(1).getDeletedTimestamp > 0)
    assert(res.nextCheck.isEmpty)
    featureRegistry.updateFeature(featuresManager.UploadParsedPhotosDeleted.name, false)
  }

  ("process: regions with disabled photos upload, upload as deleted") in new Fixture {
    Mockito.reset(parsingClient)
    featureRegistry.updateFeature(featuresManager.UploadParsedPhotosDeleted.name, true)
    val remoteUrl = "http://example.com"
    val offer = TestUtils.createOffer(category = Category.CARS)
    when(parsingClient.getPhotos(?, ?, ?)(?)).thenReturn(ParsedPhotosResponse.Photos(Seq("photo1", "photo2")))
    stub(parsingClient.mdsName(_: Category, _: String, _: String, _: String)(_: Traced)) {
      case (_, _, "photo1", namepsace, _) =>
        MdsIdSuccess(MdsPhotoData(namepsace, "3333-mdsphoto1"))
      case (_, _, "photo2", namepsace, _) =>
        MdsIdSuccess(MdsPhotoData(namepsace, "4444-mdsphoto2"))
    }
    offer.getOfferAutoruBuilder.getSourceInfoBuilder.setParseUrl(remoteUrl)
    for {
      geoId <- Seq(21930, 21931, 98932, 98933, 98934, 98935, 98936, 98937, 98938, 98939, 98940, 98941, 98942, 98943)
    } {
      offer.getOfferAutoruBuilder.getSellerBuilder.getPlaceBuilder.setGeobaseId(geoId)
      val res = worker.process(offer.build(), None)
      val newOffer = res.updateOfferFunc.get(offer.build())
      assert(newOffer.getOfferAutoru.getParsingInfo.getPhotosUploaded)
      assert(newOffer.getOfferAutoru.getPhotoCount == 2)
      assert(newOffer.getOfferAutoru.getPhoto(0).getDeleted)
      assert(newOffer.getOfferAutoru.getPhoto(0).getDeletedTimestamp > 0)
      assert(newOffer.getOfferAutoru.getPhoto(1).getDeleted)
      assert(newOffer.getOfferAutoru.getPhoto(1).getDeletedTimestamp > 0)
      assert(res.nextCheck.isEmpty)
    }
    featureRegistry.updateFeature(featuresManager.UploadParsedPhotosDeleted.name, false)
  }

}
