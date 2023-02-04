package ru.yandex.vertis.general.feed.parser.vacancy_uni_search.test

import ru.yandex.vertis.general.feed.parser.vacancy_uni_search.VacancyUSParser
import ru.yandex.vertis.general.feed.parser.vacancy_uni_search.VacancyUSParser.CollectedShopMeta
import ru.yandex.vertis.general.feed.parser.testkit.XmlParserTestkit._
import zio.test.Assertion._
import zio.test._

import java.io.File

object VacancyUSParserSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("VacancyUSParser")(
      test("Парсинг пустого файла") {
        VacancyUSParser.getParser
          .assertParsing("")(
            isParsingError(isInvalidXml("Premature end of file.") && withLocation(1, 1))
          )
      },
      testM("Прогонка файлов примеров") {
        VacancyUSParser.getParser
          .checkSamples(
            "general/feed/parser/vacancy_uni_search/samples/simple.xml"
          )
      },
      test("Учитываем категории при парсинге") {
        val result = VacancyUSParser
          .parse[String](s"""<?xml version="1.0" encoding="utf-8" standalone="yes"?>
                            |<yml_catalog date="2018-07-17 12:10">
                            |  <shop>
                            |    <name>Сэмпл.Работа</name>
                            |    <company>ООО Сэмпл.Работа</company>
                            |    <url>rabota.sample.s3.yandex.net</url>
                            |    <email>rabota@yandex.ru</email>
                            |    <currencies>
                            |      <currency id="RUR" rate="1" />
                            |    </currencies>
                            |    <categories>
                            |      <category id="1">Вакансия</category>
                            |      <category id="2" parentId="1">Информационные технологии, интернет, телеком</category>
                            |      <category id="3" parentId="1">Бухгалтерия</category>
                            |      <category id="5" parentId="3">Кадры</category>
                            |    </categories>
                            |    <sets>
                            |      <set id="s1">
                            |        <name>Вакансии компании Сбербанк</name>
                            |        <url>https://rabota.sample.s3.yandex.net/employer/3529</url>
                            |      </set>
                            |      <set id="s2">
                            |        <name>Работа бухгалтером</name>
                            |        <url>https://rabota.sample.s3.yandex.net/vacancies/bukhgalter</url>
                            |      </set>
                            |      <set id="s3">
                            |        <name>Работа главным бухгалтером</name>
                            |        <url>https://rabota.sample.s3.yandex.net/vacancies/bukhgalter_glavnyy</url>
                            |      </set>
                            |    </sets>
                            |    <offers>
                            |      <offer id="v45671738">
                            |        <name>Главный бухгалтер</name>
                            |        <vendor>ХИМЗАЩИТА</vendor>
                            |        <url>https://rabota.sample.s3.yandex.net/vacancy/45671738</url>
                            |        <price>0</price><!--з/п не указана-->
                            |        <currencyId>RUR</currencyId>
                            |        <sales_notes>по результатам собеседования</sales_notes>
                            |        <categoryId>5</categoryId>
                            |        <set-ids>s2,s3</set-ids>
                            |        <picture>https://avatars.mds.yandex.net/get-sbs-sd/1534494/3ddb1df0-e96a-4780-b7c6-5fd110c9e32f/orig</picture>
                            |        <param name="Конверсия">0.12</param>
                            |      </offer>
                            |    </offers>
                            |  </shop>
                            |</yml_catalog>
                            |""".stripMargin)
          .toList
          .collect { case Right(offer) =>
            offer
          }
        assert(result)(hasSize(equalTo(1))) &&
        assert(result.head.category.map(_.parentCategories))(
          isSome(
            equalTo(
              List(
                "Бухгалтерия",
                "Вакансия"
              )
            )
          )
        ) &&
        assert(result.head.category.map(_.category))(isSome(equalTo("Кадры")))
      }
    )
}
