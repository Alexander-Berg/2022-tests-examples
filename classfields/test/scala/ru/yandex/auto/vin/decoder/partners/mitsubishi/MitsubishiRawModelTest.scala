package ru.yandex.auto.vin.decoder.partners.mitsubishi

import auto.carfax.common.utils.misc.ResourceUtils
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.VinCode
import ru.yandex.auto.vin.decoder.partners.mitsubishi.model.MitsubishiRawModel

class MitsubishiRawModelTest extends AnyFunSuite {

  private val vinCode = VinCode("JMBSRCS3A5U010881")

  test("success") {
    val raw = ResourceUtils.getStringFromResources(s"/mitsubishi/success.json")
    val model = MitsubishiRawModel(s"    ﻿$raw    ", 200, vinCode)

    assert(model.data.error == 0)
    assert(model.data.errorDescription.isEmpty)
    assert(model.data.vin == vinCode.toString)
    assert(model.data.vin == vinCode.toString)
    assert(model.data.data.model == "Lancer Classic / Cedia (00 - )")
    assert(model.data.data.brand == "mitsubishi")

    val orders = model.data.data.orders

    assert(orders.size == 2)
    assert(orders.head.mileage == 187300)
    assert(orders(1).mileage == 187313)
    assert(orders.head.`type` == "ТР")
    assert(orders(1).`type` == "Г")
    assert(orders.head.date == "2019-06-17T00:00:00")
    assert(orders.head.city == "Санкт-Петербург")
  }

  test("orders not found") {
    val raw = ResourceUtils.getStringFromResources(s"/mitsubishi/not_found.json")
    val model = MitsubishiRawModel(raw, 200, vinCode)

    assert(model.data.data.orders.isEmpty)
  }

  test("response 200 but error") {
    val raw = ResourceUtils.getStringFromResources(s"/mitsubishi/error.json")

    intercept[RuntimeException] {
      MitsubishiRawModel(raw, 200, vinCode)
    }
  }
}
