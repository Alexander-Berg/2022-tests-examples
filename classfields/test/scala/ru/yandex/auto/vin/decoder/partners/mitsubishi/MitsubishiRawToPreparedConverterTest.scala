package ru.yandex.auto.vin.decoder.partners.mitsubishi

import auto.carfax.common.utils.tracing.Traced
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.mitsubishi.model.MitsubishiRawModel
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import auto.carfax.common.utils.concurrent.CoreFutureUtils.AwaitableFuture
import auto.carfax.common.utils.misc.ResourceUtils

import scala.jdk.CollectionConverters.ListHasAsScala

class MitsubishiRawToPreparedConverterTest extends AnyFunSuite {

  private val vinCode = VinCode("JMBSRCS3A5U010881")
  private val converter = new MitsubishiRawToPreparedConverter
  implicit val t: Traced = Traced.empty

  test("success") {
    val raw = ResourceUtils.getStringFromResources(s"/mitsubishi/success.json")
    val model = MitsubishiRawModel(raw, 200, vinCode)
    val converted = converter.convert(model).await

    assert(converted.getVin == vinCode.toString)
    assert(converted.getEventType == EventType.MITSUBISHI_SERVICE_BOOK)
    assert(converted.getStatus == VinInfoHistory.Status.OK)
    assert(converted.hasServiceBook)

    val orders = converted.getServiceBook.getOrdersList.asScala

    assert(orders.size == 2)
    assert(orders.head.getMileage == 187300)
    assert(orders(1).getMileage == 187313)
    assert(orders.head.getDescription == "Технические работы, не гарантийные")
    assert(orders(1).getDescription == "Гарантия")
    assert(orders.head.getOrderDate == 1560729600000L)
    assert(orders.head.getStoCity == "Санкт-Петербург")
  }

  test("not found") {
    val raw = ResourceUtils.getStringFromResources(s"/mitsubishi/not_found.json")
    val model = MitsubishiRawModel(raw, 404, vinCode)
    val converted = converter.convert(model).await

    assert(converted.getVin == vinCode.toString)
    assert(converted.getEventType == EventType.MITSUBISHI_SERVICE_BOOK)
    assert(!converted.hasServiceBook)
  }
}
