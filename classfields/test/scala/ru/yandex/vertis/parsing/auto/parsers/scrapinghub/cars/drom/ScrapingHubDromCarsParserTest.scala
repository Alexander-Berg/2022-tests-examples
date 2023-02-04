package ru.yandex.vertis.parsing.auto.parsers.scrapinghub.cars.drom

import java.io.{File, InputStream}
import java.nio.file.{Files, Paths, StandardCopyOption}
import org.joda.time.{DateTime, DateTimeZone}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, OptionValues}
import play.api.libs.json.{JsObject, Json}
import ru.yandex.vertis.parsing.auto.parsers.{OwnersHistoryElem, Views}
import ru.yandex.vertis.parsing.clients.bucket.ScrapingHubImportProcessor
import ru.yandex.vertis.parsing.parsers.ParsedValue
import ru.yandex.vertis.parsing.parsers.ParsedValue.{Expected, Unexpected}
import ru.yandex.vertis.parsing.util.IO

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class ScrapingHubDromCarsParserTest extends FunSuite with OptionValues {
  test("sh_last_visited") {
    val x = ScrapingHubDromCarsParser
      .parseParseDate(
        Json.obj(
          "sh_last_visited" -> "2018-12-20T05:06:06+00:00"
        )
      )
      .toOption
      .value
    assert(x == new DateTime(2018, 12, 20, 5, 6, 6, DateTimeZone.UTC))
  }

  test("listing_date from fresh dump") {
    val io = new IO(new File("."))
    val tmpFile = io.newTempFile("scrapingHubDromCarsParserTest", "")
    val in: InputStream = this.getClass.getResourceAsStream("/2019-03-07-10-31-58-fresh-drom.json")
    Files.copy(in, Paths.get(tmpFile.getAbsolutePath), StandardCopyOption.REPLACE_EXISTING)

    val processor = new ScrapingHubImportProcessor(tmpFile)

    val rows = processor.process { it =>
      it.toList
    }

    val listingDates = rows.map(row => {
      val json = ScrapingHubDromCarsParser.parseJson(row.rawJson).toOption.value
      ScrapingHubDromCarsParser.parseDisplayedPublishDate(json)
    })

    listingDates.zipWithIndex.foreach {
      case (ld, idx) =>
        assert(ld.isExpected, s"row $idx")
        assert(ld.toExpected == new DateTime(2019, 3, 7, 0, 0, 0, 0), s"row $idx")
    }
  }

  test("listing_date") {
    val x = ScrapingHubDromCarsParser.parseDisplayedPublishDate(
      Json.obj(
        "listing_date" -> "07-03-2019"
      )
    )
    assert(x.isExpected)
    assert(x.toExpected == new DateTime(2019, 3, 7, 0, 0, 0, 0))
  }

  test("incorrect listing_date") {
    val x = ScrapingHubDromCarsParser.parseDisplayedPublishDate(
      Json.obj(
        "listing_date" -> "07-03-19"
      )
    )
    assert(x.isExpected)
    assert(x.toExpected == new DateTime(2019, 3, 7, 0, 0, 0, 0))
  }

  test("incorrect listing_date 2") {
    val x = ScrapingHubDromCarsParser.parseDisplayedPublishDate(
      Json.obj(
        "listing_date" -> "31-07-19"
      )
    )
    assert(x.isExpected)
    assert(x.toExpected == new DateTime(2019, 7, 31, 0, 0, 0, 0))
  }

  test("seller_type") {
    val x = ScrapingHubDromCarsParser.parseIsDealer(
      Json.obj(
        "seller_type" -> new String("private")
      )
    )
    assert(x.isExpected)
    assert(!x.toExpected)
  }

  test("displacement") {
    assert(
      ScrapingHubDromCarsParser.parseRawDisplacement(
        Json.obj(
          "car_engine" -> "бензин, 2.4 л, гибрид, ГБО"
        )
      ) == ParsedValue.Expected("2.4")
    )

    assert(
      ScrapingHubDromCarsParser.parseRawDisplacement(
        Json.obj(
          "car_engine" -> "бензин, 1.6 л"
        )
      ) == ParsedValue.Expected("1.6")
    )

    assert(
      ScrapingHubDromCarsParser.parseRawDisplacement(
        Json.obj(
          "car_engine" -> "бензин, 1.5 л, гибрид"
        )
      ) == ParsedValue.Expected("1.5")
    )

    assert(
      ScrapingHubDromCarsParser.parseRawDisplacement(
        Json.obj(
          "car_engine" -> "бензин"
        )
      ) == ParsedValue.Ignored("бензин")
    )

    assert(
      ScrapingHubDromCarsParser.parseRawDisplacement(
        Json.obj(
          "car_engine" -> "1.6 л"
        )
      ) == ParsedValue.Expected("1.6")
    )
  }

  test("engine type") {
    assert(
      ScrapingHubDromCarsParser.parseEngineType(
        Json.obj(
          "car_engine" -> "бензин, 2.4 л, гибрид, ГБО"
        )
      ) == ParsedValue.Expected("HYBRID")
    )

    assert(
      ScrapingHubDromCarsParser.parseEngineType(
        Json.obj(
          "car_engine" -> "бензин, 2.4 л, ГБО"
        )
      ) == ParsedValue.Expected("LPG")
    )

    assert(
      ScrapingHubDromCarsParser.parseEngineType(
        Json.obj(
          "car_engine" -> "бензин, 2.4 л"
        )
      ) == ParsedValue.Expected("GASOLINE")
    )

    assert(
      ScrapingHubDromCarsParser.parseEngineType(
        Json.obj(
          "car_engine" -> "бензин"
        )
      ) == ParsedValue.Expected("GASOLINE")
    )

    assert(
      ScrapingHubDromCarsParser.parseEngineType(
        Json.obj(
          "car_engine" -> "2.4 л"
        )
      ) == ParsedValue.NoValue
    )
  }

  test("raw engine type") {
    assert(
      ScrapingHubDromCarsParser
        .parseRawEngineType(
          Json.obj(
            "car_engine" -> "бензин, 2.4 л, гибрид, ГБО"
          )
        )
        .contains("гибрид")
    )

    assert(
      ScrapingHubDromCarsParser
        .parseRawEngineType(
          Json.obj(
            "car_engine" -> "бензин, 2.4 л, ГБО"
          )
        )
        .contains("гбо")
    )

    assert(
      ScrapingHubDromCarsParser
        .parseRawEngineType(
          Json.obj(
            "car_engine" -> "бензин, 2.4 л"
          )
        )
        .contains("бензин")
    )

    assert(
      ScrapingHubDromCarsParser
        .parseRawEngineType(
          Json.obj(
            "car_engine" -> "бензин"
          )
        )
        .contains("бензин")
    )

    assert(
      ScrapingHubDromCarsParser
        .parseRawEngineType(
          Json.obj(
            "car_engine" -> "2.4 л"
          )
        )
        .isIgnored
    )
  }

  test("address") {
    val url = "https://spb.drom.ru/renault/kaptur/34019777.html"
    assert(
      ScrapingHubDromCarsParser
        .parseAddress(
          Json.obj(
            "listing_city" -> "Санкт-Петербург"
          )
        )
        .filter(_.isExpected)
        .map(_.toExpected)
        .contains("Санкт-Петербург")
    )
    assert(
      ScrapingHubDromCarsParser
        .parseAddress(
          Json.obj(
            "dealership" -> Json.obj(
              "dealership_city" -> "Санкт-Петербург"
            )
          )
        )
        .filter(_.isExpected)
        .map(_.toExpected)
        .contains("Санкт-Петербург")
    )
    assert(ScrapingHubDromCarsParser.parseAddressFromUrl(url).toOption.contains("SPB"))
  }

  test("address from several sources") {
    val url = "https://spb.drom.ru/renault/kaptur/34019777.html"
    assert(
      ScrapingHubDromCarsParser
        .parseAddress(
          Json.obj(
            "listing_city" -> "Санкт-Петербург 1",
            "dealership" -> Json.obj(
              "dealership_city" -> "Санкт-Петербург 2"
            )
          )
        )
        .map(_.toExpected) == Seq("Санкт-Петербург 1", "Санкт-Петербург 2")
    )
  }

  test("owner name") {
    assert(
      ScrapingHubDromCarsParser
        .parseOwnerName(
          Json.obj(
            "dealership" -> Json.obj(
              "dealership_name" -> "Renault РОЛЬФ Автопрайм"
            )
          )
        )
        .contains("Renault РОЛЬФ Автопрайм")
    )
    assert(ScrapingHubDromCarsParser.parseOwnerName(Json.obj()).isNoValue)
  }

  test("dealer name") {
    assert(
      ScrapingHubDromCarsParser
        .parseDealerName(
          Json.obj(
            "dealership" -> Json.obj(
              "dealership_name" -> "Renault РОЛЬФ Автопрайм"
            )
          )
        )
        .contains("Renault РОЛЬФ Автопрайм")
    )
    assert(ScrapingHubDromCarsParser.parseDealerName(Json.obj()).isNoValue)
  }

  test("dealer url") {
    assert(
      ScrapingHubDromCarsParser
        .parseDealerUrl(
          Json.obj(
            "dealership" -> Json.obj(
              "dealership_url" -> "Renault РОЛЬФ Автопрайм"
            )
          )
        )
        .contains("Renault РОЛЬФ Автопрайм")
    )
    assert(ScrapingHubDromCarsParser.parseDealerUrl(Json.obj()).isNoValue)
  }

  test("listing views") {
    import ScrapingHubDromCarsParser._

    val validJson = Json.obj("listing_views" -> "20")
    val changedName = Json.obj("listing_view" -> "20")
    val changedFormat = Json.obj("listing_views" -> "5315/13")
    val emptyField = Json.obj("listing_views" -> "")
    val emptyJson = Json.obj()

    val expectedTotal = Expected(20)
    val unexpected = Unexpected("5315/13")

    import ParsedValue._
    assert(parseViews(validJson) == Views(expectedTotal, NonParsed))
    assert(parseViews(changedName) == Views(NoValue, NonParsed))
    assert(parseViews(changedFormat) == Views(unexpected, NonParsed))
    assert(parseViews(emptyField) == Views(NoValue, NonParsed))
    assert(parseViews(emptyJson) == Views(NoValue, NonParsed))

  }

  test("car_vin_report") {
    import ScrapingHubDromCarsParser._

    val jsonStr =
      """{
        |"car_vin_report": {
        |  "stealings": false,
        |  "restrictions": false,
        |  "owners": [
        |    {"owner": "\u0444\u0438\u0437. \u043b\u0438\u0446\u043e", "period": "1998-04-04 - 2009-12-04"},
        |    {"owner": "\u0444\u0438\u0437. \u043b\u0438\u0446\u043e", "period": "2019-11-26"}]
        |}
        |}""".stripMargin
    val json = Json.parse(jsonStr).asOpt[JsObject].value
    assert(parseCarVinReportDocs(json) == ParsedValue.NonParsed)
    assert(parseCarVinReportExaminationHistory(json) == ParsedValue.NonParsed)
    assert(parseCarVinReportMileage(json) == ParsedValue.NonParsed)
    assert(parseCarVinReportMileageHistory(json) == ParsedValue.NonParsed)
    assert(parseCarVinReportOwners(json) == ParsedValue.NonParsed)
    assert(parseCarVinReportReportDate(json) == ParsedValue.NonParsed)
    assert(parseCarVinReportStealings(json) == ParsedValue.Expected(false))
    assert(parseCarVinReportRestrictions(json) == ParsedValue.Expected(false))
    assert(
      parseCarVinReportOwnersHistory(json) == ParsedValue.Expected(
        Seq(
          OwnersHistoryElem(ParsedValue.Expected("физ. лицо"), ParsedValue.Expected("1998-04-04 - 2009-12-04")),
          OwnersHistoryElem(ParsedValue.Expected("физ. лицо"), ParsedValue.Expected("2019-11-26"))
        )
      )
    )
  }

  test("price_score") {
    import ScrapingHubDromCarsParser._

    val jsonStr =
      """{
        |"listing_price_score": "\u0425\u043e\u0440\u043e\u0448\u0430\u044f \u0446\u0435\u043d\u0430",
        |"listing_price_score_estimate": "100000"
        |}""".stripMargin
    val json = Json.parse(jsonStr).asOpt[JsObject].value
    assert(parsePriceScoreListingPriceScore(json) == ParsedValue.Expected("Хорошая цена"))
    assert(parsePriceScoreListingPriceScoreEstimate(json) == ParsedValue.Expected("100000"))
  }
}
