package ru.yandex.vertis.parsing.auto.parsers.parsingsales.cars.auto29

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.parsing.auto.util.TestDataUtils
import ru.yandex.vertis.parsing.parsers.ParsedValue

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class ParsingSalesAuto29CarsParserTest extends FunSuite {
  test("tmp") {
    pending
    val in = this.getClass.getResourceAsStream("/auto29_urls.txt")
    scala.io.Source
      .fromInputStream(in)
      .getLines
      .foreach(line => {
        val url = line.split("\t")(1).replace("\"", "")
        println(url + " " + ParsingSalesAuto29CarsParser.offerId(url))
      })
  }

  test("offerId") {
    val in = this.getClass.getResourceAsStream("/auto29_urls.txt")
    scala.io.Source
      .fromInputStream(in)
      .getLines
      .foreach(line => {
        val s = line.split(" ")
        val url = s.head
        assert(ParsingSalesAuto29CarsParser.offerId(url) == s(1))
      })
  }

  test("shouldProcessUrl") {
    val in = this.getClass.getResourceAsStream("/auto29_urls.txt")
    scala.io.Source
      .fromInputStream(in)
      .getLines
      .foreach(line => {
        val s = line.split(" ")
        val url = s.head
        assert(ParsingSalesAuto29CarsParser.shouldProcessUrl(url) == ParsedValue.Expected(true))
      })
    assert(
      ParsingSalesAuto29CarsParser
        .shouldProcessUrl("https://auto.e1.ru/car/new/vaz/21927_kalina_ii_hetchbek/8854504")
        .isUnexpected
    )
    assert(
      ParsingSalesAuto29CarsParser
        .shouldProcessUrl("http://m.autochel.ru/car/motors/rus/details/2353103.php")
        .isUnexpected
    )
    assert(
      ParsingSalesAuto29CarsParser
        .shouldProcessUrl("https://youla.io/novosibirsk/avto-moto/avtomobili/miersiedies-5a27ce25a09cd50d16085b62")
        .isUnexpected
    )
    assert(ParsingSalesAuto29CarsParser.shouldProcessUrl(TestDataUtils.testAvitoCarsUrl).isUnexpected)
    assert(ParsingSalesAuto29CarsParser.shouldProcessUrl(TestDataUtils.testAvitoTrucksUrl).isUnexpected)
    assert(ParsingSalesAuto29CarsParser.shouldProcessUrl(TestDataUtils.testDromCarsUrl).isUnexpected)
    assert(ParsingSalesAuto29CarsParser.shouldProcessUrl(TestDataUtils.testDromTrucksUrl).isUnexpected)
    assert(ParsingSalesAuto29CarsParser.shouldProcessUrl(TestDataUtils.testAmruCarsUrl).isUnexpected)
  }
}
