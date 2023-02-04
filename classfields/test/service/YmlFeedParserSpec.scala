package ru.vertistraf.common.service

import org.apache.commons.io.FileUtils
import ru.vertistraf.common.model.xml.ParsedXmlFile
import ru.vertistraf.common.model.yml._
import ru.vertistraf.common.model.yml.reader.YmlFeedContentReader
import ru.vertistraf.common.service.xml.XmlParser
import zio._
import zio.test.Assertion._
import zio.test._

import java.nio.file.{Files, Path}

object YmlFeedParserSpec extends DefaultRunnableSpec {

  private val CorrectlyFormattedFeed: String = "correctly_formatted_feed_0.yml"
  private val CorrectlyEmptyFeed: String = "correctly_empty_0.yml"

  private val IncorrectFeeds = Seq(
    "incorrect_yml_0.yml",
    "missing_categories_0.yml",
    "missing_offers_0.yml",
    "missing_sets_0.yml",
    "non_escaped_char_0.yml"
  )

  private val ExpectedCategories: Seq[YmlCategory] = Seq(
    YmlCategory(
      1.ymlCategoryId,
      None,
      "Квартира"
    ),
    YmlCategory(
      2.ymlCategoryId,
      None,
      "Комната"
    ),
    YmlCategory(
      10.ymlCategoryId,
      Some(1.ymlCategoryId),
      "Студия"
    ),
    YmlCategory(
      11.ymlCategoryId,
      Some(1.ymlCategoryId),
      "1-комнатная"
    ),
    YmlCategory(
      51.ymlCategoryId,
      Some(6.ymlCategoryId),
      "Юридический адрес"
    )
  )

  val ExpectedSets = Seq(
    YmlSet(
      64.ymlSetId,
      "https://m.realty.yandex.ru/sochi/kupit/kvartira/zhk-alpijskij-kvartal-1707384/novostroyki-i-s-parkovkoy/",
      "Купить квартиру в новостройке с парковкой ЖК «Альпийский квартал» — свежие объявления в Сочи"
    ),
    YmlSet(
      65.ymlSetId,
      "https://realty.yandex.ru/sochi/kupit/kvartira/zhk-alpijskij-kvartal-1707384/novostroyki-i-s-parkovkoy/",
      "Купить квартиру в новостройке с парковкой ЖК «Альпийский квартал» — свежие объявления в Сочи"
    ),
    YmlSet(
      158.ymlSetId,
      "https://m.realty.yandex.ru/nahabino/kupit/kvartira/zhk-malina-85998/novostroyki/",
      "Купить квартиру в новостройке ЖК «Малина» — свежие объявления в Нахабино"
    ),
    YmlSet(
      159.ymlSetId,
      "https://realty.yandex.ru/nahabino/kupit/kvartira/zhk-malina-85998/novostroyki/",
      "Купить квартиру в новостройке ЖК «Малина» — свежие объявления в Нахабино"
    ),
    YmlSet(
      162.ymlSetId,
      "https://m.realty.yandex.ru/lyubertsy/kupit/kvartira/studiya/zhk-oblaka-942891/novostroyki-i-s-parkovkoy/",
      "Купить студию в новостройке с парковкой ЖК «Облака» — свежие объявления в Люберцах"
    )
  )

