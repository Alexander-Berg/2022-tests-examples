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
                               |  "owner" : [ "{\"name\":[\"\\n\\t\\tООО \\\"УАЗ Центр\\\"\\t\"],\"id\":[\"1204252\"],\"login\":[\"Uazcentr\"],\"email\":[\"uaz.centr@mail.ru\"]}" ],
                               |  "address" : [ "Новосибирск" ],
                               |  "year" : [ "2016" ],
                               |  "phone" : [ "\n\t\t\t\t\t+7 (383) 233-60-60\t\t\t" ],
                               |  "price" : [ "599 000" ],
                               |  "fn" : [ "УАЗ 3303" ],
                               |  "photo" : [ "https://static.baza.farpost.ru/v/1467974862685_bulletin", "https://static.baza.farpost.ru/v/1467974862951_bulletin", "https://static.baza.farpost.ru/v/1467974864836_bulletin", "https://static.baza.farpost.ru/v/1467974866656_bulletin", "https://static.baza.farpost.ru/v/1467974869382_bulletin", "https://static.baza.farpost.ru/v/1467974871309_bulletin", "https://static.baza.farpost.ru/v/1467974873242_bulletin" ],
                               |  "description" : [ "УАЗ БОРТОВОЙ (3303). Проверенный временем и дорогами грузовой полноприводный УАЗ 3303 без проблем доставит Ваш груз по любым видам дорожных покрытий и бездорожью. Грузовой и полноприводный УАЗ 3303 порадует Вас своей маневренностью, неприхотливостью и простотой обслуживания. Цельнометаллическая двухместная кабина с двумя боковыми одностворчатыми дверями и съемной крышкой капота двигателя, металлическая или деревянная платформа и безупречная проходимость — что еще нужно для того, чтобы с комфортом доставлять любой груз? Колесная формула 4х4. Количество мест 2. Длина, мм 4501. Ширина, мм 1974. Высота, мм 2355. Колесная база, мм 2550. Дорожный просвет, мм 205. Глубина преодолеваемого брода, мм 500. Масса снаряженного а/м, кг 1845. Полная масса, кг 3070. Грузоподъёмность, кг 1225. Двигатель Бензиновый, ЗМЗ-40911.10. Топливо Бензин с октановым числом не менее 92. Рабочий объем, л 2,693. Максимальная мощность, л.с. (кВт) 112 (82,5) при 4250 об/мин. Максимальный крутящий момент, Н·м 198 при 2500 об/мин. Максимальная скорость, км/ч 115. Расход топлива при 60 км/ч, л / 100 км 9,6. Расход топлива при 80 км/ч, л / 100 км 12,4. Емкость топливных баков, л 50. Коробка передач 5-ступенчатая, механическая. Раздаточная коробка 2-ступенчатая с отключением привода переднего моста. Тормозная система Двухконтурная, с вакуумным усилителем, передняя дисковая, задняя барабанная. Шины 225/75 R16. Тентованная платформа. Гидроусилитель руля." ],
                               |  "offer_id" : [ "45147730" ],
                               |  "info" : [ "{\"transmission\":[\"Механическая\"],\"engine\":[\"2 693 куб. см.\"],\"wheel-drive\":[\"4x4\"],\"mileage_in_russia\":[\"Без пробега\"],\"documents\":[\"Есть ПТС\"],\"wheel\":[\"Левый\"],\"fuel\":[\"Бензин\"],\"state\":[\"Новое\"],\"type\":[\"Бортовой грузовик\"],\"category\":[\"Грузовики и спецтехника\"],\"capacity\":[\"1 225 кг.\"]}" ],
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
                               |  "owner" : [ "{\"name\":[\"\\n\\t\\tООО \\\"УАЗ Центр\\\"\\t\"],\"id\":[\"1204252\"],\"login\":[\"Uazcentr\"],\"email\":[\"uaz.centr@mail.ru\"]}" ],
                               |  "address" : [ "Новосибирск" ],
                               |  "year" : [ "2016" ],
                               |  "phone" : [ "\n\t\t\t\t\t+7 (383) 233-60-60\t\t\t" ],
                               |  "price" : [ "599 000" ],
                               |  "fn" : [ "УАЗ 3303" ],
                               |  "photo" : [ "https://static.baza.farpost.ru/v/1467974862685_bulletin", "https://static.baza.farpost.ru/v/1467974862951_bulletin", "https://static.baza.farpost.ru/v/1467974864836_bulletin", "https://static.baza.farpost.ru/v/1467974866656_bulletin", "https://static.baza.farpost.ru/v/1467974869382_bulletin", "https://static.baza.farpost.ru/v/1467974871309_bulletin", "https://static.baza.farpost.ru/v/1467974873242_bulletin" ],
                               |  "description" : [ "УАЗ БОРТОВОЙ (3303). Проверенный временем и дорогами грузовой полноприводный УАЗ 3303 без проблем доставит Ваш груз по любым видам дорожных покрытий и бездорожью. Грузовой и полноприводный УАЗ 3303 порадует Вас своей маневренностью, неприхотливостью и простотой обслуживания. Цельнометаллическая двухместная кабина с двумя боковыми одностворчатыми дверями и съемной крышкой капота двигателя, металлическая или деревянная платформа и безупречная проходимость — что еще нужно для того, чтобы с комфортом доставлять любой груз? Колесная формула 4х4. Количество мест 2. Длина, мм 4501. Ширина, мм 1974. Высота, мм 2355. Колесная база, мм 2550. Дорожный просвет, мм 205. Глубина преодолеваемого брода, мм 500. Масса снаряженного а/м, кг 1845. Полная масса, кг 3070. Грузоподъёмность, кг 1225. Двигатель Бензиновый, ЗМЗ-40911.10. Топливо Бензин с октановым числом не менее 92. Рабочий объем, л 2,693. Максимальная мощность, л.с. (кВт) 112 (82,5) при 4250 об/мин. Максимальный крутящий момент, Н·м 198 при 2500 об/мин. Максимальная скорость, км/ч 115. Расход топлива при 60 км/ч, л / 100 км 9,6. Расход топлива при 80 км/ч, л / 100 км 12,4. Емкость топливных баков, л 50. Коробка передач 5-ступенчатая, механическая. Раздаточная коробка 2-ступенчатая с отключением привода переднего моста. Тормозная система Двухконтурная, с вакуумным усилителем, передняя дисковая, задняя барабанная. Шины 225/75 R16. Тентованная платформа. Гидроусилитель руля." ],
                               |  "offer_id" : [ "45147730" ],
                               |  "info" : [ "{\"transmission\":[\"Механическая\"],\"engine\":[\"2 693 куб. см.\"],\"wheel-drive\":[\"4x4\"],\"mileage_in_russia\":[\"Без пробега\"],\"documents\":[\"Есть ПТС\"],\"wheel\":[\"Левый\"],\"fuel\":[\"Бензин\"],\"state\":[\"Новое\"],\"type\":[\"Бортовой грузовик\"],\"category\":[\"Грузовики и спецтехника\"],\"capacity\":[\"1 225 кг.\"]}" ],
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
