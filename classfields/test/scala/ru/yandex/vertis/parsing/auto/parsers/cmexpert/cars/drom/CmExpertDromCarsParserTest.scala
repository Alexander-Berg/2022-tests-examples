package ru.yandex.vertis.parsing.auto.parsers.cmexpert.cars.drom

import org.joda.time.{DateTime, DateTimeZone}
import org.junit.runner.RunWith
import org.scalatest.{FunSuite, OptionValues}
import org.scalatest.junit.JUnitRunner
import play.api.libs.json.Json
import ru.yandex.vertis.parsing.auto.parsers.scrapinghub.cars.drom.ScrapingHubDromCarsParser

@RunWith(classOf[JUnitRunner])
class CmExpertDromCarsParserTest extends FunSuite with OptionValues {
  test("offerId") {
    assert(CmExpertDromCarsParser.offerId("https://abakan.drom.ru/audi/a4/46102203.html") == "46102203")
    assert(CmExpertDromCarsParser.offerId("https://abakan.drom.ru/bmw/7-series/45284097.html") == "45284097")
  }

  test("sh_last_visited") {
    val x = ScrapingHubDromCarsParser
      .parseParseDate(
        Json.obj(
          "sh_last_visited" -> "2022-03-25T14:30:07"
        )
      )
      .toOption
      .value
    assert(x == DateTime.parse("2022-03-25T14:30:07"))
  }
}
