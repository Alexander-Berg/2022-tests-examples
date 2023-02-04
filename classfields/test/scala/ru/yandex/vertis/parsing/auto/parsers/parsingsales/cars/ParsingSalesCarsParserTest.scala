package ru.yandex.vertis.parsing.auto.parsers.parsingsales.cars

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import play.api.libs.json.Json
import ru.auto.api.CommonModel
import ru.yandex.vertis.parsing.auto.parsers.parsingsales.cars.avito.ParsingSalesAvitoCarsParser
import ru.yandex.vertis.parsing.parsers.ParsedValue
import ru.yandex.vertis.parsing.auto.util.TestDataUtils.testAvitoCarsUrl

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class ParsingSalesCarsParserTest extends FunSuite {
  test("parseRawBodyType") {
    val parser = ParsingSalesAvitoCarsParser
    val in = this.getClass.getResourceAsStream("/parsing_sales_body_types.txt")
    val bodyTypes = scala.io.Source.fromInputStream(in).getLines().toList.map(_.split("\\|"))
    bodyTypes.foreach(bodyType => {
      assert(parser.parseRawBodyType(Json.obj("bodyType" -> bodyType.head)).toExpected == bodyType(1))
    })
  }

  test("parseRawDoorsCount") {
    val parser = ParsingSalesAvitoCarsParser
    val in = this.getClass.getResourceAsStream("/parsing_sales_doors.txt")
    val bodyTypes = scala.io.Source.fromInputStream(in).getLines().toList.map(_.split("\\|"))
    bodyTypes.foreach(bodyType => {
      assert(parser.parseRawDoorsCount(Json.obj("bodyType" -> bodyType.head)).toString == bodyType(1))
    })
  }

  test("parseSteeringWheel") {
    val parser = ParsingSalesAvitoCarsParser
    assert(
      parser.parseSteeringWheel(Json.obj("wheel" -> "левый", "color" -> "черный")) ==
        ParsedValue.Expected(CommonModel.SteeringWheel.LEFT)
    )
  }

  test("year") {
    val parser = ParsingSalesAvitoCarsParser
    assert(parser.parseYear(testAvitoCarsUrl, Json.obj("year" -> "")) == ParsedValue.NoValue)
    assert(parser.parseYear(testAvitoCarsUrl, Json.obj("year" -> " ")) == ParsedValue.NoValue)
    assert(parser.parseYear(testAvitoCarsUrl, Json.obj("year" -> "0")) == ParsedValue.NoValue)
    assert(parser.parseYear(testAvitoCarsUrl, Json.obj("year" -> " 1996")) == ParsedValue.Expected(1996))

    assert(parser.parseRawYear(testAvitoCarsUrl, Json.obj("year" -> "")) == ParsedValue.NoValue)
    assert(parser.parseRawYear(testAvitoCarsUrl, Json.obj("year" -> " ")) == ParsedValue.NoValue)
    assert(parser.parseRawYear(testAvitoCarsUrl, Json.obj("year" -> "0")) == ParsedValue.NoValue)
    assert(parser.parseRawYear(testAvitoCarsUrl, Json.obj("year" -> " 1996")) == ParsedValue.Expected("1996"))
  }
}
