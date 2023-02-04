package ru.yandex.auto.vin.decoder.raw.managers

import auto.carfax.common.utils.misc.ResourceUtils
import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.vin.event.VinReportEventType.EventType
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import ru.yandex.auto.vin.decoder.raw.euroauto.EuroAutoRawModelManager

import scala.concurrent.Await
import scala.concurrent.duration._

class EuroAutoRawModelManagerTest extends AnyFunSuite {

  val manager = new EuroAutoRawModelManager

  test("correct convert") {
    val raw = ResourceUtils.getStringFromResources("/euroauto/euro_auto_service_book.json")
    val model = manager.parse(raw, "", "").toOption.get
    val converted = Await.result(manager.convert(model), 10.seconds)

    assert(converted.getVin === "WV1ZZZ2EZ86021041")
    assert(converted.getGroupId === "62899-305")
    assert(converted.getEventType === EventType.EUROAUTO_SERVICE_BOOK)
    assert(converted.getStatus === VinInfoHistory.Status.OK)

    val sb = converted.getServiceBook
    assert(sb.getMark === "VW")
    assert(sb.getModel === "Crafter")
    assert(sb.getYear === 2007)

    assert(sb.getOrdersCount === 1)
    assert(sb.getOrders(0).getMileage === 320003)
    assert(sb.getOrders(0).getOrderDate === 1528243200000L)
    assert(sb.getOrders(0).getServicesCount === 7)
    assert(sb.getOrders(0).getProductsCount === 9)
  }

  test("correct convert deleted") {
    val raw = ResourceUtils.getStringFromResources("/euroauto/euro_auto_service_book_deleted.json")
    val rawModel = manager.parse(raw, "", "").toOption.get
    val converted = Await.result(manager.convert(rawModel), 10.seconds)

    assert(converted.getVin === "WV1ZZZ2EZ86021041")
    assert(converted.getGroupId === "62899-305")
    assert(converted.getEventType === EventType.EUROAUTO_SERVICE_BOOK)
    assert(converted.getStatus === VinInfoHistory.Status.OK)
    assert(converted.hasServiceBook === false)
  }

}
