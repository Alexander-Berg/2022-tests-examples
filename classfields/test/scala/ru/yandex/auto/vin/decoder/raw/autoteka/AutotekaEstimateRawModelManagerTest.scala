package ru.yandex.auto.vin.decoder.raw.autoteka

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.raw.autoteka.estimate.AutotekaEstimateRawModelManager
import ru.yandex.auto.vin.decoder.raw.autoteka.estimate.model.AutotekaEstimateRawModel
import ru.yandex.auto.vin.decoder.raw.validation.ValidationFieldErrors.{InvalidVinError, NumberFieldRangeError}
import ru.yandex.auto.vin.decoder.raw.validation.{ValidationError, ValidationRecordError}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.jdk.CollectionConverters.ListHasAsScala

class AutotekaEstimateRawModelManagerTest extends AnyFunSuite {

  private val manager = AutotekaEstimateRawModelManager(EventType.UNDEFINED)
  private val correctFile = "NovocarUG_93551706_12_20210427-090633.json"
  private val withWarnsFile = "warn-NovocarUG_93551706_12_20210427-090633.json"
  private val withErrorFile = "error-NovocarUG_93551706_12_20210427-090633.json"

  test("parse correct json") {

    val ests = parseFile(correctFile)

    assert(ests.size === 1)
    assert(ests.head.isRight)

    val est = ests.head.getOrElse(throw new RuntimeException(""))

    assert(est.groupId === "НЮГ0021022")
    assert(est.identifier.toString === "XW8JJ65L1HH711036")

    assert(est.model.event.`type` === 12)
    assert(est.model.event.timestamp === 1618876800000L)
    assert(est.model.event.region === Some("Краснодарский край"))
    assert(est.model.event.city === Some("Новороссийск"))
    assert(est.model.event.place === Some("Новороссийск"))

    assert(est.model.common.clientTypeCode === Some(0))
    assert(est.model.common.mileage === Some(24688))
    assert(est.model.common.yearManufactured === Some(2018))
    assert(est.model.common.carBrand === Some("Skoda"))
    assert(est.model.common.carModel === Some("Yeti"))

    assert(est.model.providerId === "93551706")

    assert(est.model.photos.size === 1)
    assert(est.model.photos.head.name === Some("Кузов спереди слева"))
    assert(est.model.photos.head.url === "https://mygreatimagehosting.com/foo_bar.jpg")

    assert(est.model.condition.nonEmpty)
    assert(est.model.condition.head.stars.nonEmpty)
    assert(est.model.condition.head.stars.head.label === "Кузов")
    assert(est.model.condition.head.stars.head.rating === 3)
    assert(est.model.condition.head.issues.nonEmpty)
    assert(
      est.model.condition.head.issues.head === "Пробег более 35 000 км за год и (или) общий пробег более 100 000 км"
    )

    assert(est.model.price === Some(650000))

    assert(est.model.damages.nonEmpty)
    assert(est.model.damages.head.`type` === "Потертость")
    assert(est.model.damages.head.part === "Кузов")
    assert(est.model.damages.head.degree === None)
    assert(est.model.damages.head.photoUrl === Some("https://megaautoauction.ws/kdushfkdjsfgkdsfugsdgf.jpg"))
  }

  test("parse json with warnings") {

    val ests = parseFile(withWarnsFile)

    assert(ests.size === 1)
    assert(ests.head.isRight)
    val est = ests.head.getOrElse(throw new RuntimeException(""))

    assert(est.warnings.size === 3)
    assert(est.warnings(0).isInstanceOf[ValidationRecordError]) // нет ни города, ни региона
    assert(est.warnings(1).isInstanceOf[NumberFieldRangeError[_]]) // отрицательный год
    assert(est.warnings(2).isInstanceOf[NumberFieldRangeError[_]]) // отрицательный пробег
  }

  test("parse json with errors") {

    val ests = parseFile(withErrorFile)

    assert(ests.size === 2)
    assert(ests.head.isLeft)
    assert(ests(1).isRight)
    val err = ests.head.left.getOrElse(throw new RuntimeException(""))

    assert(err.isInstanceOf[InvalidVinError])
  }

  test("convert to VinInfoHistory") {

    val ests = parseFile(correctFile)

    assert(ests.size === 1)
    assert(ests.head.isRight)

    val est = ests.head.getOrElse(throw new RuntimeException(""))
    val vih = Await.result(manager.convert(est), 3.seconds)

    assert(vih.getEstimatesList.asScala.size === 1)
    val vihEst = vih.getEstimatesList.asScala.head

    assert(vihEst.getImagesCount === 1)
    val vihImg = vihEst.getImagesList.asScala.head
    assert(vihImg.getExternalPhotoUrl === "https://mygreatimagehosting.com/foo_bar.jpg")

    assert(vihEst.hasDamages)
    assert(vihEst.getDamages.getDamagesCount === 1)
    val vihDmg = vihEst.getDamages.getDamagesList.asScala.head
    assert(vihDmg.getDescription === "Кузов: Потертость")
    assert(vihDmg.getImage.getExternalPhotoUrl === "https://megaautoauction.ws/kdushfkdjsfgkdsfugsdgf.jpg")

    assert(vihEst.hasResults)
    assert(vihEst.getResults.getPriceFrom === 650000)
    assert(vihEst.getResults.getPriceTo === 650000)
    assert(vihEst.getResults.getSummary === "Пробег более 35 000 км за год и (или) общий пробег более 100 000 км")
    assert(vihEst.getResults.getInspectionDetailsCount === 1)
    assert(vihEst.getResults.getInspectionDetailsList.asScala.head === "Кузов: 3")
  }

  private def parseFile(filename: String): List[Either[ValidationError, AutotekaEstimateRawModel]] = {
    manager
      .parseFile(
        is = getClass.getResourceAsStream(s"/autoteka/estimate/$filename"),
        filename
      )
      .toList
  }
}
