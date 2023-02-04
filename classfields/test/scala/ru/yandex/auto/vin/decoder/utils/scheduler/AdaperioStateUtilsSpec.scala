package ru.yandex.auto.vin.decoder.utils.scheduler

import org.scalatest.funspec.AnyFunSpec
import ru.yandex.auto.vin.decoder.partners.adaperio.AdaperioReportType
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.AdaperioState
import ru.yandex.auto.vin.decoder.utils.scheduler.PartnerUtils._

class AdaperioStateUtilsSpec extends AnyFunSpec {

  describe("RichAdaperioState.findReport") {
    val state = AdaperioState
      .newBuilder()
      .addAdaperioReports {
        AdaperioState
          .newBuilder()
          .setReportType(AdaperioReportType.Main.toString)
          .setShouldProcess(true)
      }
      .build()

    it("should return state") {
      assert(state.findReport(AdaperioReportType.Main).isDefined)
    }

    it("should not return state") {
      assert(state.findReport(AdaperioReportType.OldMain).isEmpty)
      assert(state.findReport(AdaperioReportType.MainUpdate).isEmpty)
    }
  }

  describe("RichAdaperioStateBuilder.findReportBuilder") {
    val stateBuilder = AdaperioState
      .newBuilder()
      .addAdaperioReports {
        AdaperioState
          .newBuilder()
          .setReportType(AdaperioReportType.Main.toString)
          .setShouldProcess(true)
      }

    it("should return state builder") {
      assert(stateBuilder.findReportBuilder(AdaperioReportType.Main).isDefined)
    }

    it("should not reeturn state builder") {
      assert(stateBuilder.findReportBuilder(AdaperioReportType.OldMain).isEmpty)
      assert(stateBuilder.findReportBuilder(AdaperioReportType.MainUpdate).isEmpty)
    }
  }
}
