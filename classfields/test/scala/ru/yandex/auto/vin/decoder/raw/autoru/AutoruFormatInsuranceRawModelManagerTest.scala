package ru.yandex.auto.vin.decoder.raw.autoru

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.VinReportModel.InsuranceType
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.raw.FileFormats
import ru.yandex.auto.vin.decoder.raw.autoru.common.raw.RawInfoParams.InsuranceStartField
import ru.yandex.auto.vin.decoder.raw.autoru.insurance.AutoruFormatInsuranceRawModelManager
import ru.yandex.auto.vin.decoder.raw.validation.ValidationFieldErrors.{DateFieldError, LimitedFieldError}
import ru.yandex.auto.vin.decoder.raw.validation.ValidationRecordError

class AutoruFormatInsuranceRawModelManagerTest extends AnyFunSuite {

  val jsonManager = new AutoruFormatInsuranceRawModelManager(EventType.AXSEL_INSURANCE, FileFormats.Json)
  val xmlManager = new AutoruFormatInsuranceRawModelManager(EventType.AXSEL_INSURANCE, FileFormats.Xml)
  val csvManager = new AutoruFormatInsuranceRawModelManager(EventType.AXSEL_INSURANCE, FileFormats.Csv)

  test("parse json") {
    val rawInputStream = getClass.getResourceAsStream("/autoru/insurances/ROLF_5_20200420-181759.json")

    val result = jsonManager.parseFile(rawInputStream, "ROLF_5_20200420-181759.json").toList.collect { case Right(v) =>
      v
    }

    assert(result.size === 3)
    assert(result(0).identifier.toString === "Z8NTBNT31CS047525")
    assert(result(0).groupId === "PE0006940")
    assert(result(0).model.info.get.eventTimestamp === 1581638400000L)
    assert(result(0).model.info.get.insuranceStart.get === 1583539200000L)
    assert(result(0).model.info.get.insuranceEnd.get === 1614988800000L)
    assert(result(0).model.info.get.insuranceType === InsuranceType.KASKO)

    assert(result(1).identifier.toString === "Z8NTBNT31CS047525")
    assert(result(1).groupId === "SA1206949")
    assert(result(1).model.info.get.eventTimestamp === 1581638400000L)
    assert(result(1).model.info.get.insuranceStart.get === 1583539200000L)
    assert(result(1).model.info.get.insuranceEnd.get === 1614988800000L)
    assert(result(1).model.info.get.insuranceType === InsuranceType.OSAGO)

    assert(result(2).identifier.toString === "Z8NTBNT32ES020695")
    assert(result(2).groupId === "AB0030278")
    assert(result(2).model.info.get.eventTimestamp === 1581638400000L)
    assert(result(2).model.info.get.insuranceStart.get === 1583539200000L)
    assert(result(2).model.info.get.insuranceEnd.get === 1614988800000L)
    assert(result(2).model.info.get.insuranceType === InsuranceType.UNKNOWN_INSURANCE)
  }

  test("parse xml") {
    val rawInputStream = getClass.getResourceAsStream("/autoru/insurances/ROLF_5_20200420-181759.xml")

    val result = xmlManager.parseFile(rawInputStream, "ROLF_5_20200420-181759.xml").toList.collect { case Right(v) =>
      v
    }

    assert(result.size === 3)
    assert(result(0).identifier.toString === "Z8NTBNT31CS047525")
    assert(result(0).groupId === "PE0006940")
    assert(result(0).model.info.get.eventTimestamp === 1581638400000L)
    assert(result(0).model.info.get.insuranceStart.get === 1583539200000L)
    assert(result(0).model.info.get.insuranceEnd.get === 1614988800000L)
    assert(result(0).model.info.get.insuranceType === InsuranceType.KASKO)

    assert(result(1).identifier.toString === "Z8NTBNT31CS047525")
    assert(result(1).groupId === "SA1206949")
    assert(result(1).model.info.get.eventTimestamp === 1581638400000L)
    assert(result(1).model.info.get.insuranceStart.get === 1583539200000L)
    assert(result(1).model.info.get.insuranceEnd.get === 1614988800000L)
    assert(result(1).model.info.get.insuranceType === InsuranceType.OSAGO)

    assert(result(2).identifier.toString === "Z8NTBNT32ES020695")
    assert(result(2).groupId === "AB0030278")
    assert(result(2).model.info.get.eventTimestamp === 1581638400000L)
    assert(result(2).model.info.get.insuranceStart.get === 1583539200000L)
    assert(result(2).model.info.get.insuranceEnd.get === 1614988800000L)
    assert(result(2).model.info.get.insuranceType === InsuranceType.UNKNOWN_INSURANCE)
  }

