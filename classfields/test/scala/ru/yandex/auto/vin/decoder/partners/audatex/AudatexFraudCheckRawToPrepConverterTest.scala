package ru.yandex.auto.vin.decoder.partners.audatex

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.audatex.model.AudatexFraudCheckRawModel
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils

import scala.jdk.CollectionConverters._

class AudatexFraudCheckRawToPrepConverterTest extends AnyFunSuite {

  private val vinCode = VinCode("SJNFDAJ11U1084830")
  private val converter = new AudatexFraudCheckRawToPrepConverter
  implicit val t: Traced = Traced.empty

  test("success") {
    val raw = ResourceUtils.getStringFromResources(s"/audatex/fraud_check/success.xml")

    val model = AudatexFraudCheckRawModel(raw, 200, vinCode)
    val converted = converter.convert(model).await

    assert(converted.getVin == vinCode.toString)
    assert(converted.getEventType == EventType.AUDATEX_FRAUD_CHECK)

    assert(converted.getUremontAuction.getLotCount == 1)
    val lot = converted.getUremontAuction.getLotList.asScala.head
    assert(lot.getAuctionRegion == "Индия")
    assert(lot.getAuctionDate == 1544648400000L)
    assert(lot.getMileage == 150000)
  }

  test("not found") {
    val raw = ResourceUtils.getStringFromResources(s"/audatex/fraud_check/not_found.xml")

    val model = AudatexFraudCheckRawModel(raw, 200, vinCode)
    val converted = converter.convert(model).await

    assert(converted.getVin == vinCode.toString)
    assert(converted.getEventType == EventType.AUDATEX_FRAUD_CHECK)

    assert(converted.getUremontAuction.getLotCount == 0)
  }
}
