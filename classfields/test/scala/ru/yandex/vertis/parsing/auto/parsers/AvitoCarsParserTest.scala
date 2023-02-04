package ru.yandex.vertis.parsing.auto.parsers

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, OptionValues}
import play.api.libs.json.Json
import ru.yandex.vertis.parsing.auto.parsers.webminer.cars.avito.AvitoCarsParser
import ru.yandex.vertis.parsing.parsers.ParsedValue

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class AvitoCarsParserTest extends FunSuite with OptionValues {
  test("parsePrice") {
    assert(AvitoCarsParser.parsePrice(Json.obj("price" -> Json.arr("20 000 руб."))) == ParsedValue.Expected(20000))
    assert(AvitoCarsParser.parsePrice(Json.obj("price" -> Json.arr("Договорная"))) == ParsedValue.Ignored("Договорная"))
    assert(AvitoCarsParser.parsePrice(Json.obj("price" -> Json.arr("pewpew"))) == ParsedValue.Unexpected("pewpew"))
  }

  test("parse Zero Price") {
    assert(AvitoCarsParser.parsePrice(Json.obj("price" -> Json.arr("0"))) == ParsedValue.NoValue)
  }

  test("parse Negative Price") {
    assert(AvitoCarsParser.parsePrice(Json.obj("price" -> Json.arr("-500 долларов"))) == ParsedValue.Unexpected("-500"))
  }

  test("parseMileage") {
    assert(
      AvitoCarsParser.parseMileage(
        Json.obj(
          "info" -> Json.arr(Json.obj("mileage" -> Json.arr("Пробег 100 000 км")).toString())
        )
      ) == ParsedValue.Expected(100000)
    )

    assert(
      AvitoCarsParser.parseMileage(
        Json.obj(
          "info" -> Json.arr(Json.obj("mileage" -> Json.arr("Пробег не указан")).toString())
        )
      ) == ParsedValue.Ignored("пробег не указан")
    )
  }

  test("parseDisplacement") {
    val rawJson = Json.parse(
      "[{\"owner\":[\"{\\\"name\\\":[\\\"Автосалон \\\\\\\"Авто Галерея\\\\\\\"\\\"],\\\"info\\\":[\\\"автодилер\\\"]}\"],\"address\":[\"Екатеринбург, м. Машиностроителей\"],\"is_dealer\":[\"true\"],\"year\":[\"2011\"],\"fn\":[\"Volkswagen Polo\"],\"description\":[\"Автомобиль в отличном техническом состоянии и не требует никаких вложений! Оригинальный ПТС! Обслуживание у официального дилера! В комплектацию автомобиля входит: * Антиблокировочная система. * AIRBAG. * Хорошая аудиосистема. * Электростеклоподъемники. * Корректор фар. * Кондиционер. * Сигнализация с автозапуском и обратной связью. * Бортовой компьютер. * Стальные диски. Осмотреть и приобрести данный автомобиль вы можете ежедневно с 10.00 до 20.00 по адресу: г. Екатеринбург, ул. Фронтовых Бригад, 14 (5 этаж)! Возможен торг! Обмен на Ваше авто! Продажа в кредит!!! Обмен.\"],\"photo\":[\"https://24.img.avito.st/640x480/4464319524.jpg\",\"https://23.img.avito.st/640x480/4464319423.jpg\",\"https://20.img.avito.st/640x480/4464319420.jpg\",\"https://78.img.avito.st/640x480/4464319378.jpg\",\"https://62.img.avito.st/640x480/4464319462.jpg\",\"https://71.img.avito.st/640x480/4464319571.jpg\",\"https://43.img.avito.st/640x480/4464319643.jpg\",\"https://39.img.avito.st/640x480/4464319739.jpg\",\"https://37.img.avito.st/640x480/4464319837.jpg\",\"https://74.img.avito.st/640x480/4464319774.jpg\",\"https://04.img.avito.st/640x480/4464319704.jpg\",\"https://98.img.avito.st/640x480/4464319698.jpg\",\"https://66.img.avito.st/640x480/4464319866.jpg\",\"https://80.img.avito.st/640x480/4464319780.jpg\",\"https://64.img.avito.st/640x480/4464319764.jpg\"],\"shop_name\":[\"autogallerya\"],\"shop_id\":[\"120943\"],\"phone\":[\"\\\"+7 343 346-85-82\\\"\"],\"price\":[\"369 000 руб.\"],\"vin\":[\"WVWZZZ6R*CY****70\"],\"views\":[\"{\\\"all\\\":[\\\"91\\\"],\\\"today\\\":[\\\"28\\\"]}\"],\"info\":[\"{\\\"condition\\\":[\\\"Не битый\\\"],\\\"transmission\\\":[\\\"Механика\\\"],\\\"color\\\":[\\\"Серый цвет\\\"],\\\"engine\\\":[\\\"1.2\\\"],\\\"wheel-drive\\\":[\\\"Передний привод\\\"],\\\"fuel\\\":[\\\"Бензин\\\"],\\\"wheel\\\":[\\\"Левый руль\\\"],\\\"car-type\\\":[\\\"С пробегом\\\"],\\\"power\\\":[\\\"69 л.с.\\\"],\\\"body\\\":[\\\"Хетчбэк\\\"],\\\"mileage\\\":[\\\"Пробег 103 000 км\\\"]}\"],\"parse_date\":[\"2018-06-07T00:27:05.052+03:00\"],\"date-published\":[\"2018-05-21\"]}]"
    )
    val json = WebminerJsonUtils.parseJson(rawJson)
    val displacement = AvitoCarsParser.parseDisplacement(json)
    assert(displacement == ParsedValue.Expected(1200))
  }

  test("parseOwnersCount") {
    assert(
      AvitoCarsParser.parseOwnersCount(
        Json.obj(
          "info" -> Json.arr(Json.obj("owners-count" -> Json.arr("1")).toString())
        )
      ) == ParsedValue.Expected(1)
    )
    assert(
      AvitoCarsParser.parseOwnersCount(
        Json.obj(
          "info" -> Json.arr(Json.obj("owners-count" -> Json.arr("2")).toString())
        )
      ) == ParsedValue.Expected(2)
    )
    assert(
      AvitoCarsParser.parseOwnersCount(
        Json.obj(
          "info" -> Json.arr(Json.obj("owners-count" -> Json.arr("3")).toString())
        )
      ) == ParsedValue.Expected(3)
    )
    assert(
      AvitoCarsParser.parseOwnersCount(
        Json.obj(
          "info" -> Json.arr(Json.obj("owners-count" -> Json.arr("4+")).toString())
        )
      ) == ParsedValue.Expected(3)
    )
    assert(
      AvitoCarsParser.parseOwnersCount(
        Json.obj(
          "info" -> Json.arr(Json.obj("owners-count" -> Json.arr("abc")).toString())
        )
      ) == ParsedValue.Unexpected("abc")
    )
  }
}
