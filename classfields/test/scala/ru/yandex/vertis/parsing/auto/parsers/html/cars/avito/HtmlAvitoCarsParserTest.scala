package ru.yandex.vertis.parsing.auto.parsers.html.cars.avito

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.parsing.parsers.html.{FirewallException, HtmlParseRetriableException}

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class HtmlAvitoCarsParserTest extends FunSuite {
  test("active page") {
    val in = this.getClass.getResourceAsStream("/avito_active.html")
    val url = "https://www.avito.ru/sankt-peterburg/avtomobili/lada_granta_2012_1250430708"
    val parser = new HtmlAvitoCarsParser(url, in)
    assert(!parser.isDeactivated._1)
  }

  test("active page2") {
    val in = this.getClass.getResourceAsStream("/avito_active2.html")
    val url = "https://www.avito.ru/sankt-peterburg/avtomobili/lada_granta_2012_1250430708"
    val parser = new HtmlAvitoCarsParser(url, in)
    assert(!parser.isDeactivated._1)
  }

  test("active page3") {
    val in = this.getClass.getResourceAsStream("/avito_active3.html")
    val url = "https://www.avito.ru/elets/avtomobili/volkswagen_transporter_1993_2211090844"
    val parser = new HtmlAvitoCarsParser(url, in)
    withClue(parser.getDebugData) {
      assert(!parser.isDeactivated._1)
    }
  }

  test("active page4") {
    val in = this.getClass.getResourceAsStream("/avito_active4.html")
    val url = "https://www.avito.ru/balashiha/avtomobili/skoda_octavia_2012_2239119153"
    val parser = new HtmlAvitoCarsParser(url, in)
    withClue(parser.getDebugData) {
      assert(!parser.isDeactivated._1)
    }
  }

  test("active page5") {
    val in = this.getClass.getResourceAsStream("/avito_active5.html")
    val url = "https://www.avito.ru/balashiha/avtomobili/skoda_octavia_2012_2239119153"
    val parser = new HtmlAvitoCarsParser(url, in)
    withClue(parser.getDebugData) {
      assert(!parser.isDeactivated._1)
    }
  }

  test("active page: no phone") {
    val in = this.getClass.getResourceAsStream("/avito_active_nophone.html")
    val url = "https://www.avito.ru/sankt-peterburg/avtomobili/lada_granta_2012_1250430708"
    val parser = new HtmlAvitoCarsParser(url, in)
    assert(!parser.isDeactivated._1)
  }

  test("beaten") {
    val in = this.getClass.getResourceAsStream("/avito_beaten.html")
    val url = "https://www.avito.ru/klintsy/zapchasti_i_aksessuary/audi_80_1988_938353831"
    val parser = new HtmlAvitoCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("beaten 2") {
    val in = this.getClass.getResourceAsStream("/avito_beaten2.html")
    val url = "https://www.avito.ru/kislovodsk/zapchasti_i_aksessuary/ford_sierra_2.0mt_1984_bityy_5000km_1047252323"
    val parser = new HtmlAvitoCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("inactive page") {
    val in = this.getClass.getResourceAsStream("/avito_inactive.html")
    val url = "https://www.avito.ru/kazan/avtomobili/lada_kalina_2011_1394425125"
    val parser = new HtmlAvitoCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("inactive page 2") {
    val in = this.getClass.getResourceAsStream("/avito_inactive2.html")
    val url = "https://www.avito.ru/staryy_oskol/avtomobili/vaz_2107_2010_1315673677"
    val parser = new HtmlAvitoCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("inactive page 3") {
    val in = this.getClass.getResourceAsStream("/avito_inactive3.html")
    val url = "https://www.avito.ru/staryy_oskol/avtomobili/vaz_2107_2010_1315673677"
    val parser = new HtmlAvitoCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("inactive page 4") {
    val in = this.getClass.getResourceAsStream("/avito_inactive4.html")
    val url = "https://www.avito.ru/staryy_oskol/avtomobili/vaz_2107_2010_1315673677"
    val parser = new HtmlAvitoCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("listing page") {
    val in = this.getClass.getResourceAsStream("/avito_listing.html")
    val url = "https://www.avito.ru/astrahan/avtomobili/audi_100_1980_1320954204"
    val parser = new HtmlAvitoCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("listing page 2") {
    val in = this.getClass.getResourceAsStream("/avito_listing2.html")
    val url = "https://www.avito.ru/moskva/avtomobili/skoda_octavia_2012_1849591268"
    val parser = new HtmlAvitoCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("listing page 3") {
    val in = this.getClass.getResourceAsStream("/avito_listing3.html")
    val url = "https://www.avito.ru/murmansk/avtomobili/ssangyong_stavic_2014_1837331821"
    val parser = new HtmlAvitoCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("services listing page") {
    val in = this.getClass.getResourceAsStream("/avito_inactive_services_listing.html")
    val url = "https://www.avito.ru/astrahan/avtomobili/audi_100_1980_1320954204"
    val parser = new HtmlAvitoCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("moderation page") {
    val in = this.getClass.getResourceAsStream("/avito_moderation.html")
    val url = "https://www.avito.ru/nizhniy_novgorod/avtomobili/vaz_2107_2008_1248505175"
    val parser = new HtmlAvitoCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("moderation page 2") {
    val in = this.getClass.getResourceAsStream("/avito_moderation2.html")
    val url = "https://www.avito.ru/nizhniy_novgorod/avtomobili/vaz_2107_2008_1248505175"
    val parser = new HtmlAvitoCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("moderation page 3") {
    val in = this.getClass.getResourceAsStream("/avito_moderation3.html")
    val url = "https://www.avito.ru/nizhniy_novgorod/avtomobili/vaz_2107_2008_1248505175"
    val parser = new HtmlAvitoCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("noncars") {
    val in = this.getClass.getResourceAsStream("/avito_noncars.html")
    val url = "https://www.avito.ru/nizhniy_novgorod/avtomobili/vaz_2107_2008_1248505175"
    val parser = new HtmlAvitoCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("500") {
    val in = this.getClass.getResourceAsStream("/avito_500.html")
    val url = "https://www.avito.ru/nizhniy_novgorod/avtomobili/vaz_2107_2008_1248505175"
    val parser = new HtmlAvitoCarsParser(url, in)
    intercept[HtmlParseRetriableException] {
      parser.isDeactivated._1
    }
  }

  test("firewall") {
    val in = this.getClass.getResourceAsStream("/avito_firewall_container.html")
    val url = "https://www.avito.ru/nizhniy_novgorod/avtomobili/vaz_2107_2008_1248505175"
    val parser = new HtmlAvitoCarsParser(url, in)
    val exception = intercept[FirewallException] {
      parser.isDeactivated._1
    }
    val debugData = exception.getLocalizedMessage.split("\n")
    val hasDivFirewallContainer = debugData.dropWhile(_ != "div.firewall-container").drop(1).head
    assert(hasDivFirewallContainer == "true")
    val h2FirewallTitle = debugData.dropWhile(_ != "div.firewall-container > h2.firewall-title").drop(1).head
    assert(h2FirewallTitle.contains("Доступ с Вашего IP временно ограничен"))
  }
}
