package ru.yandex.auto.vin.decoder.scheduler.workers.partners.rsa

import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.scrapinghub.rsa.ScrapinghubRsaReportType
import ru.yandex.auto.vin.decoder.proto.SchedulerModel.CompoundState
import ru.yandex.auto.vin.decoder.proto.VinHistory.{Insurance, VinInfoHistory}
import ru.yandex.auto.vin.decoder.scheduler.models.WatchingStateHolder
import ru.yandex.auto.vin.decoder.scheduler.workers.WorkResult
import ru.yandex.auto.vin.decoder.utils.scheduler.PartnerUtils._
import ru.yandex.auto.vin.decoder.ydb.raw.RawStorageManager.Prepared

import scala.io.Source

trait RsaWorkerSpec {

  def buildPrepared = {
    val insurance = Insurance.getDefaultInstance
    val info = VinInfoHistory.newBuilder().addInsurances(insurance).build
    Prepared(0, 0, 0, info, "")
  }

  def getReportState(
      workResult: WorkResult[CompoundState],
      holder: WatchingStateHolder[VinCode, CompoundState],
      reportType: ScrapinghubRsaReportType) =
    workResult.updater
      .get(holder.toUpdate)
      .state
      .getScrapinghubRsaState
      .findReport(reportType.toString)
      .get

  def getStringFromResouce(path: String): String = {
    val stream = getClass.getResourceAsStream(path)
    val result = Source.fromInputStream(stream, "UTF-8").mkString
    stream.close()
    result
  }
}
