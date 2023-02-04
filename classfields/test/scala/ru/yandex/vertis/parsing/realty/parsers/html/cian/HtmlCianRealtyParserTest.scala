package ru.yandex.vertis.parsing.realty.parsers.html.cian

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class HtmlCianRealtyParserTest extends FunSuite {
  test("active page") {
    val in = this.getClass.getResourceAsStream("/cian-realty-offer-ok-1.html")
    val url = "https://spb.cian.ru/rent/flat/199155943/"
    val parser = new HtmlCianRealtyParser(url, in)
    assert(!parser.isDeactivated._1)
  }

  test("inactive page") {
    val in = this.getClass.getResourceAsStream("/cian-realty-offer-deact-1.html")
    val url = "https://rostov.cian.ru/rent/flat/199155944/"
    val parser = new HtmlCianRealtyParser(url, in)
    assert(parser.isDeactivated._1)
  }
}