  private val ExpectedOffers = Seq(
    YmlOffer(
      id = 1.ymlOfferId,
      name = "1-комнатная, 30 м²",
      url =
        "https://realty.yandex.ru/kireevskiy_rayon/kupit/kvartira/s-vodoyomom-i-s-parkom/?pinnedOfferId=1014341084576098347",
      price = YmlPrice.Direct(950000.0),
      currencyId = "RUR",
      categoryId = 11.ymlCategoryId,
      pictures =
        Seq("http://avatars.mds.yandex.net/get-realty/2751002/offer.1014341084576098347.8804577040790267408/wiz_t2"),
      params = Map(
        ("Конверсия", "1.7773788244619323E-7"),
        ("Тип предложения", "Продажа"),
        ("Дата публикации", "2021-12-12T20:56:54.844Z"),
        ("Площадь", "30"),
        ("Размещено агентом", "Да"),
        ("Число комнат", "1")
      ),
      vendor = None,
      description = None,
      setIds = Seq(64, 65, 158).map(_.ymlSetId)
    ),
    YmlOffer(
      id = 2.ymlOfferId,
      name = "Студия, 20 м²",
      url =
        "https://realty.yandex.ru/krasnodar/kupit/kvartira/bez-remonta-i-s-balkonom/?pinnedOfferId=1726990548896266496",
      price = YmlPrice.Direct(2650000),
      currencyId = "RUR",
      categoryId = 10.ymlCategoryId,
      pictures = Seq(
        "http://avatars.mds.yandex.net/get-realty/1572784/add.f5caa193b3cb4f4bd229267360fdce5d.realty-api-vos/wiz_t2"
      ),
      params = Map(
        ("Конверсия", "1.777049192633372E-7"),
        ("Тип предложения", "Продажа"),
        ("Дата публикации", "2021-12-09T08:29:43.145Z"),
        ("Площадь", "20"),
        ("Размещено агентом", "Нет"),
        ("Число комнат", "0")
      ),
      vendor = None,
      description = None,
      setIds = Seq(159, 162).map(_.ymlSetId)
    ),
    YmlOffer(
      id = 3.ymlOfferId,
      name = "Студия, 45 м²",
      url =
        "https://realty.yandex.ru/krasnodar/kupit/kvartira/bez-remonta-i-s-balkonom/?pinnedOfferId=1726990548896266495",
      price = YmlPrice.From(2650000),
      currencyId = "RUR",
      categoryId = 10.ymlCategoryId,
      pictures = Seq(
        "http://avatars.mds.yandex.net/get-realty/1572784/add.f5caa193b3cb4f4bd229267360fdce5d.realty-api-vos/wiz_t2"
      ),
      params = Map(
        ("Конверсия", "1.777049192633372E-7"),
        ("Тип предложения", "Продажа"),
        ("Дата публикации", "2021-12-09T08:29:43.145Z"),
        ("Площадь", "45"),
        ("Размещено агентом", "Нет"),
        ("Число комнат", "0")
      ),
      vendor = None,
      description = None,
      setIds = Seq.empty
    )
  )

  private def doWithResourceAsFile[R, A](resourceName: String)(f: Path => RIO[R, A]) =
    for {
      tempDir <- Task.effect(Files.createTempDirectory("YmlFeedParserSpec"))
      is <- Task.effect(getClass.getClassLoader.getResourceAsStream(resourceName))
      resFile <- Task.effectTotal(tempDir.resolve(resourceName))
      _ <- Task.effect(Files.copy(is, resFile))
      result <- f(resFile)
      _ <- Task.effect(FileUtils.deleteDirectory(tempDir.toFile))
    } yield result

  private val ExpectedCorrectlyFormattedFeed =
    ParsedXmlFile[YmlFeed](
      YmlFeed(
        ExpectedCategories,
        ExpectedSets,
        ExpectedOffers
      ),
      CorrectlyFormattedFeed
    )

  private def correctlyParseTest(feedName: String, expected: ParsedXmlFile[YmlFeed]) = {
    testM(s"Should correctly parse `$feedName` feed") {
      doWithResourceAsFile(feedName) { path =>
        XmlParser
          .parseFile[YmlFeed](path)
          .map(actual => assertTrue(actual == expected))
      }
    }
  }

  private def parsingFailsTests() =
    suite("should fail for some cases")(
      IncorrectFeeds
        .map { feedName =>
          testM(s"should fail for `$feedName`") {
            doWithResourceAsFile(feedName) { path =>
              assertM {
                XmlParser.parseFile[YmlFeed](path).run
              }(fails(anything))
            }
          }
        }: _*
    )

  import YmlFeedContentReader._

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("Yml feed parser")(
      correctlyParseTest(CorrectlyFormattedFeed, ExpectedCorrectlyFormattedFeed),
      correctlyParseTest(
        CorrectlyEmptyFeed,
        ParsedXmlFile(YmlFeed(Seq.empty, Seq.empty, Seq.empty), CorrectlyEmptyFeed)
      ),
      parsingFailsTests()
    ).provideLayer(XmlParser.live[YmlFeed])

  implicit private class TaggedIntSugar(val i: Int) extends AnyVal {
    def ymlCategoryId: YmlCategoryId = makeCategoryId(i)

    def ymlSetId: YmlSetId = makeSetId(i.toString)

    def ymlOfferId: YmlOfferId = makeOfferId(i.toString)
  }
}
