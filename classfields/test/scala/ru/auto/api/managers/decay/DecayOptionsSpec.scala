package ru.auto.api.managers.decay

import ru.auto.api.BaseSpec

/**
  * Created by mcsim-gr on 27.09.17.
  */
class DecayOptionsSpec extends BaseSpec {

  "DecayOptions" should {
    "form a composition" in {
      val result = DecayOptions.HotData + DecayOptions.SensitiveData + DecayOptions.PriceHistory
      result.hotData shouldBe true
      result.sensitiveData shouldBe true
      result.priceHistory shouldBe true
    }
  }
}
