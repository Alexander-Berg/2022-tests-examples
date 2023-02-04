package ru.auto.salesman.util

import ru.auto.salesman.test.BaseSpec
import GeoUtils._

class GeoUtilsSpec extends BaseSpec {

  "Geo utils" should {

    "return correct regions without calls deposit" in {
      regionsWithoutCallsDeposit shouldBe Set(
        RegSverdlovsk,
        RegChelyabinsk,
        RegVoronezh,
        RegTula,
        RegYaroslavl
      )
    }
  }
}
