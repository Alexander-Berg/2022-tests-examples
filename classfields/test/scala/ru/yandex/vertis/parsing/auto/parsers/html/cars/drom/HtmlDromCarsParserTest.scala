package ru.yandex.vertis.parsing.auto.parsers.html.cars.drom

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class HtmlDromCarsParserTest extends FunSuite {
  test("active page") {
    val url = "https://novokuznetsk.drom.ru/lada/2105/32692022.html"
    val in = this.getClass.getResourceAsStream("/drom_active.html")
    val parser = new HtmlDromCarsParser(url, in)
    assert(!parser.isDeactivated._1)
  }

  test("active page 2") {
    val url = "https://novokuznetsk.drom.ru/lada/2105/32692022.html"
    val in = this.getClass.getResourceAsStream("/drom_active2.html")
    val parser = new HtmlDromCarsParser(url, in)
    assert(!parser.isDeactivated._1)
  }

  test("active page 3") {
    val url = "https://vladivostok.drom.ru/subaru/outback/46619197.html"
    val in = this.getClass.getResourceAsStream("/drom_active3.html")
    val parser = new HtmlDromCarsParser(url, in)
    assert(!parser.isDeactivated._1)
  }

  test("inactive page") {
    val url = "https://vladivostok.drom.ru/hummer/h2/28386969.html"
    val in = this.getClass.getResourceAsStream("/drom_inactive.html")
    val parser = new HtmlDromCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("inactive page 2") {
    val url = "https://vladivostok.drom.ru/hummer/h2/28386969.html"
    val in = this.getClass.getResourceAsStream("/drom_inactive2.html")
    val parser = new HtmlDromCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("inactive page 3") {
    val url = "https://vladivostok.drom.ru/hummer/h2/28386969.html"
    val in = this.getClass.getResourceAsStream("/drom_inactive3.html")
    val parser = new HtmlDromCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("inactive page 4") {
    val url = "https://novokuznetsk.drom.ru/lada/2105/32692022.html"
    val in = this.getClass.getResourceAsStream("/drom_inactive4.html")
    val parser = new HtmlDromCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("inactive page 5") {
    val url = "https://novokuznetsk.drom.ru/lada/2105/32692022.html"
    val in = this.getClass.getResourceAsStream("/drom_inactive5.html")
    val parser = new HtmlDromCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("inactive page 6") {
    val url = "https://novokuznetsk.drom.ru/lada/2105/32692022.html"
    val in = this.getClass.getResourceAsStream("/drom_inactive6.html")
    val parser = new HtmlDromCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("inactive page 7") {
    val url = "https://novokuznetsk.drom.ru/lada/2105/32692022.html"
    val in = this.getClass.getResourceAsStream("/drom_inactive7.html")
    val parser = new HtmlDromCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("inactive page 8") {
    val url = "https://novokuznetsk.drom.ru/lada/2105/32692022.html"
    val in = this.getClass.getResourceAsStream("/drom_inactive8.html")
    val parser = new HtmlDromCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("inactive page 9") {
    val url = "https://novokuznetsk.drom.ru/lada/2105/32692022.html"
    val in = this.getClass.getResourceAsStream("/drom_inactive9.html")
    val parser = new HtmlDromCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("inactive page 10") {
    val url = "https://novokuznetsk.drom.ru/lada/2105/32692022.html"
    val in = this.getClass.getResourceAsStream("/drom_inactive10.html")
    val parser = new HtmlDromCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("inactive page 11") {
    val url = "https://novokuznetsk.drom.ru/lada/2105/32692022.html"
    val in = this.getClass.getResourceAsStream("/drom_inactive11.html")
    val parser = new HtmlDromCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("inactive page 12") {
    val url = "https://novokuznetsk.drom.ru/lada/2105/32692022.html"
    val in = this.getClass.getResourceAsStream("/drom_inactive12.html")
    val parser = new HtmlDromCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("inactive page 13") {
    val url = "https://novokuznetsk.drom.ru/lada/2105/32692022.html"
    val in = this.getClass.getResourceAsStream("/drom_inactive13.html")
    val parser = new HtmlDromCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("inactive page 14") {
    val url = "https://novokuznetsk.drom.ru/lada/2105/32692022.html"
    val in = this.getClass.getResourceAsStream("/drom_inactive14.html")
    val parser = new HtmlDromCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("inactive page 15") {
    val url = "https://novokuznetsk.drom.ru/lada/2105/32692022.html"
    val in = this.getClass.getResourceAsStream("/drom_inactive15.html")
    val parser = new HtmlDromCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("premoderation page") {
    val url = "https://blagoveshchensk.drom.ru/nissan/patrol/19993083.html"
    val in = this.getClass.getResourceAsStream("/drom_premoderation.html")
    val parser = new HtmlDromCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("moderation deleted page") {
    val url = "https://blagoveshchensk.drom.ru/nissan/patrol/19993083.html"
    val in = this.getClass.getResourceAsStream("/drom_moderation_deleted.html")
    val parser = new HtmlDromCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }

  test("moderation deleted page 2") {
    val url = "https://blagoveshchensk.drom.ru/nissan/patrol/19993083.html"
    val in = this.getClass.getResourceAsStream("/drom_moderation_deleted2.html")
    val parser = new HtmlDromCarsParser(url, in)
    assert(parser.isDeactivated._1)
  }
}
