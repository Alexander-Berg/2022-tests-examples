package ru.yandex.auto.vin.decoder.raw.autoteka

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.raw.FileFormats
import ru.yandex.auto.vin.decoder.raw.autoteka.products.AutotekaProductsRawModelManager
import ru.yandex.auto.vin.decoder.raw.validation.ValidationFieldErrors.{NumberFieldError, NumberFieldRangeError}
import ru.yandex.auto.vin.decoder.raw.validation.ValidationRecordError

class AutotekaProductsRawModelManagerTest extends AnyFunSuite {

  private val csvManager = AutotekaProductsRawModelManager(FileFormats.Csv, ".", EventType.AXSEL_INSURANCE)

  test("parse csv") {

    val rawInputStream = getClass.getResourceAsStream("/autoteka/products/AXSEL_000001_3_20200208-023002.csv")

    val result = csvManager.parseFile(rawInputStream, "AXSEL_000001_3_20200208-023002.csv").toList.collect {
      case Right(v) => v
    }

    assert(result.size === 2)

    assert(result(0).groupId === "AMV0572421")
    assert(result(0).identifier.toString === "WBA5V71030FH76487")
    assert(result(0).model.event.`type` === 3)
    assert(result(0).model.event.timestamp === 1581033600000L)
    assert(result(0).model.event.region === Some("Санкт-Петербург"))
    assert(result(0).model.event.city === None)
    assert(result(0).model.event.place === Some("Санкт-Петербург"))
    assert(result(0).model.common.carBrand === Some("BMW"))
    assert(result(0).model.common.carModel === Some("320D XDR A"))
    assert(result(0).model.common.yearManufactured === Some(2019))
    assert(result(0).model.common.mileage === Some(1910))
    assert(result(0).model.common.clientTypeCode === Some(1))
    assert(result(0).model.products.length === 2)

    assert(result(1).groupId === "AMV0572666")
    assert(result(1).identifier.toString === "WBACW21060LS06537")
    assert(result(1).model.event.`type` === 3)
    assert(result(1).model.event.timestamp === 1581033600000L)
    assert(result(1).model.event.region === None)
    assert(result(1).model.event.city === Some("Санкт-Петербург"))
    assert(result(1).model.event.place === Some("Санкт-Петербург"))
    assert(result(1).model.common.carBrand === Some("BMW"))
    assert(result(1).model.common.carModel === Some("X7 xDrive40i"))
    assert(result(1).model.common.yearManufactured === Some(2020))
    assert(result(1).model.common.mileage === Some(6215))
    assert(result(1).model.common.clientTypeCode === Some(1))
    assert(result(1).model.products.length === 1)
  }

  test("parse csv with warnings") {

    val rawInputStream = getClass.getResourceAsStream("/autoteka/products/products_with_errors.csv")

    val parsed = csvManager.parseFile(rawInputStream, "AXSEL_000001_3_20200208-023002.csv").toList

    val errors = parsed.collect { case Left(v) =>
      v
    }

    val success = parsed.collect { case Right(v) =>
      v
    }

    assert(errors.size === 0)
    assert(success.size === 3)

    assert(success(0).warnings.size === 1)
    assert(success(0).warnings(0).asInstanceOf[NumberFieldRangeError[_]].field === "y")

    assert(success(1).warnings.size === 1)
    assert(success(1).warnings(0).asInstanceOf[NumberFieldError].field === "m")

    assert(success(2).warnings.size === 1)
    assert(success(2).warnings(0).isInstanceOf[ValidationRecordError])
  }
}
