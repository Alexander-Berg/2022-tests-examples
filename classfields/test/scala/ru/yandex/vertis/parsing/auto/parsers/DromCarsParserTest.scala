package ru.yandex.vertis.parsing.auto.parsers

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, OptionValues}
import play.api.libs.json.Json
import ru.yandex.vertis.parsing.auto.parsers.webminer.cars.drom.DromCarsParser
import ru.yandex.vertis.parsing.parsers.ParsedValue

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class DromCarsParserTest extends FunSuite with OptionValues {
  test("offerId") {
    assert(DromCarsParser.offerId("https://abakan.drom.ru/acura/mdx/29531959.html") == "29531959")
  }

  test("parseEngineType") {
    assert(
      DromCarsParser.parseEngineType(
        Json.obj(
          "info" -> Json.arr(Json.obj("engine" -> Json.arr("гбо")).toString())
        )
      ) == ParsedValue.Expected("LPG")
    )

    assert(
      DromCarsParser.parseEngineType(
        Json.obj(
          "info" -> Json.arr(Json.obj("engine" -> Json.arr("гибрид")).toString())
        )
      ) == ParsedValue.Expected("HYBRID")
    )
  }

  test("parseRawEngineType") {
    assert(
      DromCarsParser
        .parseRawEngineType(
          Json.obj(
            "info" -> Json.arr(Json.obj("engine" -> Json.arr("бензин, 1.3 л")).toString())
          )
        )
        .toExpected == "бензин"
    )

    assert(
      DromCarsParser
        .parseRawEngineType(
          Json.obj(
            "info" -> Json.arr(Json.obj("engine" -> Json.arr("бензин, гбо, 1.3 л")).toString())
          )
        )
        .toExpected == "гбо"
    )

    assert(
      DromCarsParser
        .parseRawEngineType(
          Json.obj(
            "info" -> Json.arr(Json.obj("engine" -> Json.arr("бензин, гибрид, 1.3 л")).toString())
          )
        )
        .toExpected == "гибрид"
    )

    assert(
      DromCarsParser
        .parseRawEngineType(
          Json.obj(
            "info" -> Json.arr(Json.obj("engine" -> Json.arr("1.3 л")).toString())
          )
        )
        .isUnexpected
    )

    assert(
      DromCarsParser
        .parseRawEngineType(
          Json.obj(
            "info" -> Json.arr(Json.obj("engine" -> Json.arr("бензин")).toString())
          )
        )
        .toExpected == "бензин"
    )
  }

  test("parseRawDisplacement") {
    assert(
      DromCarsParser.parseRawDisplacement(
        Json.obj(
          "info" -> Json.arr(Json.obj("engine" -> Json.arr("бензин, 1.3 л")).toString())
        )
      ) == ParsedValue.Expected("1.3")
    )

    assert(
      DromCarsParser.parseRawDisplacement(
        Json.obj(
          "info" -> Json.arr(Json.obj("engine" -> Json.arr("бензин")).toString())
        )
      ) == ParsedValue.Ignored("бензин")
    )

    assert(
      DromCarsParser.parseRawDisplacement(
        Json.obj(
          "info" -> Json.arr(Json.obj("engine" -> Json.arr("pewpew")).toString())
        )
      ) == ParsedValue.Unexpected("pewpew")
    )
  }
}
