package ru.yandex.vertis.parsing.auto.parsers

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, OptionValues}
import play.api.libs.json.Json
import ru.auto.api.ApiOfferModel.PtsStatus
import ru.auto.api.CommonModel.SteeringWheel
import ru.auto.api.TrucksModel.{Engine, GearType, Transmission, WheelDrive}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.auto.parsers.WebminerJsonUtils.parseJson
import ru.yandex.vertis.parsing.auto.parsers.webminer.trucks.drom.DromTrucksParser
import ru.yandex.vertis.parsing.parsers.ParsedValue

import scala.language.implicitConversions

/**
  * Created by andrey on 1/9/18.
  */
//scalastyle:off
@RunWith(classOf[JUnitRunner])
class NewDromTrucksParserTest extends FunSuite with OptionValues with MockitoSupport {
  test("parse") {
    val url = "https://spec.drom.ru/urai/truck/prodam-gazel-termobudka-59940174.html"
    val rawJson =
      """[{
        |"owner":["{
        |  \"id\":[\"4059334\"],
        |  \"name\":[\"Петр\"],
        |  \"email\":[\"test@example.org\"],
        |  \"login\":[\"usamig84\"]}"],
        |"address":["Урай"],
        |"year":["1998"],
        |"phone":["+7 992 351-28-59 "],
        |"price":["120 000"],
        |"fn":["ГАЗ Газель"],
        |"photo":["https://static.baza.farpost.ru/v/1514823200077_bulletin","https://static.baza.farpost.ru/v/1514823224243_bulletin","https://static.baza.farpost.ru/v/1514823236196_bulletin","https://static.baza.farpost.ru/v/1514823250698_bulletin"],
        |"description":["Продам газель термобудка,высота 180 см,магнитола."],
        |"offer_id":["59940174"],
        |"info":["{
        |  \"transmission\":[\"Механическая\"],
        |  \"engine\":[\"2 400 куб. см.\"],
        |  \"wheel-drive\":[\"4x2\"],
        |  \"mileage\":[\"45000\"],
        |  \"seats\":[\"8\"],
        |  \"mileage_in_russia\":[\"С пробегом\"],
        |  \"documents\":[\"Есть ПТС\"],
        |  \"wheel\":[\"Левый\"],
        |  \"fuel\":[\"Дизель\"],
        |  \"state\":[\"Хорошее\"],
        |  \"type\":[\"Изотермический фургон\"],
        |  \"category\":[\"Грузовики и спецтехника\"],
        |  \"capacity\":[\"2 000 кг.\"]}"],
        |"parse_date":["2018-01-02T08:21:18.138+03:00"]}]""".stripMargin.replace("\n", "")
    assert(DromTrucksParser.hash(url) == "9c8768f8a325dae2839557d1f095eb85")
    assert(DromTrucksParser.offerId(url) == "59940174")
    val json = parseJson(Json.parse(rawJson))
    assert(DromTrucksParser.parseEmail(json).toOption.value == "test@example.org")
    assert(DromTrucksParser.parseFn(json).toOption.value == "ГАЗ Газель")
    assert(
      DromTrucksParser.parseDescription(json).toOption.value == "Продам газель термобудка,высота 180 см,магнитола."
    )
    assert(DromTrucksParser.parsePrice(json).toOption.value == 120000)
    assert(DromTrucksParser.parseYear(url, json).toOption.value == 1998)
    assert(DromTrucksParser.parsePhones(json).toOption.value == Seq(ParsedValue.Expected("79923512859")))
    assert(
      DromTrucksParser.parsePhotos(json).getOrElse(Seq.empty) == Seq(
        "https://static.baza.farpost.ru/v/1514823200077_bulletin",
        "https://static.baza.farpost.ru/v/1514823224243_bulletin",
        "https://static.baza.farpost.ru/v/1514823236196_bulletin",
        "https://static.baza.farpost.ru/v/1514823250698_bulletin"
      )
    )
    assert(DromTrucksParser.parseCapacity(json).toOption.value == 2000)
    assert(DromTrucksParser.parsePts(json) == ParsedValue.Expected(PtsStatus.ORIGINAL))
    assert(DromTrucksParser.parseDisplacement(json).toOption.value == 2400)
    assert(DromTrucksParser.parseSeats(json).toOption.value == 8)
    assert(DromTrucksParser.parseWheelDrive(json) == ParsedValue.Expected(WheelDrive.WD_4x2))
  }