  test("parse csv") {
    val rawInputStream = getClass.getResourceAsStream("/autoru/insurances/ROLF_5_20200420-181759.csv")

    val result = csvManager.parseFile(rawInputStream, "ROLF_5_20200420-181759.csv").toList.collect { case Right(v) =>
      v
    }

    assert(result.size === 3)
    assert(result(0).identifier.toString === "Z8NTBNT31CS047525")
    assert(result(0).groupId === "PE0006940")
    assert(result(0).model.info.get.eventTimestamp === 1581638400000L)
    assert(result(0).model.info.get.insuranceStart.get === 1583539200000L)
    assert(result(0).model.info.get.insuranceEnd.get === 1614988800000L)
    assert(result(0).model.info.get.insuranceType === InsuranceType.KASKO)

    assert(result(1).identifier.toString === "Z8NTBNT31CS047525")
    assert(result(1).groupId === "SA1206949")
    assert(result(1).model.info.get.eventTimestamp === 1581638400000L)
    assert(result(1).model.info.get.insuranceStart.get === 1583539200000L)
    assert(result(1).model.info.get.insuranceEnd.get === 1614988800000L)
    assert(result(1).model.info.get.insuranceType === InsuranceType.OSAGO)

    assert(result(2).identifier.toString === "Z8NTBNT32ES020695")
    assert(result(2).groupId === "AB0030278")
    assert(result(2).model.info.get.eventTimestamp === 1581638400000L)
    assert(result(2).model.info.get.insuranceStart.get === 1583539200000L)
    assert(result(2).model.info.get.insuranceEnd.get === 1614988800000L)
    assert(result(2).model.info.get.insuranceType === InsuranceType.UNKNOWN_INSURANCE)
  }

  test("parse json with errors/warnings") {

    val rawInputStream = getClass.getResourceAsStream("/autoru/insurances/insurance_with_errors.json")

    val result = jsonManager.parseFile(rawInputStream, "ROLF_5_20200420-181759.json").toList

    val errors = result.collect { case Left(v) =>
      v
    }
    val successes = result.collect { case Right(v) =>
      v
    }

    assert(errors.size == 4)
    assert(successes.size == 1)

    assert(errors(0).asInstanceOf[DateFieldError].field === "INSURANCE_END")

    assert(errors(1).asInstanceOf[LimitedFieldError[_]].field === "INSURANCE_TYPE")

    assert(errors(2).isInstanceOf[ValidationRecordError])

    assert(errors(3).isInstanceOf[ValidationRecordError])

    assert(errors(3).asInstanceOf[DateFieldError].field === "INSURANCE_START")

    assert(errors(3).asInstanceOf[DateFieldError].value === "0014-02-01")

    assert(successes.head.warnings.head.isInstanceOf[ValidationRecordError])
  }

  test("parse xml with errors/warnings") {

    val rawInputStream = getClass.getResourceAsStream("/autoru/insurances/insurance_with_errors.xml")

    val result = xmlManager.parseFile(rawInputStream, "ROLF_5_20200420-181759.xml").toList

    val errors = result.collect { case Left(v) =>
      v
    }
    val successes = result.collect { case Right(v) =>
      v
    }

    assert(errors.size == 2)

    assert(errors(0).asInstanceOf[DateFieldError].field === "INSURANCE_END")

    assert(errors(1).asInstanceOf[LimitedFieldError[_]].field === "INSURANCE_TYPE")

    assert(successes.size == 1)

    assert(successes.head.warnings.head.isInstanceOf[ValidationRecordError])
    assert(successes.head.warnings.head.message == s"$InsuranceStartField is missing")

  }
}
