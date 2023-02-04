package ru.yandex.auto.vin.decoder.raw.managers

import auto.carfax.common.utils.misc.ResourceUtils
import org.scalatest.Ignore
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import ru.yandex.auto.vin.decoder.raw.autodealer.AutodealerRawModelManager

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.jdk.CollectionConverters.ListHasAsScala

@Ignore
class AutodealerRawModelManagerTest extends AnyFunSuite {

  val manager = new AutodealerRawModelManager

  test("correct parse and remove duplicates") {
    val raw = ResourceUtils.getStringFromResources("/autodealer/autodealer_service_book.json")

    val parsed = manager.parse(raw, "", "").toOption.get
    val prepared = Await.result(manager.convert(parsed), 1.second)

    val sb = prepared.getServiceBook
    val orders = sb.getOrdersList.asScala

    assert(prepared.getVin === "19XFB2670DE800428")
    assert(prepared.getStatus === VinInfoHistory.Status.OK)

    assert(sb.getMark === "Honda")
    assert(sb.getModel === "Civic")
    assert(sb.getYear === 2013)

    assert(orders.size === 1)
    assert(orders(0).getMileage === 120)
    assert(orders(0).getOrderDate === 1478379600000L)
    assert(orders(0).getStoCity === "")
    assert(orders(0).getProductsCount === 2)
    assert(orders(0).getServicesCount === 3)
  }

}
