package ru.yandex.realty.rent.model

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class RentFlatTrackIdTest extends FunSuite {
  test("toString") {
    assert(
      RentFlatTrackId("6f76d1c260f14b1094fd3be652aa356a", 1).toString == "rent-flat:6f76d1c260f14b1094fd3be652aa356a:1"
    )
  }

  test("getFlatId") {
    assert(
      RentFlatTrackId
        .getFlatId("rent-flat:6f76d1c260f14b1094fd3be652aa356a:1")
        .get == "6f76d1c260f14b1094fd3be652aa356a"
    )
  }
}
