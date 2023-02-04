package ru.yandex.vertis.parsing.auto.parsers.scrapinghub.cars.amru

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import play.api.libs.json.{JsObject, Json}
import ru.yandex.vertis.parsing.auto.parsers.Views
import ru.yandex.vertis.parsing.parsers.ParsedValue.{Expected, Unexpected}
import ru.yandex.vertis.parsing.parsers.{ParsedValue, ScrapingHubJsonUtils}

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class ScrapingHubAmruCarsParserTest extends FunSuite {
  test("remoteId") {
    val url = "https://auto.youla.ru/advert/used/kia/rio/avs-btsr-motors-na-komsomolskoy--2c089a0374660fb5/"
    assert(ScrapingHubAmruCarsParser.remoteId(url) == "am.ru|cars|2c089a0374660fb5")
  }

  test("new remoteId") {
    val url = "https://youla.ru/moskva/auto/novyy/mitsubishi-l200-2021-60fb60def03fbf45df109b85"
    assert(ScrapingHubAmruCarsParser.remoteId(url) == "am.ru|cars|60fb60def03fbf45df109b85")
  }

  test("listing_date") {
    val x = ScrapingHubAmruCarsParser.parseDisplayedPublishDate(
      Json.obj(
        "listing_date" -> "07-03-2019"
      )
    )
    assert(x.isExpected)
    assert(x.toExpected == new DateTime(2019, 3, 7, 0, 0, 0, 0))
  }

  test("engine type") {
    assert(
      ScrapingHubAmruCarsParser
        .parseRawEngineType(
          Json.obj(
            "car_engine" -> "Электро / 1 л"
          )
        )
        .contains("электро")
    )

    assert(
      ScrapingHubAmruCarsParser.parseEngineType(
        Json.obj(
          "car_engine" -> "Электро / 1 л"
        )
      ) == ParsedValue.Expected("ELECTRO")
    )

    assert(
      ScrapingHubAmruCarsParser
        .parseRawEngineType(
          Json.obj(
            "car_engine" -> "Дизель / 1.1 л"
          )
        )
        .contains("дизель")
    )

    assert(
      ScrapingHubAmruCarsParser.parseEngineType(
        Json.obj(
          "car_engine" -> "Дизель / 1.1 л"
        )
      ) == ParsedValue.Expected("DIESEL")
    )

    assert(
      ScrapingHubAmruCarsParser
        .parseRawEngineType(
          Json.obj(
            "car_engine" -> "Гибрид"
          )
        )
        .contains("гибрид")
    )

    assert(
      ScrapingHubAmruCarsParser.parseEngineType(
        Json.obj(
          "car_engine" -> "Гибрид"
        )
      ) == ParsedValue.Expected("HYBRID")
    )

    assert(
      ScrapingHubAmruCarsParser
        .parseRawEngineType(
          Json.obj(
            "car_engine" -> "1.1 л"
          )
        )
        .isUnexpected
    )

    assert(
      ScrapingHubAmruCarsParser.parseEngineType(
        Json.obj(
          "car_engine" -> "1.1 л"
        )
      ) == ParsedValue.NoValue
    )
  }

  test("displacement") {
    assert(
      ScrapingHubAmruCarsParser.parseRawDisplacement(
        Json.obj(
          "car_engine" -> "Электро / 1 л"
        )
      ) == ParsedValue.Expected("1")
    )

    assert(
      ScrapingHubAmruCarsParser.parseDisplacement(
        Json.obj(
          "car_engine" -> "Электро / 1 л"
        )
      ) == ParsedValue.Expected(1000)
    )

    assert(
      ScrapingHubAmruCarsParser.parseRawDisplacement(
        Json.obj(
          "car_engine" -> "Электро / 5.8 л"
        )
      ) == ParsedValue.Expected("5.8")
    )

    assert(
      ScrapingHubAmruCarsParser.parseDisplacement(
        Json.obj(
          "car_engine" -> "Электро / 5.8 л"
        )
      ) == ParsedValue.Expected(5800)
    )

    assert(
      ScrapingHubAmruCarsParser.parseRawDisplacement(
        Json.obj(
          "car_engine" -> "5.8 л"
        )
      ) == ParsedValue.Expected("5.8")
    )

    assert(
      ScrapingHubAmruCarsParser.parseRawDisplacement(
        Json.obj(
          "car_engine" -> "6 л"
        )
      ) == ParsedValue.Expected("6")
    )

    assert(
      ScrapingHubAmruCarsParser.parseRawDisplacement(
        Json.obj(
          "car_engine" -> "Бензин"
        )
      ) == ParsedValue.NoValue
    )
  }

  test("getFields") {
    val in = this.getClass.getResourceAsStream("/amru_scrapinghub_parse_data.json")
    val json = Json.parse(scala.io.Source.fromInputStream(in).mkString).asInstanceOf[JsObject]
    val parsedFieldsInRow = ScrapingHubJsonUtils.getFields(json)
    val knownFields = ScrapingHubAmruCarsParsedFields.knownFieldsSet
    assert(parsedFieldsInRow.forall(f => knownFields.contains(f)))
  }

  test("owners") {
    val json = Json.parse("""{
        |  "car_vin_report": {
        |    "owners":[{"f1":1}, {"f2":2}, {"f3":3}]
        |  }
        |}""".stripMargin).asInstanceOf[JsObject]
    assert(ScrapingHubAmruCarsParser.parseOwnersCount(json) == ParsedValue.Expected(3))
  }

  test("owners: no owners") {
    val json = Json.parse("""{
        |  "car_vin_report": {
        |    "f1":1
        |  }
        |}""".stripMargin).asInstanceOf[JsObject]
    assert(ScrapingHubAmruCarsParser.parseOwnersCount(json) == ParsedValue.NoValue)
  }

  test("owners: no car vin report") {
    val json = Json.parse("""{
        |  "f1":1
        |}""".stripMargin).asInstanceOf[JsObject]
    assert(ScrapingHubAmruCarsParser.parseOwnersCount(json) == ParsedValue.NoValue)
  }

  test("car name") {
    assert(
      ScrapingHubAmruCarsParser
        .parseFn(Json.parse("""{
        |  "car_name":"Geely Emgrand X7 1 поколение [рестайлинг], кроссовер 5 дв."
        |}""".stripMargin).asInstanceOf[JsObject])
        .contains("Geely Emgrand X7")
    )
    assert(ScrapingHubAmruCarsParser.parseFn(Json.parse("""{
        |  "car_name":"Kia Soul, хетчбэк 5 дв."
        |}""".stripMargin).asInstanceOf[JsObject]).contains("Kia Soul"))
    assert(ScrapingHubAmruCarsParser.parseFn(Json.parse("""{
        |  "car_name":"Suzuki SX4 1 поколение, седан 4 дв."
        |}""".stripMargin).asInstanceOf[JsObject]).contains("Suzuki SX4"))
  }

  test("listing views") {
    import ScrapingHubAmruCarsParser._

    val validJson = Json.obj("listing_views_total" -> "149", "listing_views_today" -> "1")
    val changedName = Json.obj("listing_views_total" -> "149", "listing_views_delta" -> "1")
    val changedFormat = Json.obj("listing_views_total" -> "149", "listing_views_today" -> "10+")
    val emptyField = Json.obj("listing_views_total" -> "149")
    val emptyJson = Json.obj()

    val (expectedTotal, expectedDaily) = (Expected(149), Expected(1))
    val unexpected = Unexpected("10+")

    import ParsedValue._
    assert(parseViews(validJson) == Views(expectedTotal, expectedDaily))
    assert(parseViews(changedName) == Views(expectedTotal, NoValue))
    assert(parseViews(changedFormat) == Views(expectedTotal, unexpected))
    assert(parseViews(emptyField) == Views(expectedTotal, NoValue))
    assert(parseViews(emptyJson) == Views(NoValue, NoValue))

  }
}
