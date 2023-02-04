package ru.yandex.vertis.parsing.auto.parsers.av100.cars.amru

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

/**
  * TODO
  *
  * @author aborunov
  */
//scalastyle:off line.size.limit
@RunWith(classOf[JUnitRunner])
class Av100AmruCarsParserTest extends FunSuite {
  test("remoteId") {
    val url = "https://auto.youla.ru/advert/used/kia/rio/avs-btsr-motors-na-komsomolskoy--2c089a0374660fb5/"
    assert(Av100AmruCarsParser.remoteId(url) == "am.ru|cars|2c089a0374660fb5")
  }

  test("offerId") {
    assert(
      Av100AmruCarsParser.offerId("http://am.ru/vl/used/toyota/chaser/prv--10aad272a6daed4a/") == "10aad272a6daed4a"
    )
    assert(
      Av100AmruCarsParser
        .offerId("http://am.ru/msk/used/mercedes_benz/glk_class/prv--18841e75e85ef259/") == "18841e75e85ef259"
    )
    assert(
      Av100AmruCarsParser
        .offerId("https://auto.youla.ru/advert/used/vaz_lada/2112/prv--139fdad9f4a4295e/") == "139fdad9f4a4295e"
    )
  }

  test("parseAddressFromUrl") {
    assert(
      Av100AmruCarsParser.parseAddressFromUrl("http://am.ru/vl/used/toyota/chaser/prv--10aad272a6daed4a/").isNonParsed
    )
    assert(
      Av100AmruCarsParser
        .parseAddressFromUrl("http://am.ru/msk/used/mercedes_benz/glk_class/prv--18841e75e85ef259/")
        .isNonParsed
    )
    assert(
      Av100AmruCarsParser.parseAddressFromUrl("http://am.ru/tomsk/used/kia/ceed/prv--656f39efd3c1b7f7/").isNonParsed
    )
    assert(
      Av100AmruCarsParser
        .parseAddressFromUrl("http://am.ru/advert/used/vaz_lada/2114/prv--75ef18bfe3fff668/")
        .isNonParsed
    )
    assert(
      Av100AmruCarsParser
        .parseAddressFromUrl("https://auto.youla.ru/advert/used/vaz_lada/2112/prv--139fdad9f4a4295e/")
        .isNonParsed
    )
  }
}
