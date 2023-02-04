package ru.yandex.vertis.parsing.auto.parsers.haraba.cars.avito

import org.joda.time.{DateTime, DateTimeZone}
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import play.api.libs.json.{JsObject, Json}
import ru.yandex.vertis.parsing.auto.parsers.scrapinghub.cars.avito.ScrapingHubAvitoCarsParser
import ru.yandex.vertis.parsing.parsers.ParsedValue

@RunWith(classOf[JUnitRunner])
class HarabaAvitoCarsParserTest extends FunSuite {
  test("sh_last_visited") {
    assert(
      HarabaAvitoCarsParser
        .parseParseDate(
          Json.obj(
            "sh_last_visited" -> "2021-12-22 18:15:47+00:00"
          )
        )
        .isExpected
    )
  }

  test("avito url") {
    val url = "https://www.avito.ru/1065517972"
    assert(HarabaAvitoCarsParser.offerId(url) == "1065517972")
    assert(HarabaAvitoCarsParser.shouldProcessUrl(url) == ParsedValue.Expected(true))
  }

  test("car_owners_number: null") {
    val rawJson =
      """{
        |"car_owners_number": null
        |}""".stripMargin
    assert(
      HarabaAvitoCarsParser.parseOwnersCount(
        Json.parse(rawJson).as[JsObject]
      ) == ParsedValue.NoValue
    )
  }

  test("car_owners_number: 4") {
    val rawJson =
      """{
        |"car_owners_number": 4
        |}""".stripMargin
    assert(
      HarabaAvitoCarsParser.parseOwnersCount(
        Json.parse(rawJson).as[JsObject]
      ) == ParsedValue.Expected(4)
    )
  }

  test("listing_phone") {
    val rawJson =
      """{
        |"listing_phone": "+7(988)557-3670"
        |}""".stripMargin
    assert(
      HarabaAvitoCarsParser.parsePhones(
        Json.parse(rawJson).as[JsObject]
      ) == ParsedValue.Expected(Seq(ParsedValue.Expected("79885573670")))
    )
  }

  test("vin: null") {
    val rawJson =
      """{
        |"vin": null
        |}""".stripMargin
    assert(
      HarabaAvitoCarsParser.parseVin(
        Json.parse(rawJson).as[JsObject]
      ) == ParsedValue.NoValue
    )
  }

  test("listing_date") {
    val rawJson =
      """{
        |"listing_date": "2020-12-08T00:00:21"
        |}""".stripMargin
    assert(
      HarabaAvitoCarsParser.parseDisplayedPublishDate(
        Json.parse(rawJson).as[JsObject]
      ) == ParsedValue.Expected(new DateTime(2020, 12, 8, 0, 0, 21, 0))
    )
  }

  test("parse_date") {
    val rawJson =
      """{
        |"sh_last_visited": "2020-12-15 21:08:11+00:00"
        |}""".stripMargin
    val actual = HarabaAvitoCarsParser.parseParseDate(
      Json.parse(rawJson).as[JsObject]
    )
    val expected = ParsedValue.Expected(new DateTime(2020, 12, 15, 21, 8, 11, 0, DateTimeZone.UTC))
    assert(actual.isExpected)
    assert(actual.toExpected.getMillis - expected.value.getMillis == 0)
  }

  test("listing_phone_protected") {
    val rawJson =
      """{
        |"listing_phone_protected": true
        |}""".stripMargin
    assert(
      HarabaAvitoCarsParser.parseIsPhoneProtected(
        Json.parse(rawJson).as[JsObject]
      ) == ParsedValue.Expected(true)
    )
  }

  test("listing_phone_protected: no value") {
    val rawJson =
      """{
        |
        |}""".stripMargin
    assert(
      HarabaAvitoCarsParser.parseIsPhoneProtected(
        Json.parse(rawJson).as[JsObject]
      ) == ParsedValue.NoValue
    )
  }

  test("listing_phone_protected: string") {
    val rawJson =
      """{
        |"listing_phone_protected": "str"
        |}""".stripMargin
    assert(
      HarabaAvitoCarsParser.parseIsPhoneProtected(
        Json.parse(rawJson).as[JsObject]
      ) == ParsedValue.Unexpected(""""str"""")
    )
  }

  test("listing_phone_protected: number") {
    val rawJson =
      """{
        |"listing_phone_protected": 42
        |}""".stripMargin
    assert(
      HarabaAvitoCarsParser.parseIsPhoneProtected(
        Json.parse(rawJson).as[JsObject]
      ) == ParsedValue.Unexpected("42")
    )
  }

  test("raw_displacement") {
    val rawJson =
      """{
        |"car_engine_volume": "3,5"
        |}""".stripMargin
    assert(
      HarabaAvitoCarsParser.parseRawDisplacement(
        Json.parse(rawJson).as[JsObject]
      ) == ParsedValue.Expected("3.5")
    )
  }

  for {
    (raw, expected) <- Seq(("АКПП", "автомат"), ("CVT", "вариатор"), ("МКП", "механика"), ("Робот", "робот"))
  } test(s"raw_transmission $raw") {
    val rawJson =
      s"""{
          |"car_transmission": "$raw"
          |}""".stripMargin
    assert(
      HarabaAvitoCarsParser.parseRawTransmission(
        Json.parse(rawJson).as[JsObject]
      ) == ParsedValue.Expected(expected)
    )
    val rawJson2 =
      s"""{
         |"car_transmission": "$expected"
         |}""".stripMargin
    assert(
      HarabaAvitoCarsParser.parseRawTransmission(
        Json.parse(rawJson2).as[JsObject]
      ) == ParsedValue.Expected(expected)
    )
  }

  test("displacement") {
    val rawJson =
      """{
        |"car_engine_volume": "3,5"
        |}""".stripMargin
    assert(
      HarabaAvitoCarsParser.parseDisplacement(
        Json.parse(rawJson).as[JsObject]
      ) == ParsedValue.Expected(3500)
    )
  }

  test("images") {
    val rawJson =
      """{
        |"images": [
        |    "https:\/\/12.img.avito.st\/640x480\/10048099312.jpg",
        |    "https:\/\/08.img.avito.st\/640x480\/10048099308.jpg",
        |    "https:\/\/05.img.avito.st\/640x480\/10048099305.jpg",
        |    "https:\/\/15.img.avito.st\/640x480\/10048071915.jpg",
        |    "https:\/\/12.img.avito.st\/image\/1\/LWzDM7a_gYXtklGGlV9NaT-Qh499MIA_d5CDgXcIgVV0kA"
        |]
        |}""".stripMargin
    assert(
      HarabaAvitoCarsParser.parsePhotos(
        Json.parse(rawJson).as[JsObject]
      ) == ParsedValue.Expected(
        Seq(
          "https://12.img.avito.st/640x480/10048099312.jpg",
          "https://08.img.avito.st/640x480/10048099308.jpg",
          "https://05.img.avito.st/640x480/10048099305.jpg",
          "https://15.img.avito.st/640x480/10048071915.jpg",
          "https://12.img.avito.st/image/1/LWzDM7a_gYXtklGGlV9NaT-Qh499MIA_d5CDgXcIgVV0kA"
        )
      )
    )
  }
}
