package ru.yandex.auto.vin.decoder.raw.autoteka

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.raw.FileFormats
import ru.yandex.auto.vin.decoder.raw.autoteka.services.AutotekaServicesRawModelManager
import ru.yandex.auto.vin.decoder.raw.validation.ValidationFieldErrors.{DateFieldError, LimitedFieldError}

class AutotekaServicesRawModelManagerTest extends AnyFunSuite {

  private val jsonManager = AutotekaServicesRawModelManager(FileFormats.Json, ".", EventType.AXSEL_INSURANCE)
  private val csvManager = AutotekaServicesRawModelManager(FileFormats.Csv, ".", EventType.AXSEL_INSURANCE)

  test("parse csv") {

    val rawInputStream = getClass.getResourceAsStream("/autoteka/services/AXSEL_000001_2_20200203-111959.csv")

    val result = csvManager.parseFile(rawInputStream, "AXSEL_000001_2_20200203-111959.csv").toList.collect {
      case Right(v) => v
    }

    assert(result.size === 2)

    assert(result(0).groupId === "AMV0377811")
    assert(result(0).identifier.toString === "X4XVM99430VZ96355")
    assert(result(0).model.event.`type` === 2)
    assert(result(0).model.event.timestamp === 1454371200000L)
    assert(result(0).model.event.region === Some("Санкт-Петербург"))
    assert(result(0).model.event.city === None)
    assert(result(0).model.event.place === Some("Санкт-Петербург"))
    assert(result(0).model.common.carBrand === Some("BMW"))
    assert(result(0).model.common.carModel === Some("X1 XDRIVE20I RUS SKD"))
    assert(result(0).model.common.yearManufactured === Some(2014))
    assert(result(0).model.common.mileage === Some(31747))
    assert(result(0).model.workType === Some("regulation"))
    assert(result(0).model.workTypeName === Some("Регламентное ТО"))
    assert(result(0).model.common.clientTypeCode === Some(0))
    assert(result(0).model.services.length === 4)

    assert(result(1).groupId === "AMV0377835")
    assert(result(1).identifier.toString === "WBA1D11020J808287")
    assert(result(1).model.event.`type` === 2)
    assert(result(1).model.event.timestamp === 1454371200000L)
    assert(result(1).model.event.region === Some("Санкт-Петербург"))
    assert(result(1).model.event.city === None)
    assert(result(1).model.event.place === Some("Санкт-Петербург"))
    assert(result(1).model.common.carBrand === Some("BMW"))
    assert(result(1).model.common.carModel === Some("116I RL A"))
    assert(result(1).model.common.yearManufactured === Some(2013))
    assert(result(1).model.common.mileage === Some(0))
    assert(result(1).model.workType === Some("ordinary_paid"))
    assert(result(1).model.workTypeName === Some("Текущий ремонт за свой счет"))
    assert(result(1).model.common.clientTypeCode === Some(0))
    assert(result(1).model.services.length === 5)
  }

  test("parse csv with errors") {

    val rawInputStream = getClass.getResourceAsStream("/autoteka/services/services_with_errors.csv")

    val result = csvManager.parseFile(rawInputStream, "AXSEL_000001_2_20200203-111959.csv").toList.collect {
      case Left(v) => v
    }

    assert(result.size === 2)
    assert(result(0).asInstanceOf[LimitedFieldError[_]].field === "type")
    assert(result(1).asInstanceOf[DateFieldError].field === "dt")
  }

}
