package ru.yandex.auto.vin.decoder.partners.infiniti

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils

class InfinitiRawToPreparedConverterTest extends AnyFunSuite {
  private val vinCode = VinCode("3PCMANJ55Z0552634")
  private val converter = new InfinitiRawToPreparedConvertor()
  val prefix = "/infiniti"
  implicit val t: Traced = Traced.empty

  test("parse success non empty response") {
    val rawStr = ResourceUtils.getStringFromResources(s"$prefix/found.json")
    val code = 200

    val parsed = InfinitiRawModel(rawStr, code, vinCode)
    val converted = converter.convert(parsed).await

    assert(parsed.identifier === vinCode)
    assert(parsed.rawStatus === "200")
    assert(parsed.visits.nonEmpty)

    assert(converted.getVin == vinCode.toString)
    assert(converted.getEventType == EventType.INFINITI_SERVICE_BOOK)
    assert(converted.hasServiceBook)
    assert(converted.getServiceBook.getOrdersCount == 2)

    assert(converted.getServiceBook.getOrders(0).getMileage == 10038L)
  }

  test("parse success empty response") {
    val code = 200

    val parsed = InfinitiRawModel("[]", code, vinCode)
    val converted = converter.convert(parsed).await

    assert(parsed.identifier === vinCode)
    assert(parsed.rawStatus === "200")
    assert(parsed.visits.isEmpty)

    assert(converted.getVin == vinCode.toString)
    assert(converted.getEventType == EventType.INFINITI_SERVICE_BOOK)
    assert(converted.getServiceBook.getOrdersCount == 0)
  }

}
