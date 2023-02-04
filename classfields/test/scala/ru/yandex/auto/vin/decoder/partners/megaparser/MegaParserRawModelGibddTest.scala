package ru.yandex.auto.vin.decoder.partners.megaparser

import auto.carfax.common.utils.misc.ResourceUtils
import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.gibdd.common.wanted.GibddWantedReportModel
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.megaparser.exceptions.ReportNotReadyException
import ru.yandex.auto.vin.decoder.partners.megaparser.model.MegaParserGibddReportType._
import ru.yandex.auto.vin.decoder.partners.megaparser.model.{
  MegaParserGibddReportType,
  MegaParserReportResponse,
  ReportInProgress
}

class MegaParserRawModelGibddTest extends AnyFunSuite {

  implicit val t = Traced.empty
  private val vin = VinCode("WAUZZZ8R4HA014006")

  test("successful constraints response") {
    val raw = ResourceUtils.getStringFromResources("/megaparser/success_gibdd.json")
    val rawModel = Constraints.parse(vin, 200, raw)

    assert(rawModel.response.head.records.isEmpty)
  }

  test("successful registrations response") {
    val raw = ResourceUtils.getStringFromResources("/megaparser/success_gibdd.json")
    val rawModel = Registration.parse(vin, 200, raw)

    assert(rawModel.response.head.periods.size == 2)
  }

  test("successful wanted response") {
    val raw = ResourceUtils.getStringFromResources("/megaparser/success_gibdd.json")
    val rawModel = Wanted.parse(vin, 200, raw)

    assert(rawModel.response.head.records.isEmpty)
  }

  test("successful accidents response") {
    val raw = ResourceUtils.getStringFromResources("/megaparser/success_gibdd.json")
    val rawModel = Accidents.parse(vin, 200, raw)

    assert(rawModel.response.head.accidents.size == 1)
  }

  test("no data wanted response") {
    val raw = ResourceUtils.getStringFromResources("/megaparser/nodata_gibdd.json")
    val rawModel = Wanted.parse(vin, 200, raw)

    assert(rawModel.response.isEmpty)
  }

  test("in progress response") {
    val raw = ResourceUtils.getStringFromResources("/megaparser/inprogress.json")
    val resp = MegaParserReportResponse.parse[VinCode, GibddWantedReportModel](
      200,
      vin,
      raw,
      List(""),
      MegaParserGibddReportType.Wanted.eventType
    )

    assert(resp match {
      case ReportInProgress(identifier, _) => identifier == vin
      case _ => false
    })

  }

}
