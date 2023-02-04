package ru.yandex.vertis.general.feed.transformer.logic.test

import common.zio.files.ZFiles
import general.feed.transformer.FeedFormat
import ru.yandex.vertis.general.feed.transformer.logic.FeedFormatDetector.FeedFormatDetector
import ru.yandex.vertis.general.feed.transformer.logic.{FeedFormatDetector, FeedTransformer}
import zio.ZIO
import zio.test.Assertion._
import zio.test._

import java.io.PrintWriter

object FeedFormatDetectorTest extends DefaultRunnableSpec {

  override def spec =
    suite("DefaultFeedFormatDetector")(
      testM("Detect simple GENERAL format") {
        detectForXml(
          s"""<feed version="1"></feed>""".stripMargin
        ).map(result => assert(result)(equalTo(FeedFormat.GENERAL)))
      },
      testM("Detect simple AVITO format") {
        detectForXml(
          s"""<Ads></Ads>""".stripMargin
        ).map(result => assert(result)(equalTo(FeedFormat.AVITO)))
      },
      testM("Detect simple MARKET format") {
        detectForXml(
          s"""<yml_catalog date="2021-05-27 09:25">""".stripMargin
        ).map(result => assert(result)(equalTo(FeedFormat.MARKET)))
      },
      testM("Detect GENERAL format") {
        detectForXml(
          s"""<?xml version="1.0" encoding="UTF-8"?>
             |<feed version="1"></feed>""".stripMargin
        ).map(result => assert(result)(equalTo(FeedFormat.GENERAL)))
      },
      testM("Detect AVITO format") {
        detectForXml(
          s"""<?xml version="1.0" encoding="UTF-8"?>
             |<Ads></Ads>""".stripMargin
        ).map(result => assert(result)(equalTo(FeedFormat.AVITO)))
      },
      testM("Detect MARKET format") {
        detectForXml(
          s"""<?xml version="1.0" encoding="UTF-8"?>
             |<yml_catalog date="2021-05-27 09:25">""".stripMargin
        ).map(result => assert(result)(equalTo(FeedFormat.MARKET)))
      },
      testM("Detect MARKET format with YML prolog") {
        detectForXml(
          s"""<?xml version="1.0" encoding="UTF-8"?>
             |<!DOCTYPE yml_catalog SYSTEM "shops.dtd">
             |<yml_catalog date="2021-05-27 09:25">""".stripMargin
        ).map(result => assert(result)(equalTo(FeedFormat.MARKET)))
      },
      testM("Detect MARKET format with YML prolog in single-line head") {
        detectForXml(
          s"""<?xml version="1.0" encoding="UTF-8"?><!DOCTYPE yml_catalog SYSTEM "shops.dtd"><yml_catalog date="2021-05-27 09:25">""".stripMargin
        ).map(result => assert(result)(equalTo(FeedFormat.MARKET)))
      },
      testM("Detect MARKET format with comments") {
        detectForXml(
          s"""<?xml version="1.0" encoding="UTF-8"?>
             |<!DOCTYPE yml_catalog SYSTEM "shops.dtd">
             |<!-- COMMENT -->
             |<yml_catalog date="2021-05-27 09:25">""".stripMargin
        ).map(result => assert(result)(equalTo(FeedFormat.MARKET)))
      },
      testM("Detect AVITO_SH format") {
        detectForXml(
          s"""{
             |  "avito_sh_profile": {
             |    "profile_name": "\u0420\u0443\u0441\u0441\u043a\u0438\u0439 \u0412\u043e\u0438\u043d",
             |    "profile_phone": "8913456-04-00",
             |    "sh_last_visited": "2021-06-23T13:03:49+0300",
             |    "offers": [
             |      {
             |        "listing_url": "https://www.avito.ru/novosibirsk/kollektsionirovanie/nozh_kerambit_iz_dereva_1980802836",
             |        "listing_id": "1980802836",
             |        "listing_name": "\u041d\u043e\u0436"
             |      }
             |    ]
             |  }
             |}""".stripMargin
        ).map(result => assert(result)(equalTo(FeedFormat.AVITO_SH)))
      },
      testM("Detect VACANCY_US format") {
        detectForXml(
          s"""<?xml version="1.0" encoding="utf-8" standalone="yes"?>
             |<yml_catalog date="2018-07-17 12:10">
             |  <shop>
             |    <name>Сэмпл.Работа</name>
             |    <company>ООО Сэмпл.Работа</company>
             |    <url>rabota.sample.s3.yandex.net</url>
             |    <!--Добавили sets по сравнению с обычным YML-->
             |    <sets>
             |      <set id="s1">
             |        <name>Вакансии компании Сбербанк</name>
             |        <url>https://rabota.sample.s3.yandex.net/employer/3529</url>
             |      </set>
             |    </sets>
             |    <offers>
             |      <offer id="sv1a1e3529s1">
             |        <name>Информационные технологии, интернет, телеком</name>
             |        <vendor>Сбербанк</vendor>
             |        <url>https://rabota.sample.s3.yandex.net/search/vacancy?area=1&amp;employer_id=3529&amp;specialization=1</url>
             |        <price from="true">86500</price>
             |        <currencyId>RUR</currencyId>
             |        <categoryId>2</categoryId>
             |        <set-ids>s1</set-ids>
             |        <picture>https://pic.ru/square_166</picture>
             |        <param name="Конверсия">2.01711</param>
             |      </offer>
             |    </offers>
             |  </shop>
             |</yml_catalog>
             |""".stripMargin
        ).map(result => assert(result)(equalTo(FeedFormat.VACANCY_US)))
      },
      testM("Fail with empty file") {
        detectForXml(
          s"""""".stripMargin
        ).flip.map(result => assert(result)(equalTo(FeedTransformer.InvalidFile)))
      }
    ).provideCustomLayerShared(FeedFormatDetector.live)

  private def detectForXml(xmlFileEntry: String): ZIO[FeedFormatDetector, FeedTransformer.Error, FeedFormat] = {
    ZFiles
      .makeTempFile(null, null)
      .orDie
      .use { file =>
        val writer = new PrintWriter(file)
        writer.print(xmlFileEntry)
        writer.close()
        FeedFormatDetector.detectFeedFormat(file)
      }
  }
}
