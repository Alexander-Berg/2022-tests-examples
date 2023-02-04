package ru.yandex.auto.vin.decoder.partners.rusauto

import auto.carfax.common.utils.misc.ResourceUtils
import org.scalatest.funsuite.AnyFunSuite
import play.api.libs.json.Json
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.rusauto.model.{RusAutoRawModel, RusAutoResponse}

class RusAutoRawModelTest extends AnyFunSuite {

  val prefix = "/rusauto"
  val vin = VinCode.apply("XTT315196C0516055")

  test("parse success response") {
    val raw = ResourceUtils.getStringFromResources(s"$prefix/found.json")
    val code = 200

    val parsed = RusAutoRawModel.apply(raw, code, vin, Json.parse(raw).asOpt[RusAutoResponse])
    assert(parsed.identifier.toString === "XTT315196C0516055")
    assert(parsed.rawStatus === "200")
    assert(parsed.response.head.status)

    val carData = parsed.response.head.carData.get
    val order = carData.orders.head

    assert(carData.mark === "УАЗ")
    assert(carData.model === "УАЗ-315196")
    assert(carData.getYear === Some(2012))
    assert(carData.orders.length === 1)

    assert(order.id === "ИП00036983")
    assert(order.dateTimestamp === 1384300800000L)
    assert(order.mileage === Some(337))
    assert(order.stoName === "Автоцентр РУС-АВТО")
    assert(order.stoCity === "Великий Новгород")
    assert(order.description.nonEmpty)
    assert(order.recommendations.size === 1)
    assert(order.services.size === 3)
    assert(order.products.size === 19)
  }

  test("parse not found response") {
    val vin = VinCode.apply("XTT31519570544919")
    val raw = ResourceUtils.getStringFromResources(s"$prefix/not_found.json")
    val code = 200

    val parsed = RusAutoRawModel.apply(raw, code, vin, Json.parse(raw).asOpt[RusAutoResponse])
    assert(parsed.identifier.toString === "XTT31519570544919")
    assert(parsed.rawStatus === "200")
    assert(parsed.response.head.status === false)
    assert(parsed.response.head.carData === None)
  }

}
