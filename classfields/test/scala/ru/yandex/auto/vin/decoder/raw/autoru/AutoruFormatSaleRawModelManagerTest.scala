package ru.yandex.auto.vin.decoder.raw.autoru

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.VinReportModel.OwnerType
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.raw.FileFormats
import ru.yandex.auto.vin.decoder.raw.autoru.sale.AutoruFormatSaleRawModelManager
import ru.yandex.auto.vin.decoder.raw.validation.ValidationFieldErrors._

class AutoruFormatSaleRawModelManagerTest extends AnyFunSuite {

  private val jsonManager = new AutoruFormatSaleRawModelManager(EventType.AXSEL_SELL_AUTO, FileFormats.Json)
  private val xmlManager = new AutoruFormatSaleRawModelManager(EventType.AXSEL_SELL_AUTO, FileFormats.Xml)
  private val csvManager = new AutoruFormatSaleRawModelManager(EventType.AXSEL_SELL_AUTO, FileFormats.Csv)

  test("parse json") {
    val rawInputStream = getClass.getResourceAsStream("/autoru/sales/ROLF_1_20191230-135025.json")

    val result = jsonManager.parseFile(rawInputStream, "ROLF_2_20200525-102513.json").toList.collect { case Right(v) =>
      v
    }

    assert(result.size === 2)

    assert(result(0).groupId === "АU127780")
    assert(result(0).identifier.toString === "X4X3D59440J496874")
    assert(result(0).model.info.get.eventTimestamp === 1587513600000L)
    assert(result(0).model.info.get.isNew === Some(false))
    assert(result(0).model.info.get.isCredit === Some(true))
    assert(result(0).model.info.get.sellerType === Some(OwnerType.Type.LEGAL))
    assert(result(0).model.info.get.clientType === Some(OwnerType.Type.PERSON))
    assert(result(0).model.info.get.amount === Some(1043000))

    assert(result(1).groupId === "20BBC0001209")
    assert(result(1).identifier.toString === "Z94CT41CAFR444618")
    assert(result(1).model.info.get.eventTimestamp === 1589846400000L)
    assert(result(1).model.info.get.isCredit === Some(false))
    assert(result(1).model.info.get.isNew === Some(true))
    assert(result(1).model.info.get.sellerType === None)
    assert(result(1).model.info.get.clientType === Some(OwnerType.Type.PERSON))
    assert(result(1).model.info.get.amount === Some(950000))
    assert(result(1).model.info.get.stoId === Some("51"))
    assert(result(1).model.info.get.stoName === Some("РОЛЬФ Санкт-Петербург"))
  }

  test("parse xml") {
    val rawInputStream = getClass.getResourceAsStream("/autoru/sales/ROLF_1_20191230-135025.xml")

    val result = xmlManager.parseFile(rawInputStream, "ROLF_2_20200525-102513.xml").toList.collect { case Right(v) =>
      v
    }

    assert(result.size === 2)

    assert(result(0).groupId === "АU127780")
    assert(result(0).identifier.toString === "X4X3D59440J496874")
    assert(result(0).model.info.get.eventTimestamp === 1587513600000L)
    assert(result(0).model.info.get.isNew === Some(false))
    assert(result(0).model.info.get.isCredit === Some(true))
    assert(result(0).model.info.get.sellerType === Some(OwnerType.Type.LEGAL))
    assert(result(0).model.info.get.clientType === Some(OwnerType.Type.PERSON))
    assert(result(0).model.info.get.amount === Some(1043000))

    assert(result(1).groupId === "20BBC0001209")
    assert(result(1).identifier.toString === "Z94CT41CAFR444618")
    assert(result(1).model.info.get.eventTimestamp === 1589846400000L)
    assert(result(1).model.info.get.isCredit === Some(false))
    assert(result(1).model.info.get.isNew === Some(true))
    assert(result(1).model.info.get.sellerType === None)
    assert(result(1).model.info.get.clientType === Some(OwnerType.Type.PERSON))
    assert(result(1).model.info.get.amount === Some(950000))
    assert(result(1).model.info.get.stoId === Some("51"))
    assert(result(1).model.info.get.stoName === Some("РОЛЬФ Санкт-Петербург"))
  }

