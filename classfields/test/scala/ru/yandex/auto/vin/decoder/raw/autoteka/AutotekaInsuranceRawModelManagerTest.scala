package ru.yandex.auto.vin.decoder.raw.autoteka

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.VinReportModel.InsuranceType
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.raw.FileFormats
import ru.yandex.auto.vin.decoder.raw.autoteka.insurance.AutotekaInsuranceRawModelManager
import ru.yandex.auto.vin.decoder.raw.validation.ValidationFieldErrors.{LimitedFieldError, RequiredFieldError}

class AutotekaInsuranceRawModelManagerTest extends AnyFunSuite {

  private val csvManager = AutotekaInsuranceRawModelManager(FileFormats.Csv, EventType.AXSEL_INSURANCE)

  test("parse csv") {
    val rawInputStream = getClass.getResourceAsStream("/autoteka/insurance/LUIDOR_603152_5_20200603-142114.csv")

    val result = csvManager.parseFile(rawInputStream, "LUIDOR_603152_5_20200603-142114.csv").toList.collect {
      case Right(v) => v
    }

    assert(result.size === 2)

    assert(result(0).groupId === "f7db52f6-8e58-11e7-80e0-00155d012703")
    assert(result(0).identifier.toString === "XU42824NEH0003032")
    assert(result(0).model.event.`type` === 5)
    assert(result(0).model.event.timestamp === 1500681600000L)
    assert(result(0).model.event.region === None)
    assert(result(0).model.event.city === Some("Балашиха"))
    assert(result(0).model.event.place === Some("Балашиха"))
    assert(result(0).model.common.yearManufactured === Some(2017))
    assert(result(0).model.insuranceStartTimestamp === 1504137600000L)
    assert(result(0).model.insuranceFinishTimestamp === 1535673600000L)
    assert(result(0).model.insuranceType === InsuranceType.OSAGO)

    assert(result(1).groupId === "08080a6c-e62f-11e7-80ea-00155d012705")
    assert(result(1).identifier.toString === "X96330232J2709802")
    assert(result(1).model.event.`type` === 5)
    assert(result(1).model.event.timestamp === 1513209600000L)
    assert(result(1).model.event.region === None)
    assert(result(1).model.event.city === Some("Балашиха"))
    assert(result(1).model.event.place === Some("Балашиха"))
    assert(result(1).model.common.yearManufactured === Some(2017))
    assert(result(1).model.insuranceStartTimestamp === 1513814400000L)
    assert(result(1).model.insuranceFinishTimestamp === 1545350400000L)
    assert(result(1).model.insuranceType === InsuranceType.KASKO)
  }

  test("parse csv with errors") {
    val rawInputStream = getClass.getResourceAsStream("/autoteka/insurance/insurance_with_errors.csv")

    val result = csvManager.parseFile(rawInputStream, "LUIDOR_603152_5_20200603-142114.csv").toList.collect {
      case Left(v) => v
    }

    assert(result.size === 3)

    assert(result(0).asInstanceOf[RequiredFieldError].field === "istart")
    assert(result(1).asInstanceOf[RequiredFieldError].field === "iend")
    assert(result(2).asInstanceOf[LimitedFieldError[_]].field === "itp")
  }

}
