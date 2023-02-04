package ru.yandex.vertis.parsing.auto.parsers.html

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.parsing.auto.parsers.html.cars.CommonAutoHtmlParser
import ru.yandex.vertis.parsing.common.Site

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class CommonHtmlParserTest extends FunSuite {
  test("forSiteAndCategory: avito") {
    val url = "https://www.avito.ru/kazan/avtomobili/lada_kalina_2011_1394425125"
    val in = this.getClass.getResourceAsStream("/avito_inactive.html")
    val parser = CommonAutoHtmlParser.forSiteAndCategory(Site.Avito, Category.CARS, url, in)
    assert(parser.isDeactivated._1)
  }

  test("forSiteAndCategory: drom") {
    val url = "https://vladivostok.drom.ru/hummer/h2/28386969.html"
    val in = this.getClass.getResourceAsStream("/drom_inactive.html")
    val parser = CommonAutoHtmlParser.forSiteAndCategory(Site.Drom, Category.CARS, url, in)
    assert(parser.isDeactivated._1)
  }

  test("forSiteAndCategory: am.ru") {
    val url = "https://auto.youla.ru/advert/used/vaz_lada/2110/prv--1931a290b94baae0/"
    val in = this.getClass.getResourceAsStream("/amru_inactive.html")
    val parser = CommonAutoHtmlParser.forSiteAndCategory(Site.Amru, Category.CARS, url, in)
    assert(parser.isDeactivated._1)
  }

  test("forSiteAndCategory: unexpected") {
    val url = "https://vladivostok.drom.ru/hummer/h2/28386969.html"
    val in = this.getClass.getResourceAsStream("/drom_inactive.html")
    intercept[RuntimeException] {
      CommonAutoHtmlParser.forSiteAndCategory(Site.Drom, Category.TRUCKS, url, in)
    }
  }
}
