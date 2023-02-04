package ru.yandex.vertis.parsing.auto.parsers.parsingsales.cars.youla

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
class ParsingSalesYoulaCarsParserTest extends FunSuite {
  test("tmp") {
    pending
    val in = this.getClass.getResourceAsStream("/youla_urls.txt")
    scala.io.Source
      .fromInputStream(in)
      .getLines
      .foreach(line => {
        val url = line.split("\t")(1).replace("\"", "")
        println(
          url + " " + ParsingSalesYoulaCarsParser.offerId(url) + " " + ParsingSalesYoulaCarsParser
            .parseAddressFromUrl(url)
        )
      })
  }

  test("offerId") {
    val in = this.getClass.getResourceAsStream("/youla_urls.txt")
    scala.io.Source
      .fromInputStream(in)
      .getLines
      .foreach(line => {
        val s = line.split(" ")
        val url = s.head
        assert(ParsingSalesYoulaCarsParser.offerId(url) == s(1))
      })
  }

  test("parseAddressFromUrl") {
    val in = this.getClass.getResourceAsStream("/youla_urls.txt")
    scala.io.Source
      .fromInputStream(in)
      .getLines
      .foreach(line => {
        val s = line.split(" ")
        val url = s.head
        assert(ParsingSalesYoulaCarsParser.parseAddressFromUrl(url).toExpected == s.drop(2).mkString(" "))
      })
  }

  test("shouldProcessUrl") {
    val in = this.getClass.getResourceAsStream("/youla_urls.txt")
    scala.io.Source
      .fromInputStream(in)
      .getLines
      .foreach(line => {
        val s = line.split(" ")
        val url = s.head
        assert(ParsingSalesYoulaCarsParser.shouldProcessUrl(url) == ParsedValue.Expected(true))
      })
    assert(
      ParsingSalesYoulaCarsParser
        .shouldProcessUrl("https://auto.e1.ru/car/new/vaz/21927_kalina_ii_hetchbek/8854504")
        .isUnexpected
    )
    assert(
      ParsingSalesYoulaCarsParser
        .shouldProcessUrl("http://m.auto.29.ru/car/motors/foreign/details/253742.php")
        .isUnexpected
    )
    assert(
      ParsingSalesYoulaCarsParser
        .shouldProcessUrl("http://m.autochel.ru/car/motors/foreign/details/2353628.php")
        .isUnexpected
    )
    assert(ParsingSalesYoulaCarsParser.shouldProcessUrl(TestDataUtils.testAvitoCarsUrl).isUnexpected)
    assert(ParsingSalesYoulaCarsParser.shouldProcessUrl(TestDataUtils.testAvitoTrucksUrl).isUnexpected)
    assert(ParsingSalesYoulaCarsParser.shouldProcessUrl(TestDataUtils.testDromCarsUrl).isUnexpected)
    assert(ParsingSalesYoulaCarsParser.shouldProcessUrl(TestDataUtils.testDromTrucksUrl).isUnexpected)
    assert(ParsingSalesYoulaCarsParser.shouldProcessUrl(TestDataUtils.testAmruCarsUrl).isUnexpected)
  }
}
