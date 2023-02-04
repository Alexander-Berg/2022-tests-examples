package ru.yandex.vertis.parsing.auto.parsers

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import play.api.libs.json.Json
import ru.yandex.vertis.parsing.parsers.ParsedValue
import ru.yandex.vertis.parsing.util.StringUtils.safeIntParse
import WebminerJsonUtils.{expectSingleFloatNumber, expectSingleWholeNumber}

@RunWith(classOf[JUnitRunner])
class WebminerJsonUtilsTest extends FunSuite {
  test("parseSingleFloatNumber") {
    assert(expectSingleFloatNumber(Json.obj("field" -> Json.arr("1.2")), "field") == ParsedValue.Expected("1.2"))
    assert(
      expectSingleFloatNumber(Json.obj("field" -> Json.arr("1.3 л, бензин")), "field") == ParsedValue.Expected("1.3")
    )
  }

  test("parseSingleIntNumber") {
    assert(
      expectSingleWholeNumber(Json.obj("field" -> Json.arr("150 000 км")), "field") == ParsedValue.Expected("150000")
    )
    assert(
      expectSingleWholeNumber(Json.obj("field" -> Json.arr("180 000 руб")), "field") == ParsedValue.Expected("180000")
    )
    assert(
      expectSingleWholeNumber(Json.obj("field" -> Json.arr("2 350 000")), "field") == ParsedValue.Expected("2350000")
    )
    assert(expectSingleWholeNumber(Json.obj("field" -> Json.arr("2005")), "field") == ParsedValue.Expected("2005"))
    assert(expectSingleWholeNumber(Json.obj("field" -> Json.arr("66 л.с")), "field") == ParsedValue.Expected("66"))
    assert(
      expectSingleWholeNumber(Json.obj("field" -> Json.arr("10 850 куб. см.")), "field") == ParsedValue
        .Expected("10850")
    )
    assert(expectSingleWholeNumber(Json.obj("field" -> Json.arr("8")), "field") == ParsedValue.Expected("8"))

    assert(
      expectSingleWholeNumber(Json.obj("field" -> Json.arr("pewpew")), "field") == ParsedValue.Unexpected("pewpew")
    )
    assert(
      expectSingleWholeNumber(Json.obj("field" -> Json.arr("8")), "field").flatMap(safeIntParse) == ParsedValue
        .Expected(8)
    )
    assert(
      expectSingleWholeNumber(Json.obj("field" -> Json.arr("pewpew")), "field").flatMap(safeIntParse) == ParsedValue
        .Unexpected("pewpew")
    )
    assert(expectSingleWholeNumber(Json.obj("field" -> Json.arr("pewpew")), "field2").isNoValue)
  }
}
