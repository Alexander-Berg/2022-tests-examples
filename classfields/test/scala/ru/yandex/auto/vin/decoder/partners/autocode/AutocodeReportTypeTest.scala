package ru.yandex.auto.vin.decoder.partners.autocode

import org.scalatest.funsuite.AnyFunSuite

class AutocodeReportTypeTest extends AnyFunSuite {

  test("getReportType") {
    assert(
      AutocodeReportType.getReportType(
        "autoru_techdata_report_1FAFP36353W212389@autoru"
      ) == "autoru_techdata_report@autoru"
    )
    assert(
      AutocodeReportType.getReportType(
        "autoru_1_sources_eyJ0eXBlIjoiR1JaIiwiYm9keSI6ItCdNDIw0JrQnTQ1In0=@autoru"
      ) == "autoru_1_sources@autoru"
    )
    assert(
      AutocodeReportType.getReportType(
        "autoru_full_set_of_documents_for_vehicle_WBA3Y510X0GZ92038@autoru"
      ) == "autoru_full_set_of_documents_for_vehicle@autoru"
    )
    assert(
      AutocodeReportType.getReportType(
        "autoru_taxi_history_eyJ0eXBlIjoiR1JaIiwiYm9keSI6ItCd0KAyMDY3NyIsInNjaGVtYV92ZXJzaW9uIjoiMS4wIiwic3RvcmFnZXMiOnt9fQ==@autoru"
      ) == "autoru_taxi_history@autoru"
    )
    assert(
      AutocodeReportType.getReportType(
        "autoru_mileages_report_JTJCV00W704001364@autoru"
      ) == "autoru_mileages_report@autoru"
    )
    assert(
      AutocodeReportType.getReportType(
        "autoru_main_update_report_JMBSTCY4A8U001750@autoru"
      ) == "autoru_main_update_report@autoru"
    )
    assert(
      AutocodeReportType.getReportType(
        "autoru_main_report_Z8NFBAJ11ES017019@autoru"
      ) == "autoru_main_report@autoru"
    )
  }
}
