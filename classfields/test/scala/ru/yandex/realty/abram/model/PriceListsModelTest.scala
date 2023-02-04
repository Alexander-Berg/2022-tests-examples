package ru.yandex.realty.abram.model

import org.joda.time.DateTime
import ru.yandex.realty.SpecBase

class PriceListsModelTest extends SpecBase {
  "PriceListsModel" should {
    "return differnt price list for dates of call" in {

      val summerCall = DateTime.parse("2021-07-01T16:20:00")
      val autumnCall = DateTime.parse("2021-09-07T22:00:00")
      PriceListsModel.getPriceStartDateByCallTime(autumnCall) shouldBe
        PriceStartDate.CapitalsUpdate

      PriceListsModel.getPriceStartDateByCallTime(summerCall) shouldBe
        PriceStartDate.KrasnodarUpdate
    }
  }

}
