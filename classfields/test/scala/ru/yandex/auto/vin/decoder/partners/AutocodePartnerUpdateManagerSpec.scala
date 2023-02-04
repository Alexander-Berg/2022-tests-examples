package ru.yandex.auto.vin.decoder.partners

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.partners.autocode.AutocodeReportType
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.{AutocodeReport, AutocodeState, CompoundState}
import ru.yandex.auto.vin.decoder.scheduler.models.WatchingStateUpdate
import ru.yandex.auto.vin.decoder.state.{Autocode, PartnerRequestTrigger}
import ru.yandex.auto.vin.decoder.utils.scheduler.PartnerUtils._

import scala.concurrent.duration._
import scala.language.postfixOps

class AutocodePartnerUpdateManagerSpec extends AnyWordSpecLike with MockitoSugar with Matchers {
  implicit val partner: Autocode = new Autocode
  implicit val partnerRequestTrigger: PartnerRequestTrigger = PartnerRequestTrigger.Unknown

  val autocodeUpdateManager = new PartnerUpdateManager[
    AutocodeReportType[_],
    AutocodeState,
    AutocodeState.Builder,
    AutocodeReport,
    AutocodeReport.Builder
  ]

  def makeState(reports: AutocodeReport*): WatchingStateUpdate[CompoundState] = {
    val c = CompoundState.newBuilder()
    val a = c.getAutocodeStateBuilder
    reports.foreach(a.addAutocodeReports)
    WatchingStateUpdate.defaultSync(c.build())
  }

  def buildReport(
      reportId: String,
      requestSent: Long,
      reportArrived: Long,
      noInfo: Boolean = false,
      shouldProcess: Boolean = false,
      forceUpdate: Boolean = false): AutocodeReport = {
    AutocodeReport
      .newBuilder()
      .setReportType(reportId)
      .setRequestSent(requestSent)
      .setReportArrived(reportArrived)
      .setNoInfo(noInfo)
      .setShouldProcess(shouldProcess)
      .setForceUpdate(forceUpdate)
      .build()
  }

