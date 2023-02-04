package ru.yandex.auto.vin.decoder.partners.migalki

import auto.carfax.common.utils.misc.ResourceUtils
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.LicensePlate

class MigalkiRawModelTest extends AnyFunSuite {
  private val lp = LicensePlate("а001мр97")

  test("successful response") {
    val raw = ResourceUtils.getStringFromResources("/migalki/successful_response.json")
    val rawModel = MigalkiRawModel.apply(raw, 200, lp)

    val model = rawModel.response

    assert(model.cars.size === 2)
    assert(model.cars.head.date.toString === "2009-06-01")
    assert(
      model.cars.head.originalPhotoUri ===
        "https://s1.migalki.net/upload/1000/2009/06/01/5b41962f7d4ca29af2542d80acbe8ffb.jpg"
    )
  }

  test("empty response") {
    val raw = ResourceUtils.getStringFromResources("/migalki/empty_response.json")
    val rawModel = MigalkiRawModel.apply(raw, 200, lp)

    val model = rawModel.response

    assert(model.cars.size === 0)
  }
}
