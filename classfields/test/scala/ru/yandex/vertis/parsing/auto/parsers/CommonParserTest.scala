package ru.yandex.vertis.parsing.auto.parsers

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FunSuite, OptionValues}
import play.api.libs.json.Json
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.parsing.auto.parsers.webminer.cars.avito.AvitoCarsParser
import ru.yandex.vertis.parsing.auto.parsers.webminer.cars.drom.DromCarsParser
import ru.yandex.vertis.parsing.auto.parsers.webminer.trucks.avito.AvitoTrucksParser
import ru.yandex.vertis.parsing.auto.parsers.webminer.trucks.drom.DromTrucksParser
import ru.yandex.vertis.parsing.common.Site
import WebminerJsonUtils.parseJson

/**
  * Created by andrey on 1/9/18.
  */
@RunWith(classOf[JUnitRunner])
class CommonParserTest extends FunSuite with OptionValues {
  test("hash from avito url from haraba") {
    val url = "https://www.avito.ru/2054774770"
    assert(CommonAutoParser.forUrl(url).site == Site.Avito)
    assert(CommonAutoParser.hash(url) == "d7c0c24f32495978e6c052b7125a7495")
    assert(CommonAutoParser.remoteId(url) == "avito|cars|2054774770")
  }

  test("hash from am.ru url") {
    val url = "https://auto.youla.ru/advert/used/toyota/land_cruiser/prv--46eca2d65f56b548"
    assert(CommonAutoParser.forUrl(url).site == Site.Amru)
    assert(CommonAutoParser.hash(url) == "e076190c5cf95ae8a7749febb985e11a")
    assert(CommonAutoParser.remoteId(url) == "am.ru|cars|46eca2d65f56b548")
  }

  test("fromUrl") {
    val avitoTrucksUrl: String = "https://m.avito.ru/zlatoust/gruzoviki_i_spetstehnika/zil_bychok_1181292729"
    assert(CommonAutoParser.forUrl(Category.TRUCKS, avitoTrucksUrl) == AvitoTrucksParser)

    val avitoCarsUrl: String = "https://m.avito.ru/abadzehskaya/avtomobili/gaz_3110_volga_2002_1201795634"
    assert(CommonAutoParser.forUrl(Category.CARS, avitoCarsUrl) == AvitoCarsParser)

    val dromTrucksUrl: String = "https://spec.drom.ru/urai/truck/prodam-gazel-termobudka-59940174.html"
    assert(CommonAutoParser.forUrl(Category.TRUCKS, dromTrucksUrl) == DromTrucksParser)

    val dromCarsUrl: String = "https://abakan.drom.ru/acura/mdx/29531959.html"
    assert(CommonAutoParser.forUrl(Category.CARS, dromCarsUrl) == DromCarsParser)

    intercept[RuntimeException](CommonAutoParser.forUrl(Category.TRUCKS, "yandex.ru"))
    intercept[RuntimeException](CommonAutoParser.forUrl(Category.CARS, "yandex.ru"))
  }

  test("fromPhotoUrl") {
    val avitoTrucksPhotoUrl: String = "https://74.img.avito.st/640x480/4135850774.jpg"
    assert(CommonAutoParser.forPhotoUrl(Category.TRUCKS, avitoTrucksPhotoUrl) == AvitoTrucksParser)

    val avitoCarsPhotoUrl: String = "https://35.img.avito.st/640x480/4408251435.jpg"
    assert(CommonAutoParser.forPhotoUrl(Category.CARS, avitoCarsPhotoUrl) == AvitoCarsParser)

    val dromTrucksPhotoUrl: String = "https://static.baza.farpost.ru/v/1515394178903_bulletin"
    assert(CommonAutoParser.forPhotoUrl(Category.TRUCKS, dromTrucksPhotoUrl) == DromTrucksParser)

    val dromCarsPhotoUrl: String = "https://s.auto.drom.ru/i24221/s/photos/29853/29852756/gen1200_290284664.jpg"
    assert(CommonAutoParser.forPhotoUrl(Category.CARS, dromCarsPhotoUrl) == DromCarsParser)

    intercept[RuntimeException](CommonAutoParser.forPhotoUrl(Category.TRUCKS, "yandex.ru"))
    intercept[RuntimeException](CommonAutoParser.forPhotoUrl(Category.CARS, "yandex.ru"))
  }