  test("offerId") {
    assert(
      DromTrucksParser
        .offerId("https://spec.drom.ru/chelyabinsk/truck/shassi-kamaz-43253-1017-99-55468377.html") == "55468377"
    )
    assert(
      DromTrucksParser
        .offerId("https://spec.drom.ru/chelyabinsk/truck/shassi-kamaz-43253-1017-99-55468377") == "55468377"
    )
    assert(
      DromTrucksParser.offerId(
        "https://spec.drom.ru/khabarovsk/trailer/pricep-berg-dlja-modeli-grantour-af-g1751099634.html"
      ) == "1751099634"
    )
  }

  test("shouldProcessUrl") {
    implicit def expected2bool(v: ParsedValue[Boolean]): Boolean = v.toOption.contains(true)

    assert(DromTrucksParser.checkUrl("https://spec.drom.ru/urai/truck/prodam-gazel-termobudka-59940174.html"))
    assert(DromTrucksParser.checkUrl("https://spec.drom.ru/urai/bus/prodam-gazel-termobudka-59940174.html"))
    assert(DromTrucksParser.checkUrl("https://spec.drom.ru/urai/trailer/prodam-gazel-termobudka-59940174.html"))
    assert(!DromTrucksParser.checkUrl("https://spec.drom.ru/urai/xyxy/prodam-gazel-termobudka-59940174.html"))
    assert(!DromTrucksParser.checkUrl("https://spec.drom.ru/prodam-gazel-termobudka-59940174.html"))
  }

  test("parseOwnerName") {
    val json1 = parseJson(
      Json.parse(
        """[{
        |"owner":["{
        |  \"id\":[\"4059334\"],
        |  \"name\":[\"Петр\"],
        |  \"login\":[\"usamig84\"]}"]}]""".stripMargin.replace("\n", "")
      )
    )
    assert(DromTrucksParser.parseOwnerName(json1).toOption.value == "Петр")
    val json2 = parseJson(
      Json.parse(
        """[{
        |"owner":["{
        |  \"id\":[\"4059334\"],
        |  \"login\":[\"usamig84\"]}"]}]""".stripMargin.replace("\n", "")
      )
    )
    assert(DromTrucksParser.parseOwnerName(json2).isNoValue)
  }

  test("parseRegion") {
    val url = "https://spec.drom.ru/urai/truck/prodam-gazel-termobudka-59940174.html"
    val json = parseJson(
      Json.parse(
        """[{"address":["Урай"]}]""".stripMargin.replace("\n", "")
      )
    )
    val res = DromTrucksParser.parseAddress(json)
    assert(res.headOption.value.toExpected == "Урай")
  }

  test("parseRegion from url") {
    val url = "https://spec.drom.ru/urai/truck/prodam-gazel-termobudka-59940174.html"
    val res = DromTrucksParser.parseAddressFromUrl(url)
    assert(res.toExpected == "URAI")
  }

  test("parseParseDate") {
    val json1 = parseJson(Json.parse("""[{"parse_date":["2018-01-02T08:21:18.138+03:00"]}]"""))
    assert(DromTrucksParser.parseParseDate(json1).toExpected == DateTime.parse("2018-01-02T08:21:18.138+03:00"))
    val json2 = parseJson(Json.parse("""[{}]"""))
    assert(DromTrucksParser.parseParseDate(json2).isNoValue)
  }

  test("parseEngineType") {
    assert(
      DromTrucksParser
        .parseEngineType(parseJson(Json.parse("""[{"info":["{\"fuel\":[\"Дизель\"]}"]}]"""))) == ParsedValue
        .Expected(Engine.DIESEL)
    )
    assert(
      DromTrucksParser
        .parseEngineType(parseJson(Json.parse("""[{"info":["{\"fuel\":[\"Дизельный\"]}"]}]"""))) == ParsedValue
        .Expected(Engine.DIESEL)
    )
    assert(
      DromTrucksParser
        .parseEngineType(parseJson(Json.parse("""[{"info":["{\"fuel\":[\"Бензин\"]}"]}]"""))) == ParsedValue
        .Expected(Engine.GASOLINE)
    )
    assert(
      DromTrucksParser
        .parseEngineType(parseJson(Json.parse("""[{"info":["{\"fuel\":[\"Бензиновый\"]}"]}]"""))) == ParsedValue
        .Expected(Engine.GASOLINE)
    )
    assert(
      DromTrucksParser
        .parseEngineType(parseJson(Json.parse("""[{"info":["{\"fuel\":[\"Электричество\"]}"]}]"""))) == ParsedValue
        .Expected(Engine.ELECTRO)
    )
    assert(
      DromTrucksParser.parseEngineType(parseJson(Json.parse("""[{"info":["{\"fuel\":[\"Газ\"]}"]}]"""))) == ParsedValue
        .Ignored("Газ")
    )
    assert(DromTrucksParser.parseEngineType(parseJson(Json.parse("""[{"info":["{}"]}]"""))).isNoValue)
    assert(
      DromTrucksParser.parseEngineType(parseJson(Json.parse("""[{"info":["{\"fuel\":[\"Пыщь\"]}"]}]"""))) == ParsedValue
        .Unexpected("Пыщь")
    )
  }

