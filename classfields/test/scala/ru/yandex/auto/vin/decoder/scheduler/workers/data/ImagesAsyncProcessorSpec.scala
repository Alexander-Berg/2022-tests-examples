package ru.yandex.auto.vin.decoder.scheduler.workers.data

import auto.carfax.common.clients.cv.ComputerVisionClient
import auto.carfax.common.utils.tracing.Traced
import com.google.common.util.concurrent.RateLimiter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.auto.api.internal.Mds.MdsPhotoInfo
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.manager.validation.download.AsyncUrlDownloader
import ru.yandex.auto.vin.decoder.model.LicensePlate
import ru.yandex.auto.vin.decoder.proto.CommonModels.PhotoInfo
import ru.yandex.auto.vin.decoder.proto.CommonModels.PhotoInfo.MetaData.{
  CoordinatePoint,
  Coordinates,
  RecognizedLicensePlates,
  RecognizedObject
}
import ru.yandex.auto.vin.decoder.proto.VinHistory.{PhotoEvent, VinInfoHistory}
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageModel.{MetaData, OnlyPreparedModel, PreparedData}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global

class ImagesAsyncProcessorSpec extends AnyWordSpecLike with Matchers with MockitoSupport {

  implicit val t: Traced = Traced.empty

  val lp = LicensePlate("E123KX799")

  val coordinates = Coordinates
    .newBuilder()
    .setTopLeft(CoordinatePoint.newBuilder().setX(0.5).setY(0.6).build())
    .setTopRight(CoordinatePoint.newBuilder().setX(0.7).setY(0.6).build())
    .setBottomRight(CoordinatePoint.newBuilder().setX(0.7).setY(0.8).build())
    .setBottomLeft(CoordinatePoint.newBuilder().setX(0.5).setY(0.8).build())
    .build()

  val partlyMdsPhotoInfo =
    MdsPhotoInfo.newBuilder().setGroupId(123).setName("partly").setNamespace("autoru-carfax-stored").build

  val storedMdsPhotoInfo =
    MdsPhotoInfo.newBuilder().setGroupId(123).setName("stored").setNamespace("autoru-carfax-stored").build

  val storedMeta = PhotoInfo.MetaData
    .newBuilder()
    .addRecognizedLicensePlates(
      RecognizedLicensePlates
        .newBuilder()
        .setConfidence(1.0)
        .setLicensePlate(lp.toString)
        .setWidthPercent(1.0)
        .build
    )
    .addRecognizedObjects(
      RecognizedObject
        .newBuilder()
        .setCoordinates(coordinates)
        .setConfidence(0.9)
        .setMark("MAZDA")
        .setModel("6")
        .build()
    )
    .build

  val noLpMeta = PhotoInfo.MetaData
    .newBuilder()
    .setNoRecognizedLp(true)
    .addRecognizedObjects(
      RecognizedObject
        .newBuilder()
        .setCoordinates(coordinates)
        .setConfidence(0.9)
        .setMark("MAZDA")
        .setModel("6")
        .build()
    )
    .build

  val notRecognizedObjectsMeta = PhotoInfo.MetaData
    .newBuilder()
    .addRecognizedLicensePlates(
      RecognizedLicensePlates
        .newBuilder()
        .setConfidence(1.0)
        .setLicensePlate(lp.toString)
        .setWidthPercent(1.0)
        .build
    )
    .build

  val noObjectsMeta = PhotoInfo.MetaData
    .newBuilder()
    .addRecognizedLicensePlates(
      RecognizedLicensePlates
        .newBuilder()
        .setConfidence(1.0)
        .setLicensePlate(lp.toString)
        .setWidthPercent(1.0)
        .build
    )
    .setNoRecognizedObjects(true)
    .build

  val notRecognizedMarkModelMeta = PhotoInfo.MetaData
    .newBuilder()
    .addRecognizedLicensePlates(
      RecognizedLicensePlates
        .newBuilder()
        .setConfidence(1.0)
        .setLicensePlate(lp.toString)
        .setWidthPercent(1.0)
        .build
    )
    .addRecognizedObjects(
      RecognizedObject
        .newBuilder()
        .setCoordinates(coordinates)
        .build()
    )
    .build

  val noMarkModelMeta = PhotoInfo.MetaData
    .newBuilder()
    .addRecognizedLicensePlates(
      RecognizedLicensePlates
        .newBuilder()
        .setConfidence(1.0)
        .setLicensePlate(lp.toString)
        .setWidthPercent(1.0)
        .build
    )
    .addRecognizedObjects(
      RecognizedObject
        .newBuilder()
        .setCoordinates(coordinates)
        .setNoMarkModel(true)
        .build()
    )
    .build

  val processedPhotoInfo =
    PhotoInfo
      .newBuilder()
      .setExternalPhotoUrl("http://yandex.ru/stored_image.jpg")
      .setMdsPhotoInfo(partlyMdsPhotoInfo)
      .setMeta(storedMeta)
      .build

  val notFoundPhotoInfo =
    PhotoInfo
      .newBuilder()
      .setExternalPhotoUrl("http://yandex.ru/not_found_image.jpg")
      .setNotFound(true)
      .build

  val notRecognizedLpPhotoInfo =
    PhotoInfo
      .newBuilder()
      .setExternalPhotoUrl("http://yandex.ru/stored_image.jpg")
      .setMdsPhotoInfo(storedMdsPhotoInfo)
      .build

  val noLpPhotoInfo =
    PhotoInfo
      .newBuilder()
      .setExternalPhotoUrl("http://yandex.ru/stored_image.jpg")
      .setMeta(noLpMeta)
      .build

