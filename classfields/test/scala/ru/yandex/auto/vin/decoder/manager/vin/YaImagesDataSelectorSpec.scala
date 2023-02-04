package ru.yandex.auto.vin.decoder.manager.vin

import org.scalatest.enablers.Emptiness.emptinessOfGenTraversable
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import ru.yandex.auto.vin.decoder.model.{LicensePlate, MockedFeatures}
import ru.yandex.auto.vin.decoder.proto.CommonModels.PhotoInfo
import ru.yandex.auto.vin.decoder.proto.CommonModels.PhotoInfo.MetaData
import ru.yandex.auto.vin.decoder.proto.CommonModels.PhotoInfo.MetaData.{RecognizedFactor, RecognizedLicensePlates}
import ru.yandex.auto.vin.decoder.proto.VinHistory.{PhotoEvent, VinInfoHistory}
import ru.yandex.auto.vin.decoder.report.processors.entities.photo.YaImageEntity
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager.Prepared
import ru.yandex.vertis.mockito.MockitoSupport

import scala.jdk.CollectionConverters.IterableHasAsJava

class YaImagesDataSelectorSpec extends AnyWordSpecLike with Matchers with MockitoSupport with MockedFeatures {

  val lp = LicensePlate("A102MP799")

  val buildRecognizeFactor = (factor: String, value: Double) =>
    RecognizedFactor.newBuilder().setFactor(factor).setValue(value).build()

  val buildVinInfoHistory =
    (uri: String, porno4: Int, childPornProbability: Double, gruesomeCombined: Double, isValidLp: Boolean) =>
      VinInfoHistory
        .newBuilder()
        .setEventType(YaImageEntity.eventType)
        .addPhotoEvents(
          PhotoEvent
            .newBuilder()
            .addImages(
              PhotoInfo
                .newBuilder()
                .setExternalPhotoUrl(uri)
                .setExistsInYaSearchResults(true)
                .setMeta(
                  MetaData
                    .newBuilder()
                    .addAllRecognizedFactors(
                      List(
                        buildRecognizeFactor("porno4", porno4),
                        buildRecognizeFactor("childPornProbability", childPornProbability),
                        buildRecognizeFactor("gruesomeCombined", gruesomeCombined)
                      ).asJava
                    )
                    .addRecognizedLicensePlates(
                      RecognizedLicensePlates
                        .newBuilder()
                        .setLicensePlate(if (isValidLp) lp.toString else "A100MP799")
                        .build()
                    )
                    .build()
                )
                .build
            )
            .build
        )
        .build()

  val valid = buildVinInfoHistory("http://some.valid.uri", 1, 0.49876454, 0.72345324, true)
  val fromAvtonomer = buildVinInfoHistory("http://avto-nomer.ru/some_image.jpg", 1, 0.45453, 0.7143564356, true)
  val fromAvtonomer2 = buildVinInfoHistory("http://platesmania.com/some_image.jpg", 1, 0.4936578, 0.719234, true)

  val fromMigalki = buildVinInfoHistory("http://migalki.net/some_image.jpg", 1, 0.4936578, 0.719234, true)

  val fromAutoRu =
    buildVinInfoHistory("http://avatars.mds.yandex.net/get-autoru-vos/some_image.jpg", 1, 0.43456, 0.034, true)

  val fromAutoRu2 =
    buildVinInfoHistory("http://avatars.mds.yandex.net/get-autoru-all/some_image.jpg", 1, 0.0023, 0.376, true)
  val invalidPorno4 = buildVinInfoHistory("http://some.valid.uri", 70, 0.235, 0.3456, true)
  val invalidChildPornProbability = buildVinInfoHistory("http://some.valid.uri", 0, 0.51, 0.456, true)
  val invalidgruesomeCombined = buildVinInfoHistory("http://some.valid.uri", 0, 0.41, 0.731, true)
  val invalidLp = buildVinInfoHistory("http://some.valid.uri", 0, 0.41, 0.731, false)

  val lpData = LpData(
    lp,
    Map(
      YaImageEntity.eventType -> List(
        Prepared(0, 0, 0, valid, "http://some.valid.uri".hashCode.toString),
        Prepared(0, 0, 0, fromAvtonomer, "http://avto-nomer.ru/some_image.jpg".hashCode.toString),
        Prepared(0, 0, 0, fromAvtonomer2, "http://platesmania.com/some_image.jpg".hashCode.toString),
        Prepared(0, 0, 0, fromMigalki, "http://migalki.net/some_image.jpg".hashCode.toString),
        Prepared(0, 0, 0, fromAutoRu, "http://avatars.mds.yandex.net/get-autoru-vos/some_image.jpg".hashCode.toString),
        Prepared(0, 0, 0, fromAutoRu2, "http://avatars.mds.yandex.net/get-autoru-all/some_image.jpg".hashCode.toString),
        Prepared(0, 0, 0, invalidPorno4, "http://some.valid.uri".hashCode.toString),
        Prepared(0, 0, 0, invalidChildPornProbability, "http://some.valid.uri".hashCode.toString),
        Prepared(0, 0, 0, invalidgruesomeCombined, "http://some.valid.uri".hashCode.toString),
        Prepared(0, 0, 0, invalidLp, "http://some.valid.uri".hashCode.toString)
      )
    )
  )

  "YaImagesDataSelector" should {

    "get correct images from lpData and filter incorrect" in {

      when(features.ShowYaImagesInReport).thenReturn(enabledFeature)
      val selector = new YaImagesDataSelector(features.ShowYaImagesInReport)

      val photos = selector.getPhotos(lpData)

      photos.length shouldBe 1
      photos.head shouldBe valid.getPhotoEventsList.get(0).getImagesList.get(0)
    }

    "do not get images from lpData if feature is disabled" in {

      when(features.ShowYaImagesInReport).thenReturn(disabledFeature)
      val selector = new YaImagesDataSelector(features.ShowYaImagesInReport)

      val photos = selector.getPhotos(lpData)

      photos shouldBe empty
    }
  }
}
