package ru.yandex.auto.vin.decoder.partners.fitauto

import auto.carfax.common.utils.misc.ResourceUtils
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.VinCode

class FitautoRawModelTest extends AnyFunSuite {

  private val vinCode = VinCode("SJNFDAJ11U1084830")

  test("success") {
    val raw = ResourceUtils.getStringFromResources(s"/fitauto/success.xml")

    val model = FitautoRawModel(raw, 200, vinCode)

    assert(model.data.SuccessValue)
    assert(model.data.ErrorCode.isEmpty)

    val car = model.data.CAR.get.get
    assert(car.MARK.contains("MAZDA"))
    assert(car.MODEL.contains("CX-7"))
    assert(car.YEAR.contains(2008))
    assert(car.ORDERS.get.get.Order.size == 1)
  }

  test("not found") {
    val raw = ResourceUtils.getStringFromResources(s"/fitauto/not_found.xml")

    val model = FitautoRawModel(raw, 200, vinCode)

    assert(!model.data.SuccessValue)
    assert(model.data.ErrorCode.get.get == 404)
    assert(model.data.CAR.isEmpty)
  }
}