  test("parseMileage") {
    val json1 = parseJson(Json.parse("""[{"info":["{\"mileage\":[\"45000\"]}"]}]"""))
    assert(DromTrucksParser.parseMileage(json1).toOption.value == 45000)
    val json2 = parseJson(Json.parse("""[{"info":["{\"mileage\":[\"800\"]}"]}]"""))
    assert(DromTrucksParser.parseMileage(json2).toOption.value == 800000)
  }

  test("parseNotBeaten") {
    assert(
      DromTrucksParser
        .parseNotBeaten(parseJson(Json.parse("""[{"info":["{\"state\":[\"Новое\"]}"]}]"""))) == ParsedValue
        .Expected(true)
    )
    assert(
      DromTrucksParser
        .parseNotBeaten(parseJson(Json.parse("""[{"info":["{\"state\":[\"новое\"]}"]}]"""))) == ParsedValue
        .Expected(true)
    )
    assert(
      DromTrucksParser
        .parseNotBeaten(parseJson(Json.parse("""[{"info":["{\"state\":[\"Хорошее\"]}"]}]"""))) == ParsedValue
        .Expected(true)
    )
    assert(
      DromTrucksParser
        .parseNotBeaten(parseJson(Json.parse("""[{"info":["{\"state\":[\"Удовлетворительное\"]}"]}]"""))) == ParsedValue
        .Expected(true)
    )
    assert(
      DromTrucksParser
        .parseNotBeaten(parseJson(Json.parse("""[{"info":["{\"state\":[\"После ДТП\"]}"]}]"""))) == ParsedValue
        .Expected(false)
    )
    assert(
      DromTrucksParser.parseNotBeaten(parseJson(Json.parse("""[{"info":["{\"state\":[\"RTRT\"]}"]}]"""))) == ParsedValue
        .Unexpected("RTRT")
    )
    assert(DromTrucksParser.parseNotBeaten(parseJson(Json.parse("""[{"info":["{}"]}]"""))).isNoValue)
    assert(
      DromTrucksParser.parseNotBeaten(parseJson(Json.parse("""[{"info":["{\"state\":[\"Б/у\"]}"]}]"""))) == ParsedValue
        .Expected(true)
    )
    assert(
      DromTrucksParser.parseNotBeaten(
        parseJson(Json.parse("""[{"info":["{\"state\":[\"Б/у без пробега по рф\"]}"]}]"""))
      ) == ParsedValue.Expected(true)
    )
  }

  test("parseTransmission") {
    assert(
      DromTrucksParser.parseTransmission(
        parseJson(Json.parse("""[{"info":["{\"transmission\":[\"Механическая\"]}"]}]"""))
      ) == ParsedValue.Expected(Transmission.MECHANICAL)
    )
    assert(
      DromTrucksParser.parseTransmission(
        parseJson(Json.parse("""[{"info":["{\"transmission\":[\"Автоматическая\"]}"]}]"""))
      ) == ParsedValue.Expected(Transmission.AUTOMATIC)
    )
    assert(
      DromTrucksParser
        .parseTransmission(parseJson(Json.parse("""[{"info":["{\"transmission\":[\"Вариатор\"]}"]}]"""))) == ParsedValue
        .Expected(Transmission.VARIATOR)
    )
    assert(
      DromTrucksParser
        .parseTransmission(parseJson(Json.parse("""[{"info":["{\"transmission\":[\"RTRT\"]}"]}]"""))) == ParsedValue
        .Unexpected("RTRT")
    )
    assert(DromTrucksParser.parseTransmission(parseJson(Json.parse("""[{"info":["{}"]}]"""))).isNoValue)
  }

