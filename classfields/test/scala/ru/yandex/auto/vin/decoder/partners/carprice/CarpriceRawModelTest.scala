package ru.yandex.auto.vin.decoder.partners.carprice

import auto.carfax.common.utils.misc.ResourceUtils
import auto.carfax.common.utils.tracing.Traced
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.VinCode

class CarpriceRawModelTest extends AnyFunSuite with BeforeAndAfterAll {

  implicit val t = Traced.empty

  test("success response") {
    val vin = VinCode("SJNFDAJ11U1084830")
    val raw = ResourceUtils.getStringFromResources(s"/carprice/success.json")
    val model = CarpriceRawModel.apply(200, raw, vin)

    assert(model.groupId == "2017-09-01")
    assert(model.model.nonEmpty)
    assert(model.model.get.mileage == 223000)
  }

  test("not found") {
    val vin = VinCode("SJNFDAJ11U1084830")
    val raw = ResourceUtils.getStringFromResources(s"/carprice/not_found.json")
    val model = CarpriceRawModel.apply(400, raw, vin)

    assert(model.model.isEmpty)
    assert(model.groupId == "")
  }

}
