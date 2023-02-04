package ru.yandex.auto.vin.decoder.utils

import org.scalatest.funspec.AnyFunSpec
import ru.yandex.auto.vin.decoder.partners.autocode.AutocodeReportType
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.AutocodeState
import ru.yandex.auto.vin.decoder.utils.scheduler.PartnerUtils._

class AutocodeStateUtilsTest extends AnyFunSpec {

  describe("RichAutocodeStateBuilder") {
    it("getReportBuilder") {
      val stateBuilder = AutocodeState.newBuilder()

      val lpReport = stateBuilder.getReportBuilder(AutocodeReportType.Identifiers)
      assert(stateBuilder.findReportBuilder(AutocodeReportType.Identifiers).contains(lpReport))

      val eaistoReport = stateBuilder.getReportBuilder(AutocodeReportType.TechInspections)
      assert(stateBuilder.findReportBuilder(AutocodeReportType.TechInspections).contains(eaistoReport))
    }
  }
}