  test("parsePhones") {
    val rawJson = Json.parse(
      """[ {
                               |  "owner" : [ "{\"name\":[\"\\n\\t\\t?????? \\\"?????? ??????????\\\"\\t\"],\"id\":[\"1204252\"],\"login\":[\"Uazcentr\"],\"email\":[\"uaz.centr@mail.ru\"]}" ],
                               |  "address" : [ "??????????????????????" ],
                               |  "year" : [ "2016" ],
                               |  "phone" : [ "\n\t\t\t\t\t+7 (383) 233-60-60\t\t\t" ],
                               |  "price" : [ "599 000" ],
                               |  "fn" : [ "?????? 3303" ],
                               |  "photo" : [ "https://static.baza.farpost.ru/v/1467974862685_bulletin", "https://static.baza.farpost.ru/v/1467974862951_bulletin", "https://static.baza.farpost.ru/v/1467974864836_bulletin", "https://static.baza.farpost.ru/v/1467974866656_bulletin", "https://static.baza.farpost.ru/v/1467974869382_bulletin", "https://static.baza.farpost.ru/v/1467974871309_bulletin", "https://static.baza.farpost.ru/v/1467974873242_bulletin" ],
                               |  "description" : [ "?????? ???????????????? (3303). ?????????????????????? ???????????????? ?? ???????????????? ???????????????? ???????????????????????????? ?????? 3303 ?????? ?????????????? ???????????????? ?????? ???????? ???? ?????????? ?????????? ???????????????? ???????????????? ?? ????????????????????. ???????????????? ?? ???????????????????????????? ?????? 3303 ???????????????? ?????? ?????????? ????????????????????????????, ???????????????????????????????? ?? ?????????????????? ????????????????????????. ?????????????????????????????????????? ?????????????????????? ???????????? ?? ?????????? ???????????????? ?????????????????????????????? ?????????????? ?? ?????????????? ?????????????? ???????????? ??????????????????, ?????????????????????????? ?????? ???????????????????? ?????????????????? ?? ?????????????????????? ???????????????????????? ??? ?????? ?????? ?????????? ?????? ????????, ?????????? ?? ?????????????????? ???????????????????? ?????????? ????????? ???????????????? ?????????????? 4??4. ???????????????????? ???????? 2. ??????????, ???? 4501. ????????????, ???? 1974. ????????????, ???? 2355. ???????????????? ????????, ???? 2550. ???????????????? ??????????????, ???? 205. ?????????????? ?????????????????????????????? ??????????, ???? 500. ?????????? ???????????????????????? ??/??, ???? 1845. ???????????? ??????????, ???? 3070. ????????????????????????????????, ???? 1225. ?????????????????? ????????????????????, ??????-40911.10. ?????????????? ???????????? ?? ?????????????????? ???????????? ???? ?????????? 92. ?????????????? ??????????, ?? 2,693. ???????????????????????? ????????????????, ??.??. (??????) 112 (82,5) ?????? 4250 ????/??????. ???????????????????????? ???????????????? ????????????, ?????? 198 ?????? 2500 ????/??????. ???????????????????????? ????????????????, ????/?? 115. ???????????? ?????????????? ?????? 60 ????/??, ?? / 100 ???? 9,6. ???????????? ?????????????? ?????? 80 ????/??, ?? / 100 ???? 12,4. ?????????????? ?????????????????? ??????????, ?? 50. ?????????????? ?????????????? 5-??????????????????????, ????????????????????????. ?????????????????????? ?????????????? 2-?????????????????????? ?? ?????????????????????? ?????????????? ?????????????????? ??????????. ?????????????????? ?????????????? ??????????????????????????, ?? ?????????????????? ????????????????????, ???????????????? ????????????????, ???????????? ????????????????????. ???????? 225/75 R16. ?????????????????????? ??????????????????. ???????????????????????????? ????????." ],
                               |  "offer_id" : [ "45147730" ],
                               |  "info" : [ "{\"transmission\":[\"????????????????????????\"],\"engine\":[\"2 693 ??????. ????.\"],\"wheel-drive\":[\"4x4\"],\"mileage_in_russia\":[\"?????? ??????????????\"],\"documents\":[\"???????? ??????\"],\"wheel\":[\"??????????\"],\"fuel\":[\"????????????\"],\"state\":[\"??????????\"],\"type\":[\"???????????????? ????????????????\"],\"category\":[\"?????????????????? ?? ??????????????????????\"],\"capacity\":[\"1 225 ????.\"]}" ],
                               |  "parse_date" : [ "2018-02-13T19:36:29.815+03:00" ]
                               |} ]""".stripMargin
    )
    val json = parseJson(rawJson)
    val phones = DromTrucksParser.parsePhones(json)
    assert(phones.isExpected)
    assert(phones.toExpected.head.toExpected == "73832336060")
  }

