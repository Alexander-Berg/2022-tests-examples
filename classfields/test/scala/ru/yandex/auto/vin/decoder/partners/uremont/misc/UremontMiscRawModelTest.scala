package ru.yandex.auto.vin.decoder.partners.uremont.misc

import auto.carfax.common.utils.misc.ResourceUtils
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.VinCode

class UremontMiscRawModelTest extends AnyFunSuite {

  test("success response") {
    val vin = VinCode("SJNFDAJ11U1084830")
    val raw = ResourceUtils.getStringFromResources(s"/uremont/misc/success.json")
    val model = UremontMiscRawModel.apply(vin, 200, raw)

    assert(model.data.uremont.nonEmpty)
    assert(model.data.insurance.exists(_.size == 4))
  }

  test("unexpected response code") {
    val vin = VinCode("SJNFDAJ11U1084830")
    val raw = ResourceUtils.getStringFromResources(s"/uremont/misc/success.json")

    intercept[IllegalArgumentException] {
      UremontMiscRawModel.apply(vin, 404, raw)
    }
  }

  test("error response") {
    val vin = VinCode("SJNFDAJ11U1084830")
    val raw = ResourceUtils.getStringFromResources(s"/uremont/misc/error.json")

    intercept[IllegalArgumentException] {
      UremontMiscRawModel.apply(vin, 200, raw)
    }
  }

}
