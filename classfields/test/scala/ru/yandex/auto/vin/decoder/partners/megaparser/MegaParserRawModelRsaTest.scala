package ru.yandex.auto.vin.decoder.partners.megaparser

import auto.carfax.common.utils.misc.ResourceUtils
import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.LicensePlate
import ru.yandex.auto.vin.decoder.partners.megaparser.exceptions.ReportNotReadyException
import ru.yandex.auto.vin.decoder.partners.megaparser.model.MegaParserRsaReportType.{
  currentInsurances,
  insuranceDetails
}
import ru.yandex.auto.vin.decoder.partners.megaparser.model.rsa.CurrentInsurancesResponse
import ru.yandex.auto.vin.decoder.partners.megaparser.model.{MegaParserReportResponse, ReportInProgress}

class MegaParserRawModelRsaTest extends AnyFunSuite {
  private val lp = LicensePlate("H660PH716")
  implicit private val t: Traced = Traced.empty

  test("successful response") {
    val raw = ResourceUtils.getStringFromResources("/megaparser/success.json")
    val rawModel = currentInsurances.parse(lp, 200, raw)

    assert(rawModel.response.head.currentInsurances.size == 2)
  }

  test("successful curr ins response") {
    val raw = ResourceUtils.getStringFromResources("/megaparser/success_curr_ins.json")
    val rawModel = currentInsurances.parse(lp, 200, raw)

    assert(rawModel.response.head.currentInsurances.size == 1)
  }

  test("successful ins details response") {
    val raw = ResourceUtils.getStringFromResources("/megaparser/success_ins_det.json")
    val rawModel = insuranceDetails.parse(lp, 200, raw)

    assert(rawModel.response.head.policyStatus.nonEmpty)
  }

  test("empty response for current insurances") {
    val raw = ResourceUtils.getStringFromResources("/megaparser/nodata.json")
    val rawModel = currentInsurances.parse(lp, 200, raw)
    assert(rawModel.response.isEmpty)
  }

  test("empty response for details") {
    val raw = ResourceUtils.getStringFromResources("/megaparser/nodata.json")
    val rawModel = insuranceDetails.parse(lp, 200, raw)

    assert(rawModel.response.isEmpty)
  }

  test("in progress response") {
    val raw = ResourceUtils.getStringFromResources("/megaparser/inprogress.json")
    val resp = MegaParserReportResponse.parse[LicensePlate, CurrentInsurancesResponse](
      200,
      lp,
      raw,
      List("_rsa"),
      currentInsurances.eventType
    )

    assert(resp match {
      case ReportInProgress(identifier, _) => identifier == lp
      case _ => false
    })
  }

  test("successful details response") {
    val raw = ResourceUtils.getStringFromResources("/megaparser/success_details.json")
    val rawModel = insuranceDetails.parse(lp, 200, raw)

    assert(
      (rawModel.response.head.policyStatus == "Выдан страхователю") &&
        (rawModel.response.head.changeDate == "17.02.2022") &&
        rawModel.response.head.vin.contains("WAUZZZ8R4HA014006")
    )

  }
}
