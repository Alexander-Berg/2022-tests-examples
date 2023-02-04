package ru.yandex.auto.vin.decoder.raw.autoru

import org.scalatest.Assertion
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.raw.FileFormats
import ru.yandex.auto.vin.decoder.raw.autoru.credit.AutoruFormatCreditApplicationRawModelManager
import ru.yandex.auto.vin.decoder.raw.autoru.credit.model.AutoruFormatCreditApplicationRawModel
import ru.yandex.auto.vin.decoder.raw.validation.ValidationFieldErrors.{
  DateFieldError,
  NumberFieldRangeError,
  RequiredFieldError
}

import scala.io.Source

class AutoruFormatCreditApplicationRawModelManagerTest extends AnyFunSuite {

  private val jsonManager =
    new AutoruFormatCreditApplicationRawModelManager(EventType.UNDEFINED, FileFormats.Json)

  private val xmlManager =
    new AutoruFormatCreditApplicationRawModelManager(EventType.UNDEFINED, FileFormats.Xml)

  private val csvManager =
    new AutoruFormatCreditApplicationRawModelManager(EventType.UNDEFINED, FileFormats.Csv)

  test("parse json") {
    val rawInputStream = getClass.getResourceAsStream("/autoru/credit/credit-application.json")

    val result = jsonManager.parseFile(rawInputStream, "credit-application.json").toList.collect { case Right(v) =>
      v
    }

    testParsed(result)
  }

  test("parse xml") {
    val rawInputStream = getClass.getResourceAsStream("/autoru/credit/credit-application.xml")

    val result = xmlManager.parseFile(rawInputStream, "credit-application.xml").toList.collect { case Right(v) =>
      v
    }

    testParsed(result)
  }

  test("parse csv") {
    val rawInputStream = getClass.getResourceAsStream("/autoru/credit/credit-application.csv")

    val result = csvManager.parseFile(rawInputStream, "credit-application.csv").toList.collect { case Right(v) =>
      v
    }

    testParsed(result)
  }

  def testParsed(result: Seq[AutoruFormatCreditApplicationRawModel]): Assertion = {
    assert(result.size === 3)
    assert(result(0).identifier.toString === "XTC111130Y0121617")
    assert(result(0).groupId === "723cba56-e88c-4304-9d5e-9b97ea709fb0")
    assert(result(0).model.info.get.eventTimestamp === 1555286400000L)
    assert(result(0).model.info.get.amount === Some(50000))
    assert(result(0).model.info.get.eventRegion === Some("Самарская обл"))
    assert(result(0).model.info.get.eventCity === Some("г Тольятти"))
    assert(result(0).model.info.get.mark === Some("LADA (ВАЗ)"))
    assert(result(0).model.info.get.model === Some("1111 Ока"))
    assert(result(0).model.info.get.year === Some(2000))
    assert(result(0).model.info.get.mileage === Some(54000))

    assert(result(1).identifier.toString === "XTA21140064076636")
    assert(result(1).groupId === "b4a52818-dfcd-4781-9708-c96e925c49c4")
    assert(result(1).model.info.get.eventTimestamp === 1579996800000L)
    assert(result(1).model.info.get.amount === Some(50000))
    assert(result(1).model.info.get.eventRegion === Some("Респ Татарстан"))
    assert(result(1).model.info.get.eventCity === Some("г Альметьевск"))
    assert(result(1).model.info.get.mark === Some("LADA (ВАЗ)"))
    assert(result(1).model.info.get.model === Some("2114"))
    assert(result(1).model.info.get.year === Some(2005))
    assert(result(1).model.info.get.mileage === Some(150000))

    assert(result(2).identifier.toString === "XTA21120040252427")
    assert(result(2).groupId === "ce6db864-9de1-49b0-baa3-fd11c83a10a4")
    assert(result(2).model.info.get.eventTimestamp === 1614038400000L)
    assert(result(2).model.info.get.amount === Some(50000))
    assert(result(2).model.info.get.eventRegion === Some("Ивановская обл"))
    assert(result(2).model.info.get.eventCity === Some("г Иваново"))
    assert(result(2).model.info.get.mark === Some("LADA (ВАЗ)"))
    assert(result(2).model.info.get.model === Some("2112"))
    assert(result(2).model.info.get.year === Some(2004))
    assert(result(2).model.info.get.mileage === Some(150000))

  }

  test("parse json with errors") {

    val rawInputStream = getClass.getResourceAsStream("/autoru/credit/credit-application-with-errors.json")

    val result = jsonManager.parseFile(rawInputStream, "credit-application-with-errors.json").toList.collect {
      case Left(v) => v
    }

    assert(result.size == 6)

    assert(result(0).asInstanceOf[RequiredFieldError].field === "EVENT_DATE")

    assert(result(1).asInstanceOf[RequiredFieldError].field === "ID")

    assert(result(2).asInstanceOf[RequiredFieldError].field === "VIN")

    assert(result(3).asInstanceOf[DateFieldError].value === "0014-02-01")

    assert(result(4).asInstanceOf[NumberFieldRangeError[Int]].field === "AMOUNT")

    assert(result(5).asInstanceOf[NumberFieldRangeError[Int]].field === "MILEAGE")

  }

}