  "update" should {
    "update MainUpdate type if it is present" in {
      val report = buildReport(AutocodeReportType.MainUpdate.id, 0, 1)
      val upd = makeState(report)
      val result = autocodeUpdateManager.update(upd, None)
      result.isDefined shouldBe true
      result
        .flatMap(_.state.getAutocodeState.findReport(AutocodeReportType.MainUpdate.id))
        .exists(_.getForceUpdate) shouldBe true
    }

    "not update MainUpdate type if it is up to date" in {
      val report = buildReport(AutocodeReportType.MainUpdate.id, 0, System.currentTimeMillis())
      val reportM = buildReport(AutocodeReportType.Main.id, 0, 1)
      val upd = makeState(report, reportM)
      val result = autocodeUpdateManager.update(upd, None)
      result.isDefined shouldBe false
    }

    "update MainUpdate type if it is up to date, but custom update interval is expired" in {
      val report = buildReport(AutocodeReportType.MainUpdate.id, 0, System.currentTimeMillis() - 2.hours.toMillis)
      val reportM = buildReport(AutocodeReportType.Main.id, 0, 1)
      val upd = makeState(report, reportM)
      val result = autocodeUpdateManager.update(upd, Some(1 hour))
      result.isDefined shouldBe true
      result
        .flatMap(_.state.getAutocodeState.findReport(AutocodeReportType.MainUpdate.id))
        .exists(_.getForceUpdate) shouldBe true
    }

    "not update MainUpdate type if it is expired, but custom update interval is not" in {
      val report = buildReport(AutocodeReportType.MainUpdate.id, 0, System.currentTimeMillis() - 365.days.toMillis)
      val reportM = buildReport(AutocodeReportType.Main.id, 0, 1)
      val upd = makeState(report, reportM)
      val result = autocodeUpdateManager.update(upd, Some(1000 days))
      result.isDefined shouldBe false
    }

    "create MainUpdate type if has  main" in {
      val reportM = buildReport(AutocodeReportType.Main.id, 0, 1)
      val upd = makeState(reportM)
      val result = autocodeUpdateManager.update(upd, None)
      result.isDefined shouldBe true
      result
        .flatMap(_.state.getAutocodeState.findReport(AutocodeReportType.MainUpdate.id))
        .exists(_.getShouldProcess) shouldBe true
    }

    "don't create  MainUpdate type if has main up to date" in {
      val reportM = buildReport(AutocodeReportType.Main.id, 0, System.currentTimeMillis())
      val upd = makeState(reportM)
      val result = autocodeUpdateManager.update(upd, None)
      result.isDefined shouldBe false
    }

    "don't create MainUpdate type if has main in progress" in {
      val reportM = buildReport(AutocodeReportType.Main.id, 5000, 0)
      val upd = makeState(reportM)
      val result = autocodeUpdateManager.update(upd, None)
      result.isDefined shouldBe false
    }

    "don't create MainUpdate type if has should process" in {
      val reportM = buildReport(AutocodeReportType.Main.id, 0, 0, shouldProcess = true)
      val upd = makeState(reportM)
      val result = autocodeUpdateManager.update(upd, None)
      result.isDefined shouldBe false
    }

    "don't create MainUpdate type if has force update" in {
      val reportM = buildReport(AutocodeReportType.Main.id, 0, 0, forceUpdate = true)
      val upd = makeState(reportM)
      val result = autocodeUpdateManager.update(upd, None)
      result.isDefined shouldBe false
    }

    "create Main if there is no reports" in {
      val upd = makeState()
      val result = autocodeUpdateManager.update(upd, None)
      result.isDefined shouldBe true
      result
        .flatMap(_.state.getAutocodeState.findReport(AutocodeReportType.Main.id))
        .exists(_.getShouldProcess) shouldBe true
    }

    "update with Main if previous main has no Info" in {
      val reportM = buildReport(AutocodeReportType.Main.id, 0, 1, noInfo = true)
      val upd = makeState(reportM)
      val result = autocodeUpdateManager.update(upd, None)
      result.isDefined shouldBe true
      result
        .flatMap(_.state.getAutocodeState.findReport(AutocodeReportType.Main.id))
        .exists(_.getForceUpdate) shouldBe true
    }
  }

  "update by report type" should {

    "update report if there is not report" in {
      val upd = makeState()
      val result = autocodeUpdateManager.update(upd, AutocodeReportType.TechInspections, 1.day)
      result.isDefined shouldBe true
      result
        .flatMap(_.state.getAutocodeState.findReport(AutocodeReportType.TechInspections.id))
        .exists(_.getShouldProcess) shouldBe true
    }

    "update report if last update was more then update interval ago" in {
      val reportM = buildReport(
        AutocodeReportType.TechInspections.id,
        System.currentTimeMillis() - 25.hours.toMillis,
        System.currentTimeMillis() - 25.hours.toMillis,
        shouldProcess = false
      )
      val upd = makeState(reportM)
      val result = autocodeUpdateManager.update(upd, AutocodeReportType.TechInspections, 1.day)
      result.isDefined shouldBe true
      result
        .flatMap(_.state.getAutocodeState.findReport(AutocodeReportType.TechInspections.id))
        .exists(_.getShouldProcess) shouldBe true
    }
    "don't update report if last update was early then update interval ago" in {
      val reportM = buildReport(
        AutocodeReportType.TechInspections.id,
        System.currentTimeMillis() - 20.hours.toMillis,
        System.currentTimeMillis() - 20.hours.toMillis,
        shouldProcess = false
      )
      val upd = makeState(reportM)
      val result = autocodeUpdateManager.update(upd, AutocodeReportType.TechInspections, 1.day)
      result.isDefined shouldBe false
      result
        .flatMap(_.state.getAutocodeState.findReport(AutocodeReportType.TechInspections.id))
        .exists(_.getShouldProcess) shouldBe false
    }

    "don't update report if already should process" in {
      val reportM = buildReport(
        AutocodeReportType.TechInspections.id,
        System.currentTimeMillis() - 25.hours.toMillis,
        System.currentTimeMillis() - 25.hours.toMillis,
        shouldProcess = true
      )
      val upd = makeState(reportM)
      val result = autocodeUpdateManager.update(upd, AutocodeReportType.TechInspections, 1.day)
      result.isDefined shouldBe false
      result
        .flatMap(_.state.getAutocodeState.findReport(AutocodeReportType.TechInspections.id))
        .exists(_.getShouldProcess) shouldBe false
    }
  }

