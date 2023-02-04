package ru.yandex.vertis.parsing.auto.parsers.parsingsales.cars.e1

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import play.api.libs.json.Json
import ru.yandex.vertis.parsing.auto.util.TestDataUtils
import ru.yandex.vertis.parsing.parsers.ParsedValue

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class ParsingSalesE1CarsParserTest extends FunSuite {
  test("offerId") {
    val in = this.getClass.getResourceAsStream("/e1_urls.txt")
    scala.io.Source
      .fromInputStream(in)
      .getLines
      .foreach(line => {
        val s = line.split(" ")
        val url = s.head
        assert(ParsingSalesE1CarsParser.offerId(url) == s(1))
      })
  }

  test("section") {
    val in = this.getClass.getResourceAsStream("/e1_urls.txt")
    scala.io.Source
      .fromInputStream(in)
      .getLines
      .foreach(line => {
        val s = line.split(" ")
        val url = s.head
        assert(ParsingSalesE1CarsParser.parseSection(url, Json.obj()).toString == s(2))
      })
  }

  test("shouldProcessUrl") {
    val in = this.getClass.getResourceAsStream("/e1_urls.txt")
    scala.io.Source
      .fromInputStream(in)
      .getLines
      .foreach(line => {
        val s = line.split(" ")
        val url = s.head
        assert(ParsingSalesE1CarsParser.shouldProcessUrl(url) == ParsedValue.Expected(true))
      })
    assert(
      ParsingSalesE1CarsParser
        .shouldProcessUrl("http://m.auto.29.ru/car/motors/foreign/details/253742.php")
        .isUnexpected
    )
    assert(
      ParsingSalesE1CarsParser.shouldProcessUrl("http://m.autochel.ru/car/motors/rus/details/2353103.php").isUnexpected
    )
    assert(
      ParsingSalesE1CarsParser
        .shouldProcessUrl("https://youla.io/novosibirsk/avto-moto/avtomobili/miersiedies-5a27ce25a09cd50d16085b62")
        .isUnexpected
    )
    assert(ParsingSalesE1CarsParser.shouldProcessUrl(TestDataUtils.testAvitoCarsUrl).isUnexpected)
    assert(ParsingSalesE1CarsParser.shouldProcessUrl(TestDataUtils.testAvitoTrucksUrl).isUnexpected)
    assert(ParsingSalesE1CarsParser.shouldProcessUrl(TestDataUtils.testDromCarsUrl).isUnexpected)
    assert(ParsingSalesE1CarsParser.shouldProcessUrl(TestDataUtils.testDromTrucksUrl).isUnexpected)
    assert(ParsingSalesE1CarsParser.shouldProcessUrl(TestDataUtils.testAmruCarsUrl).isUnexpected)
  }
}
