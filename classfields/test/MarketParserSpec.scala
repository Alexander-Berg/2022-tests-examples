package ru.yandex.vertis.general.feed.parser.market.test

import java.io.File
import ru.yandex.vertis.general.feed.parser.market.MarketParser
import ru.yandex.vertis.general.feed.parser.market.MarketParser.CollectedShopMeta
import ru.yandex.vertis.general.feed.parser.testkit.XmlParserTestkit._
import zio.test.Assertion._
import zio.test._

object MarketParserSpec extends DefaultRunnableSpec {

  def spec =
    suite("MarketParser")(
      test("Парсинг пустого файла") {
        MarketParser
          .getParser[String](CollectedShopMeta(categories = Map.empty, platformOpt = None, delivery = None))
          .assertParsing("")(
            isParsingError(isInvalidXml("Premature end of file.") && withLocation(1, 1))
          )
      },
      testM("Примеры из документации") {
        MarketParser
          .getParser[File](CollectedShopMeta(categories = Map.empty, platformOpt = None, delivery = None))
          .checkSamples(
            "general/feed/parser/market/samples/simple.xml",
            "general/feed/parser/market/samples/first_name_format.xml",
            "general/feed/parser/market/samples/second_name_format.xml",
            "general/feed/parser/market/samples/condition_used.xml",
            "general/feed/parser/market/samples/condition_likenew.xml",
            "general/feed/parser/market/samples/pictures.xml"
          )
      },
      testM("Берем имя из простого формата, если есть имя в простом формате и часть в составном") {
        MarketParser
          .getParser[File](CollectedShopMeta(categories = Map.empty, platformOpt = None, delivery = None))
          .checkSamples(
            "general/feed/parser/market/samples/mixed_name_format.xml"
          )
      },
      testM("Падаем с ошибкой, если в currencyId передан не RUR и не RUB") {
        MarketParser
          .getParser[File](CollectedShopMeta(categories = Map.empty, platformOpt = None, delivery = None))
          .checkSamples(
            "general/feed/parser/market/samples/errors/currency.xml"
          )
      },
      testM("Округляем дробную цену в большую сторону") {
        MarketParser
          .getParser[File](CollectedShopMeta(categories = Map.empty, platformOpt = None, delivery = None))
          .checkSamples(
            "general/feed/parser/market/samples/fractional_price.xml"
          )
      },
      test("Учитываем категории при парсинге") {
        val result = MarketParser
          .parse[String](s"""<?xml version="1.0" encoding="UTF-8"?>
                                      |<yml_catalog date="2020-11-22T14:37:38+03:00">
                                      |    <shop>
                                      |        <categories>
                                      |            <category id="1">Бытовая техника</category>
                                      |            <category id="10" parentId="1">Мелкая техника для кухни</category>
                                      |            <category id="20" parentId="1">Средняя техника для кухни</category>
                                      |            <category id="30" parentId="1">Крупная техника для кухни</category>
                                      |            <category id="150" parentId="10">Мороженицы</category>
                                      |        </categories>
                                      |        <offers>
                                      |            <offer id="9012">
                                      |                <name>Мороженица Brand 3811</name>
                                      |                <url>http://best.seller.ru/product_page.asp?pid=12345</url>
                                      |                <price>8990</price>
                                      |                <currencyId>RUB</currencyId>
                                      |                <categoryId>150</categoryId>
                                      |                <condition type="likenew">
                                      |                    <reason>Мятая коробка, царапина на корпусе.</reason>
                                      |                </condition>
                                      |                <param name="Цвет">белый</param>
                                      |                <weight>3.6</weight>
                                      |                <dimensions>20.1/20.551/22.5</dimensions>
                                      |            </offer>
                                      |        </offers>
                                      |    </shop>
                                      |</yml_catalog>""".stripMargin)
          .toList
          .collect { case Right(offer) =>
            offer
          }
        assert(result)(hasSize(equalTo(1))) &&
        assert(result.head.category.map(_.parentCategories))(
          isSome(
            equalTo(
              List(
                "Мелкая техника для кухни",
                "Бытовая техника"
              )
            )
          )
        ) &&
        assert(result.head.category.map(_.category))(isSome(equalTo("Мороженицы")))
      },
      test("Спарсился platform") {
        val platformUnexpected = "UNEXPECTED"
        val platformExpected = "BSM/Yandex/Market"
        val result = MarketParser
          .parse[String](s"""<?xml version="1.0" encoding="UTF-8"?>
               |<yml_catalog date="2020-11-22T14:37:38+03:00">
               |  <shop>
               |    <platform>$platformExpected</platform>
               |    <categories>
               |      <category id="1">Бытовая техника</category>
               |      <category id="10" parentId="1">Мелкая техника для кухни</category>
               |    </categories>
               |    <offers>
               |      <offer id="9012">
               |        <name>Мороженица Brand 3811</name>
               |        <url>http://best.seller.ru/product_page.asp?pid=12345</url>
               |        <price>8990</price>
               |        <currencyId>RUB</currencyId>
               |        <categoryId>10</categoryId>
               |        <condition type="likenew">
               |          <reason>Мятая коробка, царапина на корпусе.</reason>
               |        </condition>
               |        <param name="Цвет">белый</param>
               |        <weight>3.6</weight>
               |        <dimensions>20.1/20.551/22.5</dimensions>
               |      </offer>
               |    </offers>
               |  </shop>
               |</yml_catalog>""".stripMargin)
          .toList
          .collect { case Right(offer) => offer }
        assert(result)(hasSize(equalTo(1))) &&
        assert(result.head.platform.getOrElse(platformUnexpected))(equalTo(platformExpected))
      }
    )
}