  test("parseSteeringWheel") {
    assert(
      DromTrucksParser
        .parseSteeringWheel(parseJson(Json.parse("""[{"info":["{\"wheel\":[\"Левый\"]}"]}]"""))) == ParsedValue
        .Expected(SteeringWheel.LEFT)
    )
    assert(
      DromTrucksParser
        .parseSteeringWheel(parseJson(Json.parse("""[{"info":["{\"wheel\":[\"Правый\"]}"]}]"""))) == ParsedValue
        .Expected(SteeringWheel.RIGHT)
    )
    assert(
      DromTrucksParser
        .parseSteeringWheel(parseJson(Json.parse("""[{"info":["{\"wheel\":[\"RTRT\"]}"]}]"""))) == ParsedValue
        .Unexpected("RTRT")
    )
    assert(DromTrucksParser.parseSteeringWheel(parseJson(Json.parse("""[{"info":["{}"]}]"""))).isNoValue)
  }

  test("parseGearType") {
    assert(
      DromTrucksParser
        .parseGearType(parseJson(Json.parse("""[{"info":["{\"wheel-drive\":[\"8x6\"]}"]}]"""))) == ParsedValue
        .Ignored("8x6")
    )
    assert(
      DromTrucksParser
        .parseGearType(parseJson(Json.parse("""[{"info":["{\"wheel-drive\":[\"Полный\"]}"]}]"""))) == ParsedValue
        .Expected(GearType.FULL)
    )
    assert(
      DromTrucksParser
        .parseGearType(parseJson(Json.parse("""[{"info":["{\"wheel-drive\":[\"Задний\"]}"]}]"""))) == ParsedValue
        .Expected(GearType.BACK)
    )
    assert(
      DromTrucksParser.parseGearType(
        parseJson(Json.parse("""[{"info":["{\"wheel-drive\":[\"Полный подключаемый\"]}"]}]"""))
      ) == ParsedValue.Expected(GearType.FULL_PLUG)
    )
    assert(
      DromTrucksParser
        .parseGearType(parseJson(Json.parse("""[{"info":["{\"wheel-drive\":[\"8x2\"]}"]}]"""))) == ParsedValue
        .Ignored("8x2")
    )
    assert(
      DromTrucksParser
        .parseGearType(parseJson(Json.parse("""[{"info":["{\"wheel-drive\":[\"blabla\"]}"]}]"""))) == ParsedValue
        .Unexpected("blabla")
    )
    assert(DromTrucksParser.parseGearType(parseJson(Json.parse("""[{"info":["{}"]}]"""))).isNoValue)
  }

  test("parseWheelDrive") {
    assert(
      DromTrucksParser
        .parseWheelDrive(parseJson(Json.parse("""[{"info":["{\"wheel-drive\":[\"8x6\"]}"]}]"""))) == ParsedValue
        .Ignored("8x6")
    )
    assert(
      DromTrucksParser
        .parseWheelDrive(parseJson(Json.parse("""[{"info":["{\"wheel-drive\":[\"Полный\"]}"]}]"""))) == ParsedValue
        .Ignored("Полный")
    )
    assert(
      DromTrucksParser
        .parseWheelDrive(parseJson(Json.parse("""[{"info":["{\"wheel-drive\":[\"Задний\"]}"]}]"""))) == ParsedValue
        .Ignored("Задний")
    )
    assert(
      DromTrucksParser.parseWheelDrive(
        parseJson(Json.parse("""[{"info":["{\"wheel-drive\":[\"Полный подключаемый\"]}"]}]"""))
      ) == ParsedValue.Ignored("Полный подключаемый")
    )
    assert(
      DromTrucksParser
        .parseWheelDrive(parseJson(Json.parse("""[{"info":["{\"wheel-drive\":[\"8x2\"]}"]}]"""))) == ParsedValue
        .Expected(WheelDrive.WD_8x2)
    )
    assert(
      DromTrucksParser
        .parseWheelDrive(parseJson(Json.parse("""[{"info":["{\"wheel-drive\":[\"blabla\"]}"]}]"""))) == ParsedValue
        .Unexpected("blabla")
    )
    assert(DromTrucksParser.parseWheelDrive(parseJson(Json.parse("""[{"info":["{}"]}]"""))).isNoValue)
  }

  test("parseIsDealer") {
    val json1 = parseJson(Json.parse("""[{}]"""))
    assert(DromTrucksParser.parseIsDealer(json1).isNoValue)
    val json2 = parseJson(Json.parse("""[{"user_offer_count":["3"]}]"""))
    assert(!DromTrucksParser.parseIsDealer(json2).toOption.value)
    val json3 = parseJson(Json.parse("""[{"user_offer_count":["4"]}]"""))
    assert(DromTrucksParser.parseIsDealer(json3).toOption.value)
    val json4 = parseJson(Json.parse("""[{"phone":["+7 915 456-67-89", "8 800 351-28-59 "]}]"""))
    assert(DromTrucksParser.parseIsDealer(json4).toOption.value)
  }
}
