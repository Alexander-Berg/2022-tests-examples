package ru.yandex.vertis.moderation.plate

import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.model.generators.CoreGenerators._
import ru.yandex.vertis.moderation.model.instance.{AutoruEssentials, UpdateJournalRecord}
import ru.yandex.vertis.moderation.model.generators.Producer.generatorAsProducer
import ru.yandex.vertis.moderation.proto.Model.Service

class LicensePlateMismatchDeciderImplSpec extends SpecBase {

  "LicensePlateMismatchDeciderImpl.checkEqualsOrAbsent" should {
    "return true if plates are not present " in {
      LicensePlateMismatchDeciderImpl.checkEqualsOrAbsent(
        newEssentials(plate = None, plateOnPhoto = None)
      ) shouldBe true
      LicensePlateMismatchDeciderImpl.checkEqualsOrAbsent(
        newEssentials(plate = Some("123"), plateOnPhoto = None)
      ) shouldBe true
      LicensePlateMismatchDeciderImpl.checkEqualsOrAbsent(
        newEssentials(plate = None, plateOnPhoto = Some("123"))
      ) shouldBe true
    }

    "return false if plates are not equals" in {
      LicensePlateMismatchDeciderImpl.checkEqualsOrAbsent(
        newEssentials(plate = Some("123"), plateOnPhoto = Some("456"))
      ) shouldBe false
    }

    "return true if plates are equals" in {
      LicensePlateMismatchDeciderImpl.checkEqualsOrAbsent(
        newEssentials(plate = Some("AB123CE"), plateOnPhoto = Some("AB123CE"))
      ) shouldBe true
    }

    "return true if plates are equals after transformation" in {
      val cyrillic = "А1В2Е3К4М5Н6О7Р8С9Т0УХ"
      val latin = "A1B2E3K4M5H6O7P8C9T0YX"
      LicensePlateMismatchDeciderImpl.checkEqualsOrAbsent(
        newEssentials(plate = Some(cyrillic), plateOnPhoto = Some(latin))
      ) shouldBe true
    }
  }

  private def newEssentials(plate: Option[String], plateOnPhoto: Option[String]): AutoruEssentials = {
    val genEssentials = essentialsGen(Service.AUTORU).next
    val hasLicensePlateOnPhotos = if (plateOnPhoto.isDefined) Some(true) else Some(false)
    genEssentials
      .asInstanceOf[AutoruEssentials]
      .copy(
        licensePlate = plate,
        hasLicensePlateOnPhotos = hasLicensePlateOnPhotos,
        photosLicensePlate = plateOnPhoto
      )
  }
}