  "forceUpdate" should {
    "create Main if there is no reports" in {
      val upd = makeState()
      val result = autocodeUpdateManager.forceUpdate(upd)
      result.isDefined shouldBe true
      result
        .flatMap(_.state.getAutocodeState.findReport(AutocodeReportType.Main.id))
        .exists(_.getForceUpdate) shouldBe true
    }

    "update Main if it was without info" in {
      val reportM = buildReport(AutocodeReportType.Main.id, 0, 1, noInfo = true)
      val upd = makeState(reportM)
      val result = autocodeUpdateManager.forceUpdate(upd)
      result.isDefined shouldBe true
      result
        .flatMap(_.state.getAutocodeState.findReport(AutocodeReportType.Main.id))
        .exists(_.getForceUpdate) shouldBe true
    }

    "don't create MainUpdate if  Main is fresh" in {
      val reportM = buildReport(AutocodeReportType.Main.id, 0, System.currentTimeMillis())
      val upd = makeState(reportM)
      val result = autocodeUpdateManager.forceUpdate(upd)
      result.isDefined shouldBe false
    }

    "don't create MainUpdate if Main is updating" in {
      val reportM = buildReport(AutocodeReportType.Main.id, 5000, 0)
      val upd = makeState(reportM)
      val result = autocodeUpdateManager.forceUpdate(upd)
      result.isDefined shouldBe false
    }

    "create MainUpdate if Main is out of date" in {
      val reportM = buildReport(AutocodeReportType.Main.id, 0, 1)
      val upd = makeState(reportM)
      val result = autocodeUpdateManager.forceUpdate(upd)
      result.isDefined shouldBe true
      result
        .flatMap(_.state.getAutocodeState.findReport(AutocodeReportType.MainUpdate.id))
        .exists(_.getForceUpdate) shouldBe true
      result
        .flatMap(_.state.getAutocodeState.findReport(AutocodeReportType.Main.id))
        .exists(_.getForceUpdate) shouldBe false
    }

    "don't create MainUpdate  if it is updating" in {
      val reportM = buildReport(AutocodeReportType.MainUpdate.id, 5000, 0)
      val upd = makeState(reportM)
      val result = autocodeUpdateManager.forceUpdate(upd)
      result.isDefined shouldBe false
    }

    "don't create MainUpdate  if it is fresh" in {
      val reportM = buildReport(AutocodeReportType.MainUpdate.id, 5000, System.currentTimeMillis())
      val upd = makeState(reportM)
      val result = autocodeUpdateManager.forceUpdate(upd)
      result.isDefined shouldBe false
    }

    "create MainUpdate if it is outdated" in {
      val reportM =
        buildReport(AutocodeReportType.MainUpdate.id, 5000, System.currentTimeMillis() - 61.minutes.toMillis)
      val upd = makeState(reportM)
      val result = autocodeUpdateManager.forceUpdate(upd)
      result.isDefined shouldBe true
      result
        .flatMap(_.state.getAutocodeState.findReport(AutocodeReportType.MainUpdate.id))
        .exists(_.getForceUpdate) shouldBe true
    }
  }
}
