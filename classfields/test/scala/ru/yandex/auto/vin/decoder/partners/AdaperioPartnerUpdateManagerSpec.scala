package ru.yandex.auto.vin.decoder.partners

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import ru.yandex.auto.vin.decoder.model.scheduler.cs
import ru.yandex.auto.vin.decoder.partners.adaperio.AdaperioReportType
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.{AdaperioState, CompoundState}
import ru.yandex.auto.vin.decoder.scheduler.models.WatchingStateUpdate
import ru.yandex.auto.vin.decoder.state.{Adaperio, PartnerRequestTrigger}
import ru.yandex.auto.vin.decoder.utils.scheduler.PartnerUtils._

import scala.concurrent.duration._

class AdaperioPartnerUpdateManagerSpec extends AnyWordSpecLike with MockitoSugar with Matchers {
  implicit val partner: Adaperio = new Adaperio
  implicit val partnerRequestTrigger: PartnerRequestTrigger = PartnerRequestTrigger.Unknown

  val adaperio = new PartnerUpdateManager[
    AdaperioReportType,
    AdaperioState,
    AdaperioState.Builder,
    AdaperioState,
    AdaperioState.Builder
  ]

  def makeState(reports: AdaperioState*): WatchingStateUpdate[CompoundState] = {
    val compoundState = CompoundState.newBuilder()
    val state = reports
      .find(_.getReportType == AdaperioReportType.OldMain.toString)
      .map(compoundState.setAdaperio(_).getAdaperioBuilder)
      .getOrElse(compoundState.getAdaperioBuilder)

    reports
      .filter(_.getReportType != AdaperioReportType.OldMain.toString)
      .foreach(state.addAdaperioReports)
    WatchingStateUpdate.defaultSync(compoundState.build())
  }

  def buildReport(
      reportId: String,
      requestSent: Long,
      reportArrived: Long,
      noInfo: Boolean = false,
      shouldProcess: Boolean = false,
      forceUpdate: Boolean = false,
      noOrder: Boolean = false): AdaperioState = {
    require(reportArrived == 0 || !noOrder)

    AdaperioState
      .newBuilder()
      .setNoInfo(noInfo)
      .setNoOrder(noOrder)
      .setReportType(reportId)
      .setRequestSent(requestSent)
      .setReportArrived(reportArrived)
      .setShouldProcess(shouldProcess)
      .setForceUpdate(forceUpdate)
      .build()
  }