  test("parseParseDate") {
    val rawJson = Json.parse(
      """[ {
                               |  "owner" : [ "{\"name\":[\"\\n\\t\\t?????? \\\"?????? ??????????\\\"\\t\"],\"id\":[\"1204252\"],\"login\":[\"Uazcentr\"],\"email\":[\"uaz.centr@mail.ru\"]}" ],
                               |  "address" : [ "??????????????????????" ],
                               |  "year" : [ "2016" ],
                               |  "phone" : [ "\n\t\t\t\t\t+7 (383) 233-60-60\t\t\t" ],
                               |  "price" : [ "599 000" ],
                               |  "fn" : [ "?????? 3303" ],
                               |  "photo" : [ "https://static.baza.farpost.ru/v/1467974862685_bulletin", "https://static.baza.farpost.ru/v/1467974862951_bulletin", "https://static.baza.farpost.ru/v/1467974864836_bulletin", "https://static.baza.farpost.ru/v/1467974866656_bulletin", "https://static.baza.farpost.ru/v/1467974869382_bulletin", "https://static.baza.farpost.ru/v/1467974871309_bulletin", "https://static.baza.farpost.ru/v/1467974873242_bulletin" ],
                               |  "description" : [ "?????? ???????????????? (3303). ?????????????????????? ???????????????? ?? ???????????????? ???????????????? ???????????????????????????? ?????? 3303 ?????? ?????????????? ???????????????? ?????? ???????? ???? ?????????? ?????????? ???????????????? ???????????????? ?? ????????????????????. ???????????????? ?? ???????????????????????????? ?????? 3303 ???????????????? ?????? ?????????? ????????????????????????????, ???????????????????????????????? ?? ?????????????????? ????????????????????????. ?????????????????????????????????????? ?????????????????????? ???????????? ?? ?????????? ???????????????? ?????????????????????????????? ?????????????? ?? ?????????????? ?????????????? ???????????? ??????????????????, ?????????????????????????? ?????? ???????????????????? ?????????????????? ?? ?????????????????????? ???????????????????????? ??? ?????? ?????? ?????????? ?????? ????????, ?????????? ?? ?????????????????? ???????????????????? ?????????? ????????? ???????????????? ?????????????? 4??4. ???????????????????? ???????? 2. ??????????, ???? 4501. ????????????, ???? 1974. ????????????, ???? 2355. ???????????????? ????????, ???? 2550. ???????????????? ??????????????, ???? 205. ?????????????? ?????????????????????????????? ??????????, ???? 500. ?????????? ???????????????????????? ??/??, ???? 1845. ???????????? ??????????, ???? 3070. ????????????????????????????????, ???? 1225. ?????????????????? ????????????????????, ??????-40911.10. ?????????????? ???????????? ?? ?????????????????? ???????????? ???? ?????????? 92. ?????????????? ??????????, ?? 2,693. ???????????????????????? ????????????????, ??.??. (??????) 112 (82,5) ?????? 4250 ????/??????. ???????????????????????? ???????????????? ????????????, ?????? 198 ?????? 2500 ????/??????. ???????????????????????? ????????????????, ????/?? 115. ???????????? ?????????????? ?????? 60 ????/??, ?? / 100 ???? 9,6. ???????????? ?????????????? ?????? 80 ????/??, ?? / 100 ???? 12,4. ?????????????? ?????????????????? ??????????, ?? 50. ?????????????? ?????????????? 5-??????????????????????, ????????????????????????. ?????????????????????? ?????????????? 2-?????????????????????? ?? ?????????????????????? ?????????????? ?????????????????? ??????????. ?????????????????? ?????????????? ??????????????????????????, ?? ?????????????????? ????????????????????, ???????????????? ????????????????, ???????????? ????????????????????. ???????? 225/75 R16. ?????????????????????? ??????????????????. ???????????????????????????? ????????." ],
                               |  "offer_id" : [ "45147730" ],
                               |  "info" : [ "{\"transmission\":[\"????????????????????????\"],\"engine\":[\"2 693 ??????. ????.\"],\"wheel-drive\":[\"4x4\"],\"mileage_in_russia\":[\"?????? ??????????????\"],\"documents\":[\"???????? ??????\"],\"wheel\":[\"??????????\"],\"fuel\":[\"????????????\"],\"state\":[\"??????????\"],\"type\":[\"???????????????? ????????????????\"],\"category\":[\"?????????????????? ?? ??????????????????????\"],\"capacity\":[\"1 225 ????.\"]}" ],
                               |  "parse_date" : [ "2018-02-13T19:36:29.815+03:00" ]
                               |} ]""".stripMargin
    )
    val json = parseJson(rawJson)
    val parseDate = DromTrucksParser.parseParseDate(json).toOption.value
    assert(parseDate.getYear == 2018)
    assert(parseDate.getMonthOfYear == 2)
    assert(parseDate.getDayOfMonth == 13)
  }
}
