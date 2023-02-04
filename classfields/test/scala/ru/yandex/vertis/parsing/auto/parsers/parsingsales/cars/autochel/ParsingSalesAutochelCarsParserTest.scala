package ru.yandex.vertis.parsing.auto.parsers.parsingsales.cars.autochel

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
class ParsingSalesAutochelCarsParserTest extends FunSuite {
  test("tmp") {
    pending
    val in = this.getClass.getResourceAsStream("/autochel_urls.txt")
    scala.io.Source
      .fromInputStream(in)
      .getLines
      .foreach(line => {
        val url = line.split("\t")(1).replace("\"", "")
        println(url + " " + ParsingSalesAutochelCarsParser.offerId(url))
      })
  }

  test("offerId") {
    val in = this.getClass.getResourceAsStream("/autochel_urls.txt")
    scala.io.Source
      .fromInputStream(in)
      .getLines
      .foreach(line => {
        val s = line.split(" ")
        val url = s.head
        assert(ParsingSalesAutochelCarsParser.offerId(url) == s(1))
      })
  }

  test("shouldProcessUrl") {
    val in = this.getClass.getResourceAsStream("/autochel_urls.txt")
    scala.io.Source
      .fromInputStream(in)
      .getLines
      .foreach(line => {
        val s = line.split(" ")
        val url = s.head
        assert(ParsingSalesAutochelCarsParser.shouldProcessUrl(url) == ParsedValue.Expected(true))
      })
    assert(
      ParsingSalesAutochelCarsParser
        .shouldProcessUrl("https://auto.e1.ru/car/new/vaz/21927_kalina_ii_hetchbek/8854504")
        .isUnexpected
    )
    assert(
      ParsingSalesAutochelCarsParser
        .shouldProcessUrl("http://m.auto.29.ru/car/motors/foreign/details/253742.php")
        .isUnexpected
    )
    assert(
      ParsingSalesAutochelCarsParser
        .shouldProcessUrl("https://youla.io/novosibirsk/avto-moto/avtomobili/miersiedies-5a27ce25a09cd50d16085b62")
        .isUnexpected
    )
    assert(ParsingSalesAutochelCarsParser.shouldProcessUrl(TestDataUtils.testAvitoCarsUrl).isUnexpected)
    assert(ParsingSalesAutochelCarsParser.shouldProcessUrl(TestDataUtils.testAvitoTrucksUrl).isUnexpected)
    assert(ParsingSalesAutochelCarsParser.shouldProcessUrl(TestDataUtils.testDromCarsUrl).isUnexpected)
    assert(ParsingSalesAutochelCarsParser.shouldProcessUrl(TestDataUtils.testDromTrucksUrl).isUnexpected)
    assert(ParsingSalesAutochelCarsParser.shouldProcessUrl(TestDataUtils.testAmruCarsUrl).isUnexpected)
  }
}
