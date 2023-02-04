package ru.yandex.auto.vin.decoder.partners.jlr

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils

class JlrRawToPreparedConverterTest extends AnyFunSuite {
  private val vinCode = VinCode.apply("SALGA2FF8EA157860")
  private val converter = new JlrRawToPreparedConverter
  implicit val t: Traced = Traced.empty

  test("parse success non empty response") {
    val rawStr = ResourceUtils.getStringFromResources("/jlr/found.json")
    val code = 200

    val parsed = JlrRawModel.apply(rawStr, code, vinCode)
    val converted = converter.convert(parsed).await

    assert(parsed.identifier === vinCode)
    assert(parsed.rawStatus === "200")
    assert(parsed.data.repairs.nonEmpty)

    assert(converted.getVin == vinCode.toString)
    assert(converted.getEventType == EventType.JLR_SERVICE_BOOK)
    assert(converted.hasServiceBook)
    assert(converted.getServiceBook.getOrdersCount == 9)

    assert(converted.getServiceBook.getOrders(0).getMileage == 5)
    assert(converted.getServiceBook.getOrders(0).getProductsCount == 4)
    assert(converted.getServiceBook.getOrders(0).getServicesCount == 2)
  }

  test("parse success empty response") {
    val rawStr = ResourceUtils.getStringFromResources("/jlr/not_found.json")
    val code = 200

    val parsed = JlrRawModel.apply(rawStr, code, vinCode)
    val converted = converter.convert(parsed).await

    assert(parsed.identifier === vinCode)
    assert(parsed.rawStatus === "200")
    assert(parsed.data.repairs.isEmpty)

    assert(converted.getVin == vinCode.toString)
    assert(converted.getEventType == EventType.JLR_SERVICE_BOOK)
    assert(!converted.hasServiceBook)
  }

}
