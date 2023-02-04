package ru.yandex.auto.vin.decoder.scheduler.stage.partners

import ru.yandex.auto.vin.decoder.proto.SchedulerModel.AutocodeReport

trait AutocodeStageUtils {

  def createAutocodeReport(reportId: String, requestSent: Long, reportArrived: Long = 0): AutocodeReport = {
    AutocodeReport
      .newBuilder()
      .setReportId(reportId)
      .setRequestSent(requestSent)
      .setReportArrived(reportArrived)
      .build()
  }
}
