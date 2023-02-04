package ru.yandex.auto.vin.decoder.raw.images

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.LicensePlate
import ru.yandex.auto.vin.decoder.proto.CommonModels.PhotoInfo
import ru.yandex.auto.vin.decoder.proto.CommonModels.PhotoInfo.MetaData
import ru.yandex.auto.vin.decoder.proto.CommonModels.PhotoInfo.MetaData.RecognizedFactor
import ru.yandex.auto.vin.decoder.proto.VinHistory.{PhotoEvent, VinInfoHistory}
import ru.yandex.auto.vin.decoder.raw.images.model.{YaImage, YaImageRawModel}
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils
import ru.yandex.auto.vin.decoder.yt.diff.DbActions.Delete

import scala.jdk.CollectionConverters.IterableHasAsJava

class YaImageRawModelManagerSpec extends AnyWordSpecLike with Matchers {

  import ru.yandex.auto.vin.decoder.raw.images.YaImageRawModelManagerSpec._

  val manager = new YaImagesRawModelManager

  "YaImagesRawModelManager" should {

    "correctly parse raw json" in {
      val expected = YaImageRawModel(
        Json.parse(correctYaImageJson).toString,
        LicensePlate.apply("A001MP799"),
        "38981439",
        correctYaImage
      )
      val model = manager.parse(correctYaImageJson, "", "").toOption.get

      model shouldBe expected
    }

    "correctly convert raw model to VinInfoHistory" in {
      val vh = VinInfoHistory
        .newBuilder()
        .setLicensePlate("A001MP799")
        .setEventType(EventType.YANDEX_IMAGES)
        .setStatus(VinInfoHistory.Status.OK)
        .setGroupId("38981439")
        .addPhotoEvents(
          PhotoEvent
            .newBuilder()
            .addImages(
              PhotoInfo.newBuilder
                .setExternalPhotoUrl("http://picture.com")
                .setMeta(
                  MetaData
                    .newBuilder()
                    .addAllRecognizedFactors(
                      List(
                        RecognizedFactor.newBuilder().setFactor("porno4").setValue(1.0).build,
                        RecognizedFactor.newBuilder().setFactor("childPornProbability").setValue(0.0234456567).build(),
                        RecognizedFactor.newBuilder().setFactor("gruesomeCombined").setValue(0.000324534278).build()
                      ).asJava
                    )
                )
            )
        )
        .build()

      val raw = ResourceUtils.getStringFromResources("/images/ya_image.json")
      val data =
        YaImage(
          LicensePlate.apply("A001MP799"),
          "http://picture.com",
          1,
          0.0234456567,
          0.000324534278,
          deleted = false
        )
      val model = YaImageRawModel(raw, LicensePlate.apply("A001MP799"), "38981439", data)

      val converted = manager.convert(model).await

      converted shouldBe vh
    }

    "buildDeleted" should {
      "build deleted ya image" in {
        val deleteAction = Delete("", "", 1L, 1L, 1L, deletedYaImageJson)
        val expected = YaImageRawModel(
          Json.parse(deletedYaImageJson).toString,
          LicensePlate.apply("A001MP799"),
          "38981439",
          deletedYaImage
        )

        manager.buildDeleted(deleteAction) should be(expected)
      }
    }

    "alreadyDeleted" should {
      "return false for not deleted ya image" in {
        manager.alreadyDeleted(correctYaImageJson) should be(false)
      }

      "return true for deleted ya image" in {
        manager.alreadyDeleted(deletedYaImageJson) should be(true)
      }
    }
  }
}

object YaImageRawModelManagerSpec {
  val correctYaImageJson: String = ResourceUtils.getStringFromResources("/images/ya_image.json")

  val correctYaImage: YaImage =
    YaImage(LicensePlate.apply("A001MP799"), "http://picture.com", 1, 0.0234456567, 0.000324534278, false)

  val deletedYaImageJson: String = ResourceUtils.getStringFromResources("/images/ya_image_deleted.json")

  val deletedYaImage: YaImage =
    YaImage(
      LicensePlate.apply("A001MP799"),
      "http://picture.com",
      1,
      0.0234456567,
      0.000324534278,
      deleted = true
    )
}
