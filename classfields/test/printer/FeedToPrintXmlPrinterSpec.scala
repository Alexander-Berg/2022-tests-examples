package ru.vertistraf.cost_plus.exporter.printer

import common.tagged.tag
import ru.vertistraf.common.model.s3.S3Path
import ru.vertistraf.cost_plus.exporter.config.{ShopConfig, YmlFeedConfig}
import ru.vertistraf.cost_plus.exporter.model.FeedToPrint
import ru.vertistraf.cost_plus.exporter.printer.{CostPlusOfferXmlPrinter, CostPlusSetXmlPrinter, FeedToPrintXmlPrinter}
import ru.vertistraf.cost_plus.model.result.{CostPlusOffer, CostPlusPrice, CostPlusSet}
import ru.vertistraf.cost_plus.model.{AutoCategories, Categories, IdTag}
import zio.test._
import zio.test.Assertion._

import java.io.{PrintWriter, StringWriter}
import java.time.Instant

object FeedToPrintXmlPrinterSpec extends DefaultRunnableSpec {

  private val Sets = Seq(
    CostPlusSet(
      url = "url1",
      title = "title1",
      id = tag[IdTag][String]("1")
    )
  )

  private def baseSpec(name: String, in: FeedToPrint)(expected: String) = {
    test(s"should correctly print $name") {
      val sw = new StringWriter()
      val pw = new PrintWriter(sw)

      printer.print(in)(pw)
      pw.close()

      assertTrue(sw.toString == expected)
    }
  }

  object SpecCategories extends Categories {
    val A = Category(1, "a")
    val B = Category(2, "b", parent = Some(A))
  }

  private val printer = FeedToPrintXmlPrinter(CostPlusSetXmlPrinter, CostPlusOfferXmlPrinter)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = suite("FeedToPrintXmlPrinter")(
    baseSpec(
      "empty feed",
      FeedToPrint(
        Seq.empty,
        Seq.empty,
        YmlFeedConfig(
          ShopConfig(
            name = "name",
            company = ShopConfig.Company.Verticals,
            url = "url",
            email = Some("email")
          ),
          categories = SpecCategories.all
        ),
        createTime = Instant.ofEpochSecond(10000)
      )
    ) {
      s"""<?xml version="1.0" encoding="utf-8" standalone="yes"?>
         |<yml_catalog date="1970-01-01 02:46">
         |<shop>
         |  <name>name</name>
         |  <company>${ShopConfig.Company.Verticals.companyName}</company>
         |  <url>url</url>
         |  <email>email</email>
         |  <currencies>
         |    <currency id="RUR" rate="1" />
         |  </currencies>
         |<categories>
         |  <category id="1">a</category>
         |  <category parent="1" id="2">b</category>
         |</categories>
         |<sets>
         |</sets>
         |<offers>
         |</offers>
         |</shop>
         |</yml_catalog>
         |""".stripMargin
    },
    baseSpec(
      "non empty feed",
      FeedToPrint(
        Seq(
          CostPlusOffer(
            id = tag[IdTag][String]("id"),
            name = "name",
            url = "http://url/?a=1&get=2",
            price = CostPlusPrice.From(1),
            currency = "RUR",
            categoryId = 1,
            pictures = Seq("img1", "img2"),
            vendor = Some("vendor"),
            description = None,
            params = Map("Название" -> "10"),
            sets = Sets
          )
        ),
        Sets,
        YmlFeedConfig(
          ShopConfig(
            name = "name",
            company = ShopConfig.Company.Verticals,
            url = "url",
            email = Some("email")
          ),
          categories = SpecCategories.all
        ),
        createTime = Instant.ofEpochSecond(10000)
      )
    ) {
      s"""<?xml version="1.0" encoding="utf-8" standalone="yes"?>
         |<yml_catalog date="1970-01-01 02:46">
         |<shop>
         |  <name>name</name>
         |  <company>${ShopConfig.Company.Verticals.companyName}</company>
         |  <url>url</url>
         |  <email>email</email>
         |  <currencies>
         |    <currency id="RUR" rate="1" />
         |  </currencies>
         |<categories>
         |  <category id="1">a</category>
         |  <category parent="1" id="2">b</category>
         |</categories>
         |<sets>
         |<set id="1">
         |  <name>title1</name>
         |  <url>url1</url>
         |</set>
         |</sets>
         |<offers>
         |<offer id="id">
         |  <name>name</name>
         |  <url>http://url/?a=1&amp;get=2</url>
         |  <price from="true">1</price>
         |  <currencyId>RUR</currencyId>
         |  <categoryId>1</categoryId>
         |  <picture>img1</picture>
         |  <picture>img2</picture>
         |  <vendor>vendor</vendor>
         |  <param name="Название">10</param>
         |  <set-ids>1</set-ids>
         |</offer>
         |</offers>
         |</shop>
         |</yml_catalog>
         |""".stripMargin
    }
  )

}