  test("parse csv") {
    val rawInputStream = getClass.getResourceAsStream("/autoru/sales/ROLF_1_20191230-135025.csv")

    val result = csvManager.parseFile(rawInputStream, "ROLF_2_20200525-102513.csv").toList.collect { case Right(v) =>
      v
    }

    assert(result.size === 2)

    assert(result(0).groupId === "АU127780")
    assert(result(0).identifier.toString === "X4X3D59440J496874")
    assert(result(0).model.info.get.eventTimestamp === 1587513600000L)
    assert(result(0).model.info.get.isNew === Some(false))
    assert(result(0).model.info.get.isCredit === Some(true))
    assert(result(0).model.info.get.sellerType === Some(OwnerType.Type.LEGAL))
    assert(result(0).model.info.get.clientType === Some(OwnerType.Type.PERSON))
    assert(result(0).model.info.get.amount === Some(1043000))

    assert(result(1).groupId === "20BBC0001209")
    assert(result(1).identifier.toString === "Z94CT41CAFR444618")
    assert(result(1).model.info.get.eventTimestamp === 1589846400000L)
    assert(result(1).model.info.get.isCredit === Some(false))
    assert(result(1).model.info.get.isNew === Some(true))
    assert(result(1).model.info.get.sellerType === None)
    assert(result(1).model.info.get.clientType === Some(OwnerType.Type.PERSON))
    assert(result(1).model.info.get.amount === Some(950000))
    assert(result(1).model.info.get.stoId === Some("51"))
    assert(result(1).model.info.get.stoName === Some("РОЛЬФ Санкт-Петербург"))
  }

  test("parse json with errors") {
    val rawInputStream = getClass.getResourceAsStream("/autoru/sales/sales_with_errors.json")

    val parsed = jsonManager.parseFile(rawInputStream, "ROLF_2_20200525-102513.json").toList

    val errors = parsed.collect { case Left(v) =>
      v
    }

    val success = parsed.collect { case Right(v) =>
      v
    }

    assert(errors.size === 7)
    assert(success.size === 2)

    assert(success(0).warnings.size === 1)
    assert(success(0).warnings(0).asInstanceOf[NumberFieldRangeError[_]].field === "YEAR")
    assert(success(1).warnings.size === 1)
    assert(success(1).warnings(0).asInstanceOf[NumberFieldRangeError[_]].field === "MILEAGE")

    assert(errors(0).asInstanceOf[RequiredFieldError].field === "ID")

    assert(errors(1).asInstanceOf[RequiredFieldError].field === "VIN")

    assert(errors(2).asInstanceOf[EqualsFieldError].field === "EVENT_TYPE")

    assert(errors(3).asInstanceOf[RequiredFieldError].field === "EVENT_DATE")

    assert(errors(4).asInstanceOf[LimitedFieldError[_]].field === "CLIENT_TYPE")

    assert(errors(5).asInstanceOf[LimitedFieldError[_]].field === "IS_NEW")

    assert(errors(6).asInstanceOf[NumberFieldError].field === "AMOUNT")
  }

  test("parse xml with errors") {
    val rawInputStream = getClass.getResourceAsStream("/autoru/sales/sales_with_errors.xml")

    val parsed = xmlManager.parseFile(rawInputStream, "ROLF_2_20200525-102513.xml").toList

    val errors = parsed.collect { case Left(v) =>
      v
    }

    val success = parsed.collect { case Right(v) =>
      v
    }

    assert(errors.size === 8)
    assert(success.size === 2)

    assert(success(0).warnings.size === 1)
    assert(success(0).warnings(0).asInstanceOf[NumberFieldRangeError[_]].field === "YEAR")
    assert(success(1).warnings.size === 1)
    assert(success(1).warnings(0).asInstanceOf[NumberFieldRangeError[_]].field === "MILEAGE")

    assert(errors(0).asInstanceOf[RequiredFieldError].field === "ID")

    assert(errors(1).asInstanceOf[RequiredFieldError].field === "VIN")

    assert(errors(2).asInstanceOf[EqualsFieldError].field === "EVENT_TYPE")

    assert(errors(3).asInstanceOf[RequiredFieldError].field === "EVENT_DATE")

    assert(errors(4).asInstanceOf[LimitedFieldError[_]].field === "CLIENT_TYPE")

    assert(errors(5).asInstanceOf[LimitedFieldError[_]].field === "IS_NEW")

    assert(errors(6).asInstanceOf[NumberFieldRangeError[_]].field === "AMOUNT")

    assert(errors(7).asInstanceOf[NumberFieldError].field === "AMOUNT")
  }

}