  "update" should {
    "update MainUpdate type if it is present" in {
      val report = buildReport(AdaperioReportType.MainUpdate, 0, 1)
      val upd = makeState(report)
      val result = adaperio.update(upd, None)
      result.isDefined shouldBe true
      result
        .flatMap(_.state.getAdaperio.findReport(AdaperioReportType.MainUpdate))
        .exists(_.getForceUpdate) shouldBe true
    }

    "not update MainUpdate type if it is up to date" in {
      val report = buildReport(AdaperioReportType.MainUpdate, 0, System.currentTimeMillis())
      val reportM = buildReport(AdaperioReportType.Main, 0, 1)
      val upd = makeState(report, reportM)
      val result = adaperio.update(upd, None)
      result.isDefined shouldBe false
    }

    "update MainUpdate type if it is up to date, but custom update interval is expired" in {
      val report = buildReport(AdaperioReportType.MainUpdate, 0, System.currentTimeMillis() - 2.hours.toMillis)
      val reportM = buildReport(AdaperioReportType.Main, 0, 1)
      val upd = makeState(report, reportM)
      val result = adaperio.update(upd, Some(1.hour))
      result.isDefined shouldBe true
      result
        .flatMap(_.state.getAdaperio.findReport(AdaperioReportType.MainUpdate))
        .exists(_.getForceUpdate) shouldBe true
    }

    "not update MainUpdate type if it is expired, but custom update interval is not" in {
      val report = buildReport(AdaperioReportType.MainUpdate, 0, System.currentTimeMillis() - 365.days.toMillis)
      val reportM = buildReport(AdaperioReportType.Main, 0, 1)
      val upd = makeState(report, reportM)
      val result = adaperio.update(upd, Some(1000.days))
      result.isDefined shouldBe false
    }

    "create MainUpdate type if has main" in {
      val reportM = buildReport(AdaperioReportType.Main, 0, 1)
      val upd = makeState(reportM)
      val result = adaperio.update(upd, None)
      result.isDefined shouldBe true
      result
        .flatMap(_.state.getAdaperio.findReport(AdaperioReportType.MainUpdate))
        .exists(_.getShouldProcess) shouldBe true
    }

    "don't create MainUpdate type if has main up to date" in {
      val reportM = buildReport(AdaperioReportType.Main, 0, System.currentTimeMillis())
      val upd = makeState(reportM)
      val result = adaperio.update(upd, None)
      result.isDefined shouldBe false
    }

    "don't create MainUpdate type if has main in progress" in {
      val reportM = buildReport(AdaperioReportType.Main, 5000, 0)
      val upd = makeState(reportM)
      val result = adaperio.update(upd, None)
      result.isDefined shouldBe false
    }

    "don't create  MainUpdate type if has should process" in {
      val reportM = buildReport(AdaperioReportType.Main, 0, 0, shouldProcess = true)
      val upd = makeState(reportM)
      val result = adaperio.update(upd, None)
      result.isDefined shouldBe false
    }

    "don't create  MainUpdate type if has force update" in {
      val reportM = buildReport(AdaperioReportType.Main, 0, 0, forceUpdate = true)
      val upd = makeState(reportM)
      val result = adaperio.update(upd, None)
      result.isDefined shouldBe false
    }

    "create Main if there is no reports" in {
      val upd = makeState()
      val result = adaperio.update(upd, None)
      result.isDefined shouldBe true
      result
        .flatMap(_.state.getAdaperio.findReport(AdaperioReportType.Main))
        .exists(_.getShouldProcess) shouldBe true
    }

    "create Main if there is old format outdated" in {
      val reportOld = buildReport(AdaperioReportType.OldMain, 0, 1L)
      val upd = makeState(reportOld)
      val result = adaperio.update(upd, None)
      result.isDefined shouldBe true
      result
        .flatMap(_.state.getAdaperio.findReport(AdaperioReportType.Main))
        .exists(_.getShouldProcess) shouldBe true
    }

    "create Main if there is old format up to date" in {
      val reportOld = buildReport(AdaperioReportType.OldMain, 0, System.currentTimeMillis())
      val upd = makeState(reportOld)
      val result = adaperio.update(upd, None)
      result.isDefined shouldBe true
      result
        .flatMap(_.state.getAdaperio.findReport(AdaperioReportType.Main))
        .exists(_.getShouldProcess) shouldBe true
    }

    "create Main if there is old format is in progress" in {
      val reportOld = buildReport(AdaperioReportType.OldMain, 50000, 0)
      val upd = makeState(reportOld)
      val result = adaperio.update(upd, None)
      result.isDefined shouldBe true
      result
        .flatMap(_.state.getAdaperio.findReport(AdaperioReportType.Main))
        .exists(_.getShouldProcess) shouldBe true
    }

    "create Main if there is old format with should process" in {
      val reportOld = buildReport(AdaperioReportType.OldMain, 0, 0, shouldProcess = true)
      val upd = makeState(reportOld)
      val result = adaperio.update(upd, None)
      result.isDefined shouldBe true
      result
        .flatMap(_.state.getAdaperio.findReport(AdaperioReportType.Main))
        .exists(_.getShouldProcess) shouldBe true
    }

    "update with Main if previous main has no Info" in {
      val reportM = buildReport(AdaperioReportType.Main, 0, 1, noInfo = true)
      val upd = makeState(reportM)
      val result = adaperio.update(upd, None)
      result.isDefined shouldBe true
      result
        .flatMap(_.state.getAdaperio.findReport(AdaperioReportType.Main))
        .exists(_.getForceUpdate) shouldBe true
    }
  }

