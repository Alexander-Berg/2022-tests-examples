package ru.yandex.vertis.parsing.auto.parsers

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, OptionValues}
import play.api.libs.json.Json
import ru.yandex.vertis.parsing.auto.parsers.webminer.trucks.avito.AvitoTrucksParser

/**
  * Created by andrey on 1/9/18.
  */
@RunWith(classOf[JUnitRunner])
class NewAvitoTrucksParserTest extends FunSuite with OptionValues {
  test("parse") {
    val url = "https://m.avito.ru/zlatoust/gruzoviki_i_spetstehnika/zil_bychok_1181292729"
    val rawJson =
      """[{
        |"owner":["{\"name\":[\"Ольга\"]}"],
        |"address":["Челябинская область, Златоуст"],
        |"is_dealer":["false"],
        |"phone":["\"+7 950 745-45-93\""],
        |"price":["100 000 руб."],
        |"fn":["ЗИЛ Бычок"],
        |"description":["2001 г.На полном ходу.Фургон 20 куб.рама безномерная(по докум). Двигатель собран в чехии.Все навесное чешское(турбина,ТНВД,стартер и Т/Д)поставлен 3года назад.пробег 70000 тыс. Масло от замены до замены. Расход д/т.Груженный 8-10 литр.Порожний 6-7 литр.Очень экономичный.Реально. Старый двиг кушал 20-25 литр. Бак 250 литр. Резина хорошая,износ 20%. Требует сварочных работ(двери,крылья,пороги). Доедет в любую точку России. Разбирать не буду."],
        |"photo":["https://52.img.avito.st/640x480/4101525452.jpg"],
        |"views":["{\"all\":[\"17\"],\"today\":[\"17\"]}"],
        |"info":["{\"type\":[\"Грузовики\"]}"],
        |"date-published":["2018-01-02"],
        |"parse_date":["2018-01-02T15:35:34.980+03:00"]}]""".stripMargin
    assert(AvitoTrucksParser.hash(url) == "c0217ded8dee79e5963d335d8f0440ef")
    assert(AvitoTrucksParser.offerId(url) == "1181292729")
    val json = WebminerJsonUtils.parseJson(Json.parse(rawJson))
    assert(!AvitoTrucksParser.parseIsDealer(json).toOption.value)
  }

  test("offerId") {
    val url1: String =
      "https://m.avito.ru/shahty/gruzoviki_i_spetstehnika/kamaz_4310_vezdehod_1199799263?slocation=651110"
    assert(AvitoTrucksParser.offerId(url1) == "1199799263")
    val url2: String =
      "https://m.avito.ru/shahty/gruzoviki_i_spetstehnika/kamaz_4310_vezdehod_1199799263"
    assert(AvitoTrucksParser.offerId(url2) == "1199799263")
  }
}