  val notDetectedObjectsPhotoInfo =
    PhotoInfo
      .newBuilder()
      .setExternalPhotoUrl("http://yandex.ru/stored_image.jpg")
      .setMeta(notRecognizedObjectsMeta)
      .build

  val noObjectsPhotoInfo =
    PhotoInfo
      .newBuilder()
      .setExternalPhotoUrl("http://yandex.ru/stored_image.jpg")
      .setMeta(noObjectsMeta)
      .build

  val notRecognizedMarkModelPhotoInfo =
    PhotoInfo
      .newBuilder()
      .setExternalPhotoUrl("http://yandex.ru/stored_image.jpg")
      .setMeta(notRecognizedMarkModelMeta)
      .build

  val noMarkModelPhotoInfo =
    PhotoInfo
      .newBuilder()
      .setExternalPhotoUrl("http://yandex.ru/stored_image.jpg")
      .setMeta(noMarkModelMeta)
      .build

  val photoInfo =
    PhotoInfo
      .newBuilder()
      .setExternalPhotoUrl("http://yandex.ru/some_image.jpg")
      .build

  val recordNew = OnlyPreparedModel(
    lp,
    PreparedData(buildVihFrom(photoInfo)),
    MetaData("same", EventType.YANDEX_IMAGES, "", 100, 100, 100)
  )

  val recordOld = OnlyPreparedModel(
    lp,
    PreparedData(buildVihFrom(photoInfo)),
    MetaData("same", EventType.YANDEX_IMAGES, "", 0, 0, 0)
  )

  val recordAnother = OnlyPreparedModel(
    lp,
    PreparedData(buildVihFrom(processedPhotoInfo)),
    MetaData("another", EventType.YANDEX_IMAGES, "", 50, 50, 50)
  )

  val rawStorageManager = mock[RawStorageManager[LicensePlate]]
  val avatarsManager = mock[AvatarsPhotoManager]
  val computerVisionClient = mock[ComputerVisionClient]
  val downloader = mock[AsyncUrlDownloader]
  val rateLimiter = mock[RateLimiter]
  val computerVisionManager = new ComputerVisionManager(avatarsManager, computerVisionClient, rateLimiter)

  val processor =
    new ImagesAsyncProcessor(rawStorageManager, avatarsManager, computerVisionManager)

  val yaImagesProcessor = new YaImagesAsyncProcessor(rawStorageManager, computerVisionManager, downloader)

  def buildVihFrom(photo: PhotoInfo) = {
    VinInfoHistory.newBuilder().addPhotoEvents(PhotoEvent.newBuilder().addImages(photo)).build
  }

  "ImagesAsyncProcessor" should {

    "correctly get photos from records" in {
      val records = List(recordNew, recordOld, recordAnother)
      val photos = processor.getAllPhotosFrom(records)

      photos shouldBe List(photoInfo, photoInfo, processedPhotoInfo)
    }

    "get correct should process if photo is not processed yet" in {
      val res = processor.shouldProcessPhoto(photoInfo)
      val yaRes = yaImagesProcessor.shouldProcessPhoto(photoInfo)

      res shouldBe true
      yaRes shouldBe true
    }

    "get correct should process if photo is already processed" in {
      val res = processor.shouldProcessPhoto(processedPhotoInfo)
      val yaRes = yaImagesProcessor.shouldProcessPhoto(processedPhotoInfo)

      res shouldBe false
      yaRes shouldBe false
    }

    "get correct should process if photo is already processed but not found" in {
      val res = processor.shouldProcessPhoto(notFoundPhotoInfo)
      val yaRes = yaImagesProcessor.shouldProcessPhoto(notFoundPhotoInfo)

      res shouldBe false
      yaRes shouldBe false
    }

    "get correct should process if photo not recognized lp" in {
      val res = processor.shouldProcessPhoto(notRecognizedLpPhotoInfo)
      val yaRes = yaImagesProcessor.shouldProcessPhoto(notRecognizedLpPhotoInfo)

      res shouldBe true
      yaRes shouldBe true
    }

    "get correct should process if photo is already processed but there is no recognized LP" in {
      val res = processor.shouldProcessPhoto(noLpPhotoInfo)
      val yaRes = yaImagesProcessor.shouldProcessPhoto(noLpPhotoInfo)

      res shouldBe false
      yaRes shouldBe false
    }

    "get correct should process if photo not detected objects" in {
      val res = processor.shouldProcessPhoto(notDetectedObjectsPhotoInfo)
      val yaRes = yaImagesProcessor.shouldProcessPhoto(notDetectedObjectsPhotoInfo)

      res shouldBe true
      yaRes shouldBe true
    }

    "get correct should process if photo is already processed but there is no detected objects" in {
      val res = processor.shouldProcessPhoto(noObjectsPhotoInfo)
      val yaRes = yaImagesProcessor.shouldProcessPhoto(noObjectsPhotoInfo)

      res shouldBe false
      yaRes shouldBe false
    }
    "get correct should process if photo not recognized mark-model" in {
      val res = processor.shouldProcessPhoto(notRecognizedMarkModelPhotoInfo)
      val yaRes = yaImagesProcessor.shouldProcessPhoto(notRecognizedMarkModelPhotoInfo)

      res shouldBe true
      yaRes shouldBe true
    }

    "get correct should process if photo is already processed but there is no recognized mark-model" in {
      val res = processor.shouldProcessPhoto(noMarkModelPhotoInfo)
      val yaRes = yaImagesProcessor.shouldProcessPhoto(noMarkModelPhotoInfo)

      res shouldBe false
      yaRes shouldBe false
    }

  }
}
