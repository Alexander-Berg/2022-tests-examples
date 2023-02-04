package ru.yandex.auto.vin.decoder.raw.autoru

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.raw.FileFormats
import ru.yandex.auto.vin.decoder.raw.autoru.services.AutoruFormatSbRawModelManager
import ru.yandex.auto.vin.decoder.raw.validation.ValidationFieldErrors.{LimitedFieldError, RequiredFieldError}

class AutoruFormatSbRawModelManagerTest extends AnyFunSuite {

  private val jsonManager = new AutoruFormatSbRawModelManager(EventType.AXSEL_SERVICE_BOOK, FileFormats.Json)
  private val xmlManager = new AutoruFormatSbRawModelManager(EventType.AXSEL_SERVICE_BOOK, FileFormats.Xml)
  private val csvManager = new AutoruFormatSbRawModelManager(EventType.AXSEL_SERVICE_BOOK, FileFormats.Csv)

  test("parse json") {
    val rawInputStream = getClass.getResourceAsStream("/autoru/services/ROLF_2_20200525-102513.json")

    val result = jsonManager.parseFile(rawInputStream, "ROLF_2_20200525-102513.json").toList.collect { case Right(v) =>
      v
    }

    assert(result.size === 2)

    assert(result(0).identifier.toString === "X4XYB81160D247134")
    assert(result(0).groupId === "AMV0378172")
    assert(result(0).model.info.nonEmpty)
    assert(result(0).model.info.get.works.size === 3)
    assert(result(0).model.info.get.products.size === 3)

    assert(result(1).identifier.toString === "Z8NTBNT31CS046950")
    assert(result(1).groupId === "387630-100")
    assert(result(1).model.info.nonEmpty)
    assert(result(1).model.info.get.works.size === 2)
    assert(result(1).model.info.get.products.size === 1)
  }

  test("parse xml") {
    val rawInputStream = getClass.getResourceAsStream("/autoru/services/ROLF_2_20200525-102513.xml")

    val result = xmlManager.parseFile(rawInputStream, "ROLF_2_20200525-102513.xml").toList.collect { case Right(v) =>
      v
    }

    assert(result.size === 2)

    assert(result(0).identifier.toString === "X4XYB81160D247134")
    assert(result(0).groupId === "AMV0378172")
    assert(result(0).model.info.nonEmpty)
    assert(result(0).model.info.get.works.size === 3)
    assert(result(0).model.info.get.products.size === 3)

    assert(result(1).identifier.toString === "Z8NTBNT31CS046950")
    assert(result(1).groupId === "387630-100")
    assert(result(1).model.info.nonEmpty)
    assert(result(1).model.info.get.works.size === 2)
    assert(result(1).model.info.get.products.size === 1)
  }

  test("parse csv") {
    val rawInputStream = getClass.getResourceAsStream("/autoru/services/ROLF_2_20200525-102513.csv")

    val result = csvManager.parseFile(rawInputStream, "ROLF_2_20200525-102513.csv").toList.collect { case Right(v) =>
      v
    }

    assert(result.size === 2)

    assert(result(0).identifier.toString === "X4XYB81160D247134")
    assert(result(0).groupId === "AMV0378172")
    assert(result(0).model.info.nonEmpty)
    assert(result(0).model.info.get.works.size === 3)
    assert(result(0).model.info.get.products.size === 3)

    assert(result(1).identifier.toString === "Z8NTBNT31CS046950")
    assert(result(1).groupId === "387630-100")
    assert(result(1).model.info.nonEmpty)
    assert(result(1).model.info.get.works.size === 2)
    assert(result(1).model.info.get.products.size === 1)
  }

  test("parse json with errors") {
    val rawInputStream = getClass.getResourceAsStream("/autoru/services/services_with_errors.json")

    val result = jsonManager.parseFile(rawInputStream, "services_with_errors.json").toList.collect { case Left(v) =>
      v
    }

    assert(result.size === 3)

    assert(result(0).asInstanceOf[RequiredFieldError].field === "WORK.NAME")

    assert(result(1).asInstanceOf[RequiredFieldError].field === "PRODUCT.NAME")

    assert(result(2).asInstanceOf[LimitedFieldError[_]].field === "TYPE")
  }
}
