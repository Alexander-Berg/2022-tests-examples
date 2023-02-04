package ru.yandex.vertis.parsing.realty.parsers.html.avito

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class HtmlAvitoRealtyParserTest extends FunSuite {
  test("active page") {
    val in = this.getClass.getResourceAsStream("/avito_realty_offer.html")
    val url = "https://www.avito.ru/ivanovo/kvartiry/2-k_kvartira_52_m_912_et._1788886430"
    val parser = new HtmlAvitoRealtyParser(url, in)
    assert(!parser.isDeactivated._1)
  }

  test("active page 2") {
    val in = this.getClass.getResourceAsStream("/avito_realty_offer-2.html")
    val url = "https://www.avito.ru/moskva/kvartiry/3-k_kvartira_162_m_37_et._1790444632"
    val parser = new HtmlAvitoRealtyParser(url, in)
    assert(!parser.isDeactivated._1)
  }

  test("active page 3") {
    val in = this.getClass.getResourceAsStream("/avito_realty_offer-3.html")
    val url = "https://www.avito.ru/ulyanovsk/kvartiry/1-k_kvartira_41.1_m_14_et._1960014888"
    val parser = new HtmlAvitoRealtyParser(url, in)
    assert(!parser.isDeactivated._1)
  }

  test("inactive page") {
    val in = this.getClass.getResourceAsStream("/avito_realty_offer_deact.html")
    val url = "https://www.avito.ru/novyy_oskol/kvartiry/3-k_kvartira_60_m_45_et._1268052339"
    val parser = new HtmlAvitoRealtyParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("listing page") {
    val in = this.getClass.getResourceAsStream("/avito_realty_listing.html")
    val url = "https://www.avito.ru/penza/kvartiry/1-k_kvartira_30_m_25_et._1921914891"
    val parser = new HtmlAvitoRealtyParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("rejected by mod") {
    val in = this.getClass.getResourceAsStream("/avito_realty_rejected_by_mod.html")
    val url =
      "https://www.avito.ru/volgogradskaya_oblast_volzhskiy/doma_dachi_kottedzhi/dom_60_m_na_uchastke_20_sot._1452602654"
    val parser = new HtmlAvitoRealtyParser(url, in)
    assert(parser.isDeactivated._1)
    assert(parser.isDeactivated._2 == "moderation_page")
  }
}
