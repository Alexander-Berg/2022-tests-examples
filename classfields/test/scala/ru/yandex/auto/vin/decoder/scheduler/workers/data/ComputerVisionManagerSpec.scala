package ru.yandex.auto.vin.decoder.scheduler.workers.data

import auto.carfax.common.clients.avatars.{AvatarsImageInfoResponse, OrigSize}
import com.google.common.util.concurrent.RateLimiter
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import auto.carfax.common.clients.avatars.OrigSize
import auto.carfax.common.clients.cv.{ComputerVisionClient, EmptyResponseException}
import auto.carfax.common.clients.cv.model.Crop
import auto.carfax.common.clients.cv.model.DetectObjectsResponseModels.{DetectObjectsResponse, DetectedObject}
import auto.carfax.common.clients.cv.model.RecognizeMarkModelResponseModels.{Prediction, RecognizeMarkModelResponse}
import auto.carfax.common.clients.cv.model.RecognizeResponseModels.{RecognizeResponse, RecognizedPlatesResponse}
import auto.carfax.common.clients.cv.EmptyResponseException
import auto.carfax.common.utils.tracing.Traced
import ru.yandex.auto.vin.decoder.model.LicensePlate
import ru.yandex.auto.vin.decoder.proto.CommonModels.PhotoInfo
import ru.yandex.auto.vin.decoder.proto.CommonModels.PhotoInfo.MetaData
import ru.yandex.auto.vin.decoder.proto.CommonModels.PhotoInfo.MetaData.{
  CoordinatePoint,
  Coordinates,
  RecognizedLicensePlates,
  RecognizedObject
}
import ru.yandex.auto.vin.decoder.scheduler.workers.data.ComputerVisionManager.autoClassId
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ComputerVisionManagerSpec extends AnyWordSpecLike with Matchers with MockitoSupport {

  implicit val t: Traced = Traced.empty

  val lp = LicensePlate("E123KX799")

  val photoInfo = {
    val b = PhotoInfo
      .newBuilder()
      .setExternalPhotoUrl("http://yandex.ru/some_image.jpg")
    b.getMdsPhotoInfoBuilder.setName("img_name").setGroupId(1)
    b.build
  }

  val avatarsPhotoManager = mock[AvatarsPhotoManager]
  val computerVisionClient = mock[ComputerVisionClient]
  val rateLimiter = mock[RateLimiter]

  val detectedObject1 = DetectedObject(autoClassId, Crop(0.5, 0.6, 0.7, 0.8))
  val detectedObject2 = DetectedObject(autoClassId + 1, Crop(0.6, 0.7, 0.8, 0.9))
  val detectObjectsResponse = DetectObjectsResponse(List(detectedObject1, detectedObject2))

  val prediction1 = Prediction(0.9, "PORSCHE", "BOXSTER", Some("III (981)"), Some("Cabri"))
  val prediction2 = Prediction(0.5, "PORSCHE", "CAYMAN", Some("II (981)"), None)
  val recognizeMMResponse = RecognizeMarkModelResponse(List(prediction1, prediction2))

  val validRecognizedLp =
    RecognizedPlatesResponse(
      1.0,
      lp.toString,
      0.2,
      Array(Array(334, 479), Array(334, 443), Array(511, 445), Array(511, 481))
    )
  val invalidRecognizedLp = RecognizedPlatesResponse(0.1, "invalid_lp", 0.1, Array.empty)
  val recognizeLpResponse = RecognizeResponse(List(validRecognizedLp, invalidRecognizedLp))

  val avatarsImageInfoResponse = AvatarsImageInfoResponse(123, "image", OrigSize(1000L, 1000L), Map.empty)

  when(rateLimiter.tryAcquire(?, ?, ?)).thenReturn(true)

  when(computerVisionClient.detectObjects(?)(?)).thenReturn(Future.successful(detectObjectsResponse))
  when(computerVisionClient.recognizeMarkModel(?, ?)(?)).thenReturn(Future.successful(recognizeMMResponse))
  when(computerVisionClient.recognizePlates(?)(?)).thenReturn(Future.successful(recognizeLpResponse))

  when(avatarsPhotoManager.getOrigImage(?, ?)(?)).thenReturn(Future.successful(null))
  when(avatarsPhotoManager.getImageInfo(?, ?)(?)).thenReturn(Future.successful(avatarsImageInfoResponse))

  val manager = new ComputerVisionManager(avatarsPhotoManager, computerVisionClient, rateLimiter)

  "ComputerVisionManager" should {

    "correctly recognize mark model" in {

      val expected =
        PhotoInfo.newBuilder
          .setExternalPhotoUrl("http://yandex.ru/some_image.jpg")
          .setMdsPhotoInfo(photoInfo.getMdsPhotoInfo)
          .setMeta(
            MetaData.newBuilder
              .addRecognizedObjects(
                RecognizedObject
                  .newBuilder()
                  .setMark("PORSCHE")
                  .setModel("BOXSTER")
                  .setGeneration("III (981)")
                  .setBodyType("Cabri")
                  .setConfidence(0.9)
                  .setCoordinates(
                    Coordinates.newBuilder
                      .setTopLeft(CoordinatePoint.newBuilder.setX(0.5).setY(0.6).build)
                      .setTopRight(CoordinatePoint.newBuilder.setX(0.7).setY(0.6).build)
                      .setBottomRight(CoordinatePoint.newBuilder.setX(0.7).setY(0.8).build)
                      .setBottomLeft(CoordinatePoint.newBuilder.setX(0.5).setY(0.8).build)
                      .build
                  )
                  .build
              )
              .build
          )
          .build

      val res = manager.tryRecognizeMarkModel(photoInfo, None).futureValue

      res.get shouldBe expected
    }

    "correctly recognize license plate" in {

      val expected =
        PhotoInfo.newBuilder
          .setExternalPhotoUrl("http://yandex.ru/some_image.jpg")
          .setMdsPhotoInfo(photoInfo.getMdsPhotoInfo)
          .setMeta(
            MetaData.newBuilder
              .addRecognizedLicensePlates(
                RecognizedLicensePlates
                  .newBuilder()
                  .setConfidence(1.0)
                  .setLicensePlate(lp.toString)
                  .setWidthPercent(0.2)
                  .setCoordinates(
                    Coordinates
                      .newBuilder()
                      .setTopLeft(CoordinatePoint.newBuilder().setX(0.334).setY(0.443).build())
                      .setTopRight(CoordinatePoint.newBuilder().setX(0.511).setY(0.445).build())
                      .setBottomRight(CoordinatePoint.newBuilder().setX(0.511).setY(0.481).build())
                      .setBottomLeft(CoordinatePoint.newBuilder().setX(0.334).setY(0.479).build())
                      .build()
                  )
                  .build
              )
              .build
          )
          .build

      val res = manager.tryRecognizeLicensePlates(photoInfo, None).futureValue

      res.get shouldBe expected
    }

    "do not return uncorrect recognized licenseplates" in {

      val recognizedModel = RecognizedLicensePlates
        .newBuilder()
        .setConfidence(1.0)
        .setLicensePlate(lp.toString)
        .setWidthPercent(0.2)
        .setCoordinates(
          Coordinates
            .newBuilder()
            .setTopLeft(CoordinatePoint.newBuilder().setX(0.334).setY(0.443).build())
            .setTopRight(CoordinatePoint.newBuilder().setX(0.511).setY(0.445).build())
            .setBottomRight(CoordinatePoint.newBuilder().setX(0.511).setY(0.481).build())
            .setBottomLeft(CoordinatePoint.newBuilder().setX(0.334).setY(0.479).build())
            .build()
        )
        .build

      val res = manager.recognizeLicensePlates(photoInfo, None).futureValue

      res.length shouldBe 1
      res.head shouldBe recognizedModel
    }

    "set no recognized lp = true when response is empty" in {
      when(computerVisionClient.recognizePlates(?)(?)).thenThrow(EmptyResponseException("recognizePlates", ""))

      val res = manager.tryRecognizeLicensePlates(photoInfo, None).futureValue

      res.get.getMeta.getNoRecognizedLp shouldBe true
      res.get.getMeta.getNoRecognizedObjects shouldBe true
    }
  }
}
