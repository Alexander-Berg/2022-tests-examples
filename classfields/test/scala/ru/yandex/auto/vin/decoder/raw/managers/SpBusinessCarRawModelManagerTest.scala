package ru.yandex.auto.vin.decoder.raw.managers

import auto.carfax.common.utils.misc.ResourceUtils
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.proto.VinHistory.VinInfoHistory
import ru.yandex.auto.vin.decoder.raw.spbcar.SpBusinessCarRawModelManager

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.jdk.CollectionConverters.ListHasAsScala

class SpBusinessCarRawModelManagerTest extends AnyFunSuite {

  val manager = new SpBusinessCarRawModelManager

  test("correct parse") {
    val raw = ResourceUtils.getStringFromResources("/sp-business-car/sp_business_car_service_book.json")

    val parsed = manager.parse(raw, "", "").toOption.get
    val prepared = Await.result(manager.convert(parsed), 1.second)

    val sb = prepared.getServiceBook
    val orders = sb.getOrdersList.asScala

    assert(prepared.getVin === "JTDBZ20E500142530")
    assert(prepared.getStatus === VinInfoHistory.Status.OK)

    assert(sb.getMark === "TOYOTA")
    assert(sb.getModel === "COROLLA")
    assert(sb.getYear === 2006)

    assert(orders.size === 2)

    assert(orders.head.getMileage === 49892)
    assert(orders.head.getOrderDate === 1215454176000L)
    assert(orders.head.getStoCity === "Москва")
    assert(orders.head.getProductsCount === 0)
    assert(orders.head.getServicesCount === 8)

    assert(orders(1).getMileage === 90457)
    assert(orders(1).getOrderDate === 1280477417000L)
    assert(orders(1).getStoCity === "Одинцово")
    assert(orders(1).getProductsCount === 0)
    assert(orders(1).getServicesCount === 1)
  }
}
