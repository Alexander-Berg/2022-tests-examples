package ru.yandex.vertis.parsing.auto.parsers.scrapinghub.cars.avito

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.{FunSuite, OptionValues}
import org.scalatest.junit.JUnitRunner
import play.api.libs.json.{JsObject, Json}
import ru.yandex.vertis.parsing.auto.parsers.Views
import ru.yandex.vertis.parsing.parsers.ParsedValue
import ru.yandex.vertis.parsing.parsers.ParsedValue.{Expected, Unexpected}

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class ScrapingHubAvitoCarsParserTest extends FunSuite with OptionValues {
  test("mileage") {
    assert(
      ScrapingHubAvitoCarsParser.parseMileage(
        Json.obj(
          "car_mileage" -> "142 823 км"
        )
      ) == ParsedValue.Expected(142823)
    )
  }

  test("sh_last_visited") {
    assert(
      ScrapingHubAvitoCarsParser
        .parseParseDate(
          Json.obj(
            "sh_last_visited" -> "2021-12-22 18:15:47+00:00"
          )
        )
        .isUnexpected
    )
  }

  test("listing_date") {
    val x = ScrapingHubAvitoCarsParser.parseDisplayedPublishDate(
      Json.obj(
        "listing_date" -> "07-03-2019"
      )
    )
    assert(x.isExpected)
    assert(x.toExpected == new DateTime(2019, 3, 7, 0, 0, 0, 0))
  }

  test("incorrect listing_date") {
    val x = ScrapingHubAvitoCarsParser.parseDisplayedPublishDate(
      Json.obj(
        "listing_date" -> "07-03-19"
      )
    )
    assert(x.isExpected)
    assert(x.toExpected == new DateTime(2019, 3, 7, 0, 0, 0, 0))
  }

  test("incorrect listing_date 2") {
    val x = ScrapingHubAvitoCarsParser.parseDisplayedPublishDate(
      Json.obj(
        "listing_date" -> "31-07-19"
      )
    )
    assert(x.isExpected)
    assert(x.toExpected == new DateTime(2019, 7, 31, 0, 0, 0, 0))
  }

  test("power") {
    assert(
      ScrapingHubAvitoCarsParser.parseRawHorsePower(
        Json.obj(
          "car_engine_power" -> "150 л.с."
        )
      ) == ParsedValue.Expected("150")
    )

    assert(
      ScrapingHubAvitoCarsParser.parseHorsePower(
        Json.obj(
          "car_engine_power" -> "150 л.с."
        )
      ) == ParsedValue.Expected(150)
    )
  }

  test("displacement") {
    assert(
      ScrapingHubAvitoCarsParser.parseDisplacement(
        Json.obj(
          "car_engine_volume" -> "5.4"
        )
      ) == ParsedValue.Expected(5400)
    )

    assert(
      ScrapingHubAvitoCarsParser.parseDisplacement(
        Json.obj(
          "car_engine_volume" -> "6.0+"
        )
      ) == ParsedValue.Expected(6000)
    )

    assert(
      ScrapingHubAvitoCarsParser.parseDisplacement(
        Json.obj(
          "car_engine_volume" -> "A"
        )
      ) == ParsedValue.Unexpected("A")
    )

    assert(
      ScrapingHubAvitoCarsParser.parseDisplacement(Json.obj()) ==
        ParsedValue.NoValue
    )
  }

  test("raw_displacement") {
    assert(
      ScrapingHubAvitoCarsParser.parseRawDisplacement(
        Json.obj(
          "car_engine_volume" -> "5.4"
        )
      ) == ParsedValue.Expected("5.4")
    )

    assert(
      ScrapingHubAvitoCarsParser.parseRawDisplacement(
        Json.obj(
          "car_engine_volume" -> "6.0+"
        )
      ) == ParsedValue.Expected("6.0")
    )

    assert(
      ScrapingHubAvitoCarsParser.parseRawDisplacement(
        Json.obj(
          "car_engine_volume" -> "A"
        )
      ) == ParsedValue.Unexpected("A")
    )

    assert(
      ScrapingHubAvitoCarsParser.parseRawDisplacement(Json.obj()) ==
        ParsedValue.NoValue
    )
  }

  test("unexpected year from url") {
    val url = "https://www.avito.ru/privodino/avtomobili/chevrolet_niva_6522565226520_1420242164"
    assert(
      ScrapingHubAvitoCarsParser.parseYear(
        url,
        Json.obj(
          "car_year" -> "2018"
        )
      ) == ParsedValue.Expected(2018)
    )

    assert(ScrapingHubAvitoCarsParser.parseYear(url, Json.obj()) == ParsedValue.NoValue)
  }

  test("parse year from url") {
    val url = "https://www.avito.ru/privodino/avtomobili/chevrolet_niva_2003_1420242164"
    assert(
      ScrapingHubAvitoCarsParser.parseYear(
        url,
        Json.obj(
          "car_year" -> "2018"
        )
      ) == ParsedValue.Expected(2018)
    )

    assert(
      ScrapingHubAvitoCarsParser.parseRawYear(
        url,
        Json.obj(
          "car_year" -> "2018"
        )
      ) == ParsedValue.Expected("2018")
    )

    assert(ScrapingHubAvitoCarsParser.parseYear(url, Json.obj()) == ParsedValue.Expected(2003))
    assert(ScrapingHubAvitoCarsParser.parseRawYear(url, Json.obj()) == ParsedValue.Expected("2003"))

    assert(
      ScrapingHubAvitoCarsParser.parseYear(
        "https://www.avito.ru/privodino/avtomobili/chevrolet_niva_1959_1420242164",
        Json.obj()
      ) == ParsedValue.NoValue
    )

    assert(
      ScrapingHubAvitoCarsParser.parseRawYear(
        "https://www.avito.ru/privodino/avtomobili/chevrolet_niva_1959_1420242164",
        Json.obj()
      ) == ParsedValue.NoValue
    )

    // TODO сломается в 2050, надо будет поправить =)
    assert(
      ScrapingHubAvitoCarsParser.parseYear(
        "https://www.avito.ru/privodino/avtomobili/chevrolet_niva_2050_1420242164",
        Json.obj()
      ) == ParsedValue.NoValue
    )

    assert(
      ScrapingHubAvitoCarsParser.parseRawYear(
        "https://www.avito.ru/privodino/avtomobili/chevrolet_niva_2050_1420242164",
        Json.obj()
      ) == ParsedValue.NoValue
    )
  }

  test("parse listing views from string like [1]") {
    import ScrapingHubAvitoCarsParser._

    val validJson = Json.obj("listing_views" -> "1")

    import ParsedValue._
    assert(parseViews(validJson) == Views(Expected(1), NoValue))
  }

  test("parse listing views from string like [123 (+45)]") {
    import ScrapingHubAvitoCarsParser._

    val validJson = Json.obj("listing_views" -> "5315 (+13)")
    val changedName = Json.obj("listing_view" -> "5315 (+13)")
    val changedFormat = Json.obj("listing_views" -> "5315/13")
    val wrongData = Json.obj("listing_views" -> "abc")
    val emptyField = Json.obj("listing_views" -> "")
    val emptyJson = Json.obj()

    val (expectedTotal, expectedDaily) = (Expected(5315), Expected(13))
    val unexpectedChangedFormat = Unexpected("5315/13")
    val unexpectedWrongData = Unexpected("abc")

    import ParsedValue._
    assert(parseViews(validJson) == Views(expectedTotal, expectedDaily))
    assert(parseViews(changedName) == Views(NoValue, NoValue))
    assert(parseViews(changedFormat) == Views(unexpectedChangedFormat, unexpectedChangedFormat))
    assert(parseViews(wrongData) == Views(unexpectedWrongData, unexpectedWrongData))
    assert(parseViews(emptyField) == Views(NoValue, NoValue))
    assert(parseViews(emptyJson) == Views(NoValue, NoValue))
  }

  test("car_vin_report") {
    import ScrapingHubAvitoCarsParser._

    val jsonStr =
      """{
        |"car_vin_report": {
        |  "report_date": "Отчёт Автотеки от 16 февраля 2021",
        |  "mileage": "1 раз продавался на Авито: 10 фото, старая цена и пробег",
        |  "docs": "Характеристики совпадают с ПТС",
        |  "mileage_history": "5 записей с 2014 года в истории пробега"
        |}
        |}""".stripMargin
    val json = Json.parse(jsonStr).asOpt[JsObject].value
    assert(parseCarVinReportDocs(json) == ParsedValue.Expected("Характеристики совпадают с ПТС"))
    assert(parseCarVinReportExaminationHistory(json) == ParsedValue.NoValue)
    assert(
      parseCarVinReportMileage(json) == ParsedValue.Expected("1 раз продавался на Авито: 10 фото, старая цена и пробег")
    )
    assert(parseCarVinReportMileageHistory(json) == ParsedValue.Expected("5 записей с 2014 года в истории пробега"))
    assert(parseCarVinReportOwners(json) == ParsedValue.NoValue)
    assert(parseCarVinReportReportDate(json) == ParsedValue.Expected("Отчёт Автотеки от 16 февраля 2021"))
    assert(parseCarVinReportOwnersHistory(json) == ParsedValue.NonParsed)
    assert(parseCarVinReportRestrictions(json) == ParsedValue.NonParsed)
    assert(parseCarVinReportStealings(json) == ParsedValue.NonParsed)
  }
}
