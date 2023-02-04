package ru.auto.api.model

import org.scalatest.funsuite.AnyFunSuite
import ru.auto.api.CommonModel.PriceInfo
import ru.auto.api.model.CommonModelUtils._

class CommonModelUtilsTest extends AnyFunSuite {
  test("RichPriceInfoOrBuilder double fields") {
    val price = 1234567.03
    val expected = 1234567.0d
    val builder = PriceInfo.newBuilder
      .setDprice(price)
      .setRurDprice(price)
      .setUsdDprice(price)

    assert(builder.selectPrice == expected)
    assert(builder.selectRurPrice == expected)
    assert(builder.selectUsdPrice == expected)
    assert(builder.selectEurPrice == 0)

    builder.complement

    assert(builder.getPrice == expected.toFloat)
    assert(builder.getRurPrice == expected.toFloat)
    assert(builder.getUsdPrice == expected.toFloat)
    assert(builder.getEurPrice == 0)
  }

  test("RichPriceInfoOrBuilder float fields") {
    val price = 123456789.02f
    val expected = 123456789.02f
    val builder = PriceInfo.newBuilder
      .setPrice(price)
      .setRurPrice(price)
      .setUsdPrice(price)

    assert(builder.selectPrice == expected)
    assert(builder.selectRurPrice == expected)
    assert(builder.selectUsdPrice == expected)
    assert(builder.selectEurPrice == 0)

    builder.complement

    assert(builder.getDprice == expected)
    assert(builder.getRurDprice == expected)
    assert(builder.getUsdDprice == expected)
    assert(builder.getEurDprice == 0)
  }
}
