package ru.yandex.auto.vin.decoder.partners.avtonomer

import auto.carfax.common.utils.misc.ResourceUtils
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.LicensePlate
import auto.carfax.common.utils.misc.DateTimeUtils._

class AvtonomerRawModelTest extends AnyFunSuite {

  private val lp = LicensePlate("a001aa98")

  test("successful response") {
    val raw = ResourceUtils.getStringFromResources("/avtonomer/successful_response.json")
    val rawModel = AvtonomerRawModel.apply(raw, 200, lp)

    val model = rawModel.response

    assert(model.error === 0)
    assert(model.cars.size === 2)
    assert(model.cars.head.make === Some("Mercedes-Benz"))
    assert(model.cars.head.model === Some("S-Klasse"))
    assert(model.cars.head.date.getMillis === 1350072000000L)
  }

  test("empty response") {
    val raw = ResourceUtils.getStringFromResources("/avtonomer/empty_response.json")
    val rawModel = AvtonomerRawModel.apply(raw, 200, lp)

    val model = rawModel.response

    assert(model.error === 1)
    assert(model.cars.size === 0)
  }

}
