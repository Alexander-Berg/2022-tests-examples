package ru.yandex.auto.vin.decoder.partners.uremont.auction

import auto.carfax.common.utils.misc.ResourceUtils
import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.auto.vin.decoder.model.VinCode

class UremontAuctionRawModelTest extends AnyFunSuite {

  test("success response") {
    val vin = VinCode("SJNFDAJ11U1084830")
    val raw = ResourceUtils.getStringFromResources(s"/uremont/auction/success.json")
    val model = UremontAuctionRawModel.apply(vin, raw, 200)

    assert(model.data.migtorg.nonEmpty)
    assert(model.data.uremont.isEmpty)
    assert(model.data.insurance.isEmpty)
  }

  test("unexpected response code") {
    val vin = VinCode("SJNFDAJ11U1084830")
    val raw = ResourceUtils.getStringFromResources(s"/uremont/auction/success.json")

    intercept[IllegalArgumentException] {
      UremontAuctionRawModel.apply(vin, raw, 404)
    }
  }

  test("error response") {
    val vin = VinCode("SJNFDAJ11U1084830")
    val raw = ResourceUtils.getStringFromResources(s"/uremont/auction/error.json")

    intercept[IllegalArgumentException] {
      UremontAuctionRawModel.apply(vin, raw, 200)
    }
  }
}