  "forceUpdate" should {
    "create Main if there is no reports" in {
      val upd = makeState()
      val result = adaperio.forceUpdate(upd)
      result.isDefined shouldBe true
      result
        .flatMap(_.state.getAdaperio.findReport(AdaperioReportType.Main))
        .exists(_.getForceUpdate) shouldBe true
    }

    "create Main if there is old format older then one hour" in {
      val reportOld = buildReport(AdaperioReportType.OldMain, 0, System.currentTimeMillis() - 2.hours.toMillis)
      val upd = makeState(reportOld)
      val result = adaperio.forceUpdate(upd)
      result.isDefined shouldBe true
      result
        .flatMap(_.state.getAdaperio.findReport(AdaperioReportType.Main))
        .exists(_.getForceUpdate) shouldBe true
    }

    "create Main if there is old format fresh" in {
      val reportOld = buildReport(AdaperioReportType.OldMain, 0, System.currentTimeMillis())
      val upd = makeState(reportOld)
      val result = adaperio.forceUpdate(upd)
      result
        .flatMap(_.state.getAdaperio.findReport(AdaperioReportType.Main))
        .exists(_.getForceUpdate) shouldBe true
    }

    "create Main if there is old format is updating" in {
      val reportOld = buildReport(AdaperioReportType.OldMain, 10, 0)
      val upd = makeState(reportOld)
      val result = adaperio.forceUpdate(upd)
      result
        .flatMap(_.state.getAdaperio.findReport(AdaperioReportType.Main))
        .exists(_.getForceUpdate) shouldBe true
    }

    "update Main if it was without info" in {
      val reportM = buildReport(AdaperioReportType.Main, 0, 1, noInfo = true)
      val upd = makeState(reportM)
      val result = adaperio.forceUpdate(upd)
      result.isDefined shouldBe true
      result
        .flatMap(_.state.getAdaperio.findReport(AdaperioReportType.Main))
        .exists(_.getForceUpdate) shouldBe true
    }

    "don't create MainUpdate if  Main is fresh" in {
      val reportM = buildReport(AdaperioReportType.Main, 0, System.currentTimeMillis())
      val upd = makeState(reportM)
      val result = adaperio.forceUpdate(upd)
      result.isDefined shouldBe false
    }

    "don't create MainUpdate if Main is updating" in {
      val reportM = buildReport(AdaperioReportType.Main, 5000, 0)
      val upd = makeState(reportM)
      val result = adaperio.forceUpdate(upd)
      result.isDefined shouldBe false
    }

    "create MainUpdate if Main is out of date" in {
      val reportM = buildReport(AdaperioReportType.Main, 0, 1)
      val upd = makeState(reportM)
      val result = adaperio.forceUpdate(upd)
      result.isDefined shouldBe true
      result
        .flatMap(_.state.getAdaperio.findReport(AdaperioReportType.MainUpdate))
        .exists(_.getForceUpdate) shouldBe true
    }

    "don't create MainUpdate  if it is updating" in {
      val reportM = buildReport(AdaperioReportType.MainUpdate, 5000, 0)
      val upd = makeState(reportM)
      val result = adaperio.forceUpdate(upd)
      result.isDefined shouldBe false
    }

    "don't create MainUpdate  if it is fresh" in {
      val reportM = buildReport(AdaperioReportType.MainUpdate, 5000, System.currentTimeMillis())
      val upd = makeState(reportM)
      val result = adaperio.forceUpdate(upd)
      result.isDefined shouldBe false
    }

    "create MainUpdate if it is outdated" in {
      val reportM = buildReport(AdaperioReportType.MainUpdate, 5000, System.currentTimeMillis() - 61.minutes.toMillis)
      val upd = makeState(reportM)
      val result = adaperio.forceUpdate(upd)
      result.isDefined shouldBe true
      result
        .flatMap(_.state.getAdaperio.findReport(AdaperioReportType.MainUpdate))
        .exists(_.getForceUpdate) shouldBe true
    }

    "refetch main if no order" in {
      val report = buildReport(AdaperioReportType.Main, 1, 0, noOrder = true)
      val stateUpdate = makeState(report)
      val result = adaperio.forceUpdate(stateUpdate)

      result.isDefined shouldBe true
      result.foreach { stateUpdate =>
        val adaperioState = stateUpdate.state.getAdaperio
        adaperioState.findReport(AdaperioReportType.Main).exists(_.getForceUpdate) shouldBe true
        adaperioState.findReport(AdaperioReportType.MainUpdate) shouldBe None
      }
    }

    "don't update main if main already exists" in {
      val reportM = buildReport(AdaperioReportType.Main, 5000, System.currentTimeMillis() - 61.minutes.toMillis)
      val upd = makeState(reportM)
      val result = adaperio.forceUpdate(upd)
      result.isDefined shouldBe true
      result.foreach { stateUpdate =>
        val adaperioState = stateUpdate.state.getAdaperio
        adaperioState.findReport(AdaperioReportType.Main).exists(_.getForceUpdate) shouldBe false
        adaperioState.findReport(AdaperioReportType.Main).exists(_.getShouldProcess) shouldBe false

        adaperioState.findReport(AdaperioReportType.MainUpdate).exists(_.getForceUpdate) shouldBe true
      }
    }
  }
}
