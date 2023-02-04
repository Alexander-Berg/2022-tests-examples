package ru.yandex.vos2.autoru.model.extdata

import java.io.StringReader

import org.scalatest.funsuite.AnyFunSuite
import org.testcontainers.shaded.org.apache.commons.io.input.ReaderInputStream
import ru.yandex.vos2.autoru.model.extdata.DefaultBookingAllowedDictionary.ByRegion
import ru.auto.api.ApiOfferModel.{Category, Section}
import ru.yandex.vos2.model.UserRefAutoruClient

class DefaultBookingAllowedDictionarySpec extends AnyFunSuite {

  private val data: String =
    """[{
      |  "name": "default_booking_allowed",
      |  "fullName": "/auto_ru/common/default_booking_allowed",
      |  "flushDate": "2020-01-01T00:00:00.000Z",
      |  "version": 1,
      |  "mime": "application/json; charset=utf-8",
      |  "content": {
      |	    "by_regions": [
      |	    	{
      |			    "category": "CARS",
      |			    "section": "NEW",
      |			    "geobase_ids": [1,2]
      |	    	},
      |	    	{
      |			    "category": "CARS",
      |		    	"section": "USED",
      |		    	"geobase_ids": [3,4]
      |	    	}
      |    ],
      |    "excluded_dealer_ids": [5,6]
      |  }
      |}]""".stripMargin

  test("parse json from bunker") {
    val expected = DefaultBookingAllowedDictionary(
      byRegions = Set(
        ByRegion(Category.CARS, Section.NEW, geobaseIds = Set(1, 2)),
        ByRegion(Category.CARS, Section.USED, geobaseIds = Set(3, 4))
      ),
      excludedDealerIds = Set(UserRefAutoruClient(5), UserRefAutoruClient(6))
    )
    val actual = DefaultBookingAllowedDictionary.parse(new ReaderInputStream(new StringReader(data), "utf-8"))
    assert(actual == expected)
  }
}
