package ru.yandex.auto.garage.scheduler.stage

import auto.carfax.common.clients.cv.{LicensePlatesBlurManager, RequestEntityTooLargeException}
import org.mockito.Mockito.{reset, times, verify}
import org.scalatest.BeforeAndAfter
import org.scalatest.wordspec.AnyWordSpecLike
import auto.carfax.common.clients.cv.LicensePlatesBlurManager.BlurResult
import auto.carfax.common.utils.avatars.PhotoInfoId
import ru.yandex.auto.vin.decoder.garage.proto.GarageSchema.GarageCard
import ru.yandex.auto.vin.decoder.proto.CommonModels.PhotoInfo
import ru.yandex.auto.vin.decoder.proto.CommonModels.PhotoInfo.MetaData.RecognizedLicensePlates
import ru.yandex.auto.vin.decoder.proto.CommonModels.PhotoInfo.PhotoTransformation
import ru.yandex.auto.vin.decoder.scheduler.engine.ProcessingState
import ru.yandex.auto.vin.decoder.scheduler.models.{DefaultDelay, WatchingStateUpdate}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.jdk.CollectionConverters.{IterableHasAsJava, ListHasAsScala}

class LicensePlateBlurStageTest
  extends AnyWordSpecLike
  with MockitoSupport
  with BeforeAndAfter
  with GarageCardStageSupport[LicensePlateBlurStage] {

  private val manager = mock[LicensePlatesBlurManager]
  private val stage = createProcessingStage()

  before {
    reset(manager)
  }

  "stage.should_process" should {
    "return false" when {
      "no images" in {
        val card = buildCard(List.empty)
        val state = createProcessingState(card)
        stage.shouldProcess(state) shouldBe false
      }
      "all images already blurred" in {
        val images = List(
          buildBlurredImage(PhotoInfoId(1, "orig1", "autoru-carfax"), PhotoInfoId(2, "blurred1", "autoru-carfax")),
          buildBlurredImage(PhotoInfoId(3, "orig2", "autoru-carfax"), PhotoInfoId(4, "blurred2", "autoru-carfax"))
        )

        val card = buildCard(images)
        val state = createProcessingState(card)
        stage.shouldProcess(state) shouldBe false
      }
      "all images already blurred or deleted" in {
        val images = List(
          buildBlurredImage(PhotoInfoId(1, "orig1", "autoru-carfax"), PhotoInfoId(2, "blurred1", "autoru-carfax")),
          buildDeletedImage(PhotoInfoId(3, "orig2", "autoru-carfax"))
        )

        val card = buildCard(images)
        val state = createProcessingState(card)
        stage.shouldProcess(state) shouldBe false
      }
    }
    "return true" when {
      "there are not blurred images" in {
        val images = List(
          buildBlurredImage(PhotoInfoId(1, "orig1", "autoru-carfax"), PhotoInfoId(2, "blurred1", "autoru-carfax")),
          buildNotBlurredImage(PhotoInfoId(3, "orig2", "autoru-carfax"))
        )

        val card = buildCard(images)
        val state = createProcessingState(card)
        stage.shouldProcess(state) shouldBe true
      }
    }
  }

  "stage.process" should {
    "blur images" in {
      val blurredImage1 =
        buildBlurredImage(PhotoInfoId(1, "orig1", "autoru-carfax"), PhotoInfoId(1, "blurred1", "autoru-carfax"))
      val notBlurredImage2 = buildNotBlurredImage(PhotoInfoId(2, "orig2", "autoru-carfax"))
      val notBlurredImage3 = buildNotBlurredImage(PhotoInfoId(3, "orig3", "autoru-carfax"))
      val notBlurredImage4 = buildNotBlurredImage(PhotoInfoId(4, "orig4", "autoru-carfax"))

      val blurredPhotoInfo3 = PhotoInfoId(3, "blurred3", "autoru-carfax")
      val recognizedInfo = List(
        RecognizedLicensePlates
          .newBuilder()
          .setLicensePlate("A123AA24")
          .setConfidence(0.55)
          .setWidthPercent(0.12)
          .build(),
        RecognizedLicensePlates
          .newBuilder()
          .setLicensePlate("T555AB77")
          .setConfidence(0.9)
          .setWidthPercent(0.52)
          .build()
      )

      val images = List(blurredImage1, notBlurredImage2, notBlurredImage3, notBlurredImage4)

      val card = buildCard(images)
      val state = createProcessingState(card)

      when(manager.blur(eq(PhotoInfoId.from(notBlurredImage2.getMdsPhotoInfo)))(?)).thenReturn(
        Future.failed(new RuntimeException("some error"))
      )

      when(manager.blur(eq(PhotoInfoId.from(notBlurredImage3.getMdsPhotoInfo)))(?)).thenReturn(
        Future.successful(
          Right(
            BlurResult(
              PhotoInfoId.toMdsInfo(blurredPhotoInfo3),
              recognizedInfo
            )
          )
        )
      )

      when(manager.blur(eq(PhotoInfoId.from(notBlurredImage4.getMdsPhotoInfo)))(?)).thenReturn(
        Future.successful(
          Left(RequestEntityTooLargeException("op", "req"))
        )
      )

      val updatedImages = stage.processWithAsync(0, state).state.getVehicleInfo.getImagesList.asScala
      updatedImages(0) shouldBe blurredImage1
      updatedImages(1) shouldBe notBlurredImage2

      updatedImages(2).getMdsPhotoInfo shouldBe notBlurredImage3.getMdsPhotoInfo
      updatedImages(2).getTransformationsCount == 1
      updatedImages(2).getTransformations(0).getBlurredPlates shouldBe true
      updatedImages(2).getTransformations(0).getMdsPhotoInfo shouldBe PhotoInfoId.toMdsInfo(blurredPhotoInfo3)
      updatedImages(2).getMeta.getRecognizedLicensePlatesList.asScala shouldBe recognizedInfo

      updatedImages(3).getMdsPhotoInfo shouldBe notBlurredImage4.getMdsPhotoInfo
      updatedImages(3).getIsDeleted shouldBe true

      verify(manager, times(1)).blur(eq(PhotoInfoId.from(notBlurredImage2.getMdsPhotoInfo)))(?)
      verify(manager, times(1)).blur(eq(PhotoInfoId.from(notBlurredImage3.getMdsPhotoInfo)))(?)
      verify(manager, times(1)).blur(eq(PhotoInfoId.from(notBlurredImage4.getMdsPhotoInfo)))(?)
    }
  }

  private def buildCard(images: List[PhotoInfo]): GarageCard = {
    val builder = GarageCard.newBuilder()
    builder.getVehicleInfoBuilder.addAllImages(images.asJava)
    builder.build()
  }

  private def buildBlurredImage(orig: PhotoInfoId, blurred: PhotoInfoId) = {
    PhotoInfo
      .newBuilder()
      .setMdsPhotoInfo(PhotoInfoId.toMdsInfo(orig))
      .addTransformations(
        PhotoTransformation.newBuilder().setBlurredPlates(true).setMdsPhotoInfo(PhotoInfoId.toMdsInfo(blurred))
      )
      .build()
  }

  private def buildNotBlurredImage(orig: PhotoInfoId): PhotoInfo = {
    PhotoInfo
      .newBuilder()
      .setMdsPhotoInfo(PhotoInfoId.toMdsInfo(orig))
      .build()
  }

  private def buildDeletedImage(orig: PhotoInfoId): PhotoInfo = {
    PhotoInfo
      .newBuilder()
      .setMdsPhotoInfo(PhotoInfoId.toMdsInfo(orig))
      .setIsDeleted(true)
      .build()
  }

  private def createProcessingState(card: GarageCard): ProcessingState[GarageCard] = {
    ProcessingState(WatchingStateUpdate(card, DefaultDelay(25.hours)))
  }

  override def createProcessingStage(): LicensePlateBlurStage = new LicensePlateBlurStage(manager)
}
