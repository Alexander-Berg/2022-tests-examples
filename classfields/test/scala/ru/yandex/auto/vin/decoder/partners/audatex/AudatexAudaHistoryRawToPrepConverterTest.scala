package ru.yandex.auto.vin.decoder.partners.audatex

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.audatex.model.AudatexAudaHistoryRawModel
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils

class AudatexAudaHistoryRawToPrepConverterTest extends AnyFunSuite {

  private val vinCode = VinCode("SJNFDAJ11U1084830")
  private val converter = new AudatexAudaHistoryRawToPrepConverter
  implicit val t: Traced = Traced.empty

  test("success") {
    val raw = ResourceUtils.getStringFromResources(s"/audatex/auda_history/success.xml")

    val model = AudatexAudaHistoryRawModel(raw, 200, vinCode)
    val converted = converter.convert(model).await

    assert(converted.getVin == vinCode.toString)
    assert(converted.getEventType == EventType.AUDATEX)

    assert(converted.getAdaperioAudatexCount == 2)
    val report1 = converted.getAdaperioAudatex(0)
    assert(report1.getReport.getCalculation == "0008132368")
    assert(report1.getReport.getCalculated == 1378148400000L)
    assert(!report1.getReport.getTotalLoss)
    assert(report1.getReport.getLabourCost == 1.0)
    assert(report1.getReport.getPartsCost == 19997.0)
    assert(report1.getReport.getPaintCost == 0.0)
    assert(report1.getReport.getTotalCost == 19998.0)
    assert(report1.getReport.getMileage == 0)
    assert(report1.getReport.getWorksCount == 6)
    assert(report1.getReport.getWorks(0).getMethod == "Замена")
    assert(report1.getReport.getWorks(0).getDescription == "БАМПЕР П - С/У")
  }

  test("not found") {
    val raw = ResourceUtils.getStringFromResources(s"/audatex/auda_history/not_found.xml")

    val model = AudatexAudaHistoryRawModel(raw, 200, vinCode)
    val converted = converter.convert(model).await

    assert(converted.getVin == vinCode.toString)
    assert(converted.getEventType == EventType.AUDATEX)

    assert(converted.getAdaperioAudatexCount == 0)
  }
}
