package ru.yandex.auto.vin.decoder.raw.autoteka

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.ApiOfferModel.Section
import ru.auto.api.vin.VinReportModel.OwnerType
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.raw.FileFormats
import ru.yandex.auto.vin.decoder.raw.autoteka.sale.AutotekaSaleRawModelManager
import ru.yandex.auto.vin.decoder.raw.validation.ValidationFieldErrors.{
  EqualsFieldError,
  LimitedFieldError,
  NumberFieldError,
  RequiredFieldError
}

class AutotekaSaleRawModelManagerTest extends AnyFunSuite {

  private val jsonManager = AutotekaSaleRawModelManager(FileFormats.Json, EventType.BRIGHT_PARK_SALES)
  private val csvManager = AutotekaSaleRawModelManager(FileFormats.Csv, EventType.BRIGHT_PARK_SALES)

  test("parse json") {
    val rawInputStream = getClass.getResourceAsStream("/autoteka/sales/BRIGHTPARK_1_1_20200602-174502.json")

    val result = jsonManager.parseFile(rawInputStream, "BRIGHTPARK_1_1_20200602-174502.json").toList.collect {
      case Right(v) => v
    }

    assert(result.size === 1)

    assert(result(0).groupId === "17О000007891")
    assert(result(0).identifier.toString === "XTT316300J1003747")
    assert(result(0).model.event.`type` === 1)
    assert(result(0).model.event.timestamp === 1509148800000L)
    assert(result(0).model.event.region === Some("Пермский край"))
    assert(result(0).model.event.city === Some("Пермь г"))
    assert(result(0).model.event.place === Some("Пермь г"))
    assert(result(0).model.common.mileage === Some(0))
    assert(result(0).model.common.carBrand === Some("UAZ"))
    assert(result(0).model.common.carModel === Some("PATRIOT (NEW)"))
    assert(result(0).model.optIsNew === Some(true))
    assert(result(0).model.section === Section.NEW)
    assert(result(0).model.optIsCredit === Some(false))
  }

  test("parse csv") {
    val rawInputStream = getClass.getResourceAsStream("/autoteka/sales/AlfaGarant_yandex_1_20200615-153212.csv")

    val result = csvManager.parseFile(rawInputStream, "AlfaGarant_yandex_1_20200615-153212.csv").toList.collect {
      case Right(v) => v
    }

    assert(result.size === 2)

    assert(result(0).groupId === "P000000250")
    assert(result(0).identifier.toString === "XTT390945L1216671")
    assert(result(0).model.event.`type` === 1)
    assert(result(0).model.event.timestamp === 1591056000000L)
    assert(result(0).model.event.region === Some("ПЕРМЬ"))
    assert(result(0).model.event.city === None)
    assert(result(0).model.event.place === Some("ПЕРМЬ"))
    assert(result(0).model.optClientType === Some(OwnerType.Type.PERSON))
    assert(result(0).model.sellerType === OwnerType.Type.LEGAL)
    assert(result(0).model.optIsNew === Some(true))
    assert(result(0).model.optIsCredit === Some(false))
    assert(result(0).model.currency === "usd")
    assert(result(0).model.amount === Some(95500))
    assert(result(0).model.common.mileage === None)

    assert(result(1).groupId === "В000000212")
    assert(result(1).identifier.toString === "XWEPC811BC0007669")
    assert(result(1).model.event.`type` === 1)
    assert(result(1).model.event.timestamp === 1590969600000L)
    assert(result(1).model.event.region === Some("ПЕРМЬ"))
    assert(result(0).model.event.city === None)
    assert(result(0).model.event.place === Some("ПЕРМЬ"))
    assert(result(1).model.optClientType === Some(OwnerType.Type.LEGAL))
    assert(result(1).model.sellerType === OwnerType.Type.LEGAL)
    assert(result(1).model.optIsNew === Some(false))
    assert(result(1).model.optIsCredit === Some(true))
    assert(result(1).model.currency === "rub")
    assert(result(1).model.amount === Some(600000))
    assert(result(1).model.common.mileage === Some(130000))
  }

  test("parse json with errors") {
    val rawInputStream = getClass.getResourceAsStream("/autoteka/sales/sales_with_errors.json")

    val result = jsonManager.parseFile(rawInputStream, "BRIGHTPARK_1_1_20200602-174502.json").toList.collect {
      case Left(v) => v
    }

    assert(result.size === 5)

    assert(result(0).asInstanceOf[RequiredFieldError].field === "id")

    assert(result(1).asInstanceOf[RequiredFieldError].field === "v")

    assert(result(2).asInstanceOf[EqualsFieldError].field === "et")

    assert(result(3).asInstanceOf[RequiredFieldError].field === "dt")

    assert(result(4).asInstanceOf[LimitedFieldError[_]].field === "isn")
  }

  test("parse csv with errors") {
    val rawInputStream = getClass.getResourceAsStream("/autoteka/sales/sales_with_errors.csv")

    val result = csvManager.parseFile(rawInputStream, "BRIGHTPARK_1_1_20200602-174502.csv").toList.collect {
      case Left(v) => v
    }

    assert(result.size === 2)

    assert(result(0).asInstanceOf[RequiredFieldError].field === "v")

    assert(result(1).asInstanceOf[NumberFieldError].field === "isn")
  }

}
