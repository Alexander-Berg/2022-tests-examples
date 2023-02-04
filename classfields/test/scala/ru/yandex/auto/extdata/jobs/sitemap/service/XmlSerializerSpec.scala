package ru.yandex.auto.extdata.jobs.sitemap.service

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.extdata.jobs.sitemap.model.{SitemapRow, SitemapTarget}
import ru.yandex.auto.extdata.jobs.sitemap.services.impl.LiveXmlSerializer
import ru.yandex.auto.extdata.service.sitemap.SitemapCanonicalRequest.Changefreqs
import ru.yandex.auto.traffic.model.Url

import java.util.Date

@RunWith(classOf[JUnitRunner])
class XmlSerializerSpec extends WordSpec with Matchers {

  private val serializer = LiveXmlSerializer

  "XmlSerializer" should {

    "correctly serialize entries" in {
      val input =
        Seq(
          SitemapRow(
            url = Url("https://auto.ru/cars/all/"),
            lastmod = None,
            changefreq = None,
            priority = None,
            images = Seq.empty,
            target = SitemapTarget.CostPlusAll
          ),
          SitemapRow(
            url = Url("https://auto.ru/cars/all/"),
            lastmod = Some(new Date(0)),
            changefreq = Some(Changefreqs.Weekly),
            priority = Some("0.7"),
            images = Seq("https://auto.ru/img1", "https://auto.ru/img2"),
            target = SitemapTarget.CostPlusAll
          )
        )

      val actual = serializer.serializeSitemap(input)

      println(actual)

      actual shouldBe """<?xml version="1.0" encoding="UTF-8"?>
                        |<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"
                        |xmlns:image="http://www.google.com/schemas/sitemap-image/1.1">
                        |  <url>
                        |    <loc>https://auto.ru/cars/all/</loc>
                        |  </url>
                        |  <url>
                        |    <loc>https://auto.ru/cars/all/</loc>
                        |    <lastmod>1970-01-01</lastmod>
                        |    <changefreq>weekly</changefreq>
                        |    <priority>0.7</priority>
                        |    <image:image>
                        |      <image:loc>https://auto.ru/img1</image:loc>
                        |    </image:image>
                        |    <image:image>
                        |      <image:loc>https://auto.ru/img2</image:loc>
                        |    </image:image>
                        |  </url>
                        |</urlset>
                        |""".stripMargin
    }
  }
}
