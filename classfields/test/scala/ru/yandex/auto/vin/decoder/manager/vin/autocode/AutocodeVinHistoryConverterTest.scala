package ru.yandex.auto.vin.decoder.manager.vin.autocode

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json
import ru.yandex.auto.vin.decoder.partners.autocode.model._
import AutocodeReportResponse.autocodeFullSetTechDataReads
import auto.carfax.common.utils.misc.ResourceUtils

class AutocodeVinHistoryConverterTest extends AnyFunSuite with Matchers {

  test("Can parse main report without errors") {
    val jsonRaw = ResourceUtils.getStringFromResources(
      "/autocode/main/autoru_main_report_eyJ0eXBlIjoiQk9EWSIsImJvZHkiOiIwNTQyOTg3Iiwic2NoZW1hX3ZlcnNpb24iOiIxLjAiLCJzdG9yYWdlcyI6e319@autoru.json"
    )
    val response = Json.parse(jsonRaw).as[AutocodeReportResponse]
    AutocodeVinHistoryConverter.convertRegPeriods(response)
  }

  test("Can parse full set of documents without errors") {
    val jsonRaw = ResourceUtils.getStringFromResources(
      "/autocode/full_set_of_documents_for_vehicle/autoru_full_set_of_documents_for_vehicle_XTA210600N2822640@autoru.json"
    )
    val response = Json.parse(jsonRaw).as[AutocodeReportResponse]
    AutocodeVinHistoryConverter.convertRegPeriods(response)

    assert(response.data.content.get.fullSetTechData.isDefined)
    response.data.content.get.fullSetTechData.get should be(
      AutocodeReportFullSetTechData(Some("2106"), Some("2258400"))
    )
  }

  test("Can parse engine tech data without engine model") {
    val jsonRaw = ResourceUtils.getStringFromResources(
      "/autocode/full_set_of_documents_for_vehicle/tech_data_engine_without_model.json"
    )
    val response = Json.parse(jsonRaw).as[AutocodeReportFullSetTechData]

    response should be(
      AutocodeReportFullSetTechData(None, Some("2258400"))
    )
  }

  test("Can parse engine tech data without engine number") {
    val jsonRaw = ResourceUtils.getStringFromResources(
      "/autocode/full_set_of_documents_for_vehicle/tech_data_engine_without_number.json"
    )
    val response = Json.parse(jsonRaw).as[AutocodeReportFullSetTechData]

    response should be(
      AutocodeReportFullSetTechData(Some("2106"), None)
    )
  }

}
