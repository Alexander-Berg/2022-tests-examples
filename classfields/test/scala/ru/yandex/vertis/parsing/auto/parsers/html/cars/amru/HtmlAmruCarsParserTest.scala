package ru.yandex.vertis.parsing.auto.parsers.html.cars.amru

import org.scalatest.FunSuite

/**
  * TODO
  *
  * @author aborunov
  */
class HtmlAmruCarsParserTest extends FunSuite {
  test("active page") {
    val url = "https://auto.youla.ru/advert/used/vaz_lada/2110/prv--a7134d426fbdb2/"
    val in = this.getClass.getResourceAsStream("/amru_active.html")
    val parser = new HtmlAmruCarsParser(url, in)
    assert(!parser.isDeactivated._1)
  }

  test("inactive page") {
    val url = "https://auto.youla.ru/advert/used/volkswagen/polo/prv--6fffabc636e50141/"
    val in = this.getClass.getResourceAsStream("/amru_inactive.html")
    val parser = new HtmlAmruCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("listing page") {
    val url = "https://auto.youla.ru/advert/new/mercedes_benz/gls_class/avs-avtospot-sankt-peterburg--31c92e3af7f74cc4"
    val in = this.getClass.getResourceAsStream("/amru_listing.html")
    val parser = new HtmlAmruCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }
}
