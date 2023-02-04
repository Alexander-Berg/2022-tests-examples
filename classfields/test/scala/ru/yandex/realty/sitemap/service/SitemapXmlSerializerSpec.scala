package ru.yandex.realty.sitemap.service

import eu.timepit.refined.auto._
import org.junit.runner.RunWith
import ru.yandex.realty.canonical.base.request.AbsoluteDate
import ru.yandex.realty.model.url.UrlPath.UrlPath
import ru.yandex.realty.sitemap.config.DomainsConfig
import ru.yandex.realty.sitemap.model.{ChangeFrequency, FeedTarget, SitemapUrl}
import ru.yandex.realty.sitemap.service.SitemapXmlSerializer.SitemapXmlSerializer
import ru.yandex.realty.sitemap.service.live.LiveSitemapXmlSerializer
import ru.yandex.realty.traffic.utils.FilesService
import ru.yandex.realty.traffic.utils.FilesService.{FilesService, FilesServiceConfig}
import zio._
import zio.blocking.{effectBlocking, Blocking}
import zio.magic._
import zio.test._
import zio.test.environment.{TestClock, TestEnvironment}
import zio.test.junit._

import java.io.{BufferedReader, InputStreamReader}
import java.nio.file.{Files, Path}
import java.util.Date
import java.util.stream.Collectors
import java.util.zip.GZIPInputStream

@RunWith(classOf[ZTestJUnitRunner])
class SitemapXmlSerializerSpec extends JUnitRunnableSpec {

  private val CurrentDateMs = 1337111814L
  private val CurrentDateMsFormat = "1970-01-16T14:25:11+03:00"

  private val Host: String = "url.host"
  private val RentHost: String = "rent.url.host"
  private val IndexUrlPrefix: String = "http://url.host/index/prefix"
  private val SpecConfig: LiveSitemapXmlSerializer.Config = LiveSitemapXmlSerializer.Config(IndexUrlPrefix)

  private def getGzFileContent(path: Path): RIO[Blocking, String] =
    effectBlocking {
      new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(path))))
        .lines()
        .collect(Collectors.joining(System.lineSeparator()))
    }

  private def withService[A](
    action: RIO[SitemapXmlSerializer with TestClock with Blocking, A]
  ): RIO[TestEnvironment, A] = {
    val layer = ZLayer.wireSome[TestEnvironment, FilesService with SitemapXmlSerializer with TestClock with Blocking](
      ZLayer.succeed(SpecConfig),
      ZLayer.succeed(FilesServiceConfig(Files.createTempDirectory("SitemapXmlSerializerSpec"))),
      FilesService.live,
      SitemapXmlSerializer.live,
      ZLayer.succeed(DomainsConfig(Host, RentHost))
    )

    (for {
      res <- action
      _ <- FilesService.freeAllTemporary()
    } yield res).provideLayer(layer)
  }

  private def urlsSerializationSpec(urls: SitemapUrl*)(expected: String) =
    withService {
      SitemapXmlSerializer
        .serializeUrlsFeed(urls)
        .flatMap(getGzFileContent)
        .map(res => assertTrue(res == expected))
    }

  private def makeUrl(
    path: UrlPath,
    images: Seq[String] = Seq.empty,
    target: FeedTarget = FeedTarget.SitemapJournal
  ): SitemapUrl =
    SitemapUrl(
      path = path,
      lastMod = AbsoluteDate(new Date(CurrentDateMs)).calculateModificationDate(),
      changeFrequency = ChangeFrequency.Always,
      priority = 1.0,
      images = images,
      target = target
    )

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("SitemapXmlSerializer")(
      testM("correctly serialize sitemap index") {
        TestClock.setTime(zio.duration.Duration.fromMillis(CurrentDateMs)) *>
          withService {
            SitemapXmlSerializer
              .serializeIndexFile(Set("sitemap1.xml", "sitemap2.xml"))
              .flatMap(getGzFileContent)
              .map {
                content =>
                  assertTrue {
                    content == s"""<?xml version=\"1.0\" encoding=\"UTF-8\"?>
                               |<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">
                               | <sitemap>
                               |  <loc>$IndexUrlPrefix/sitemap1.xml</loc>
                               |  <lastmod>$CurrentDateMsFormat</lastmod>
                               | </sitemap>
                               | <sitemap>
                               |  <loc>$IndexUrlPrefix/sitemap2.xml</loc>
                               |  <lastmod>$CurrentDateMsFormat</lastmod>
                               | </sitemap>
                               |</sitemapindex>
                               |""".stripMargin
                  }
              }
          }
      },
      testM("correctly serialize url without images") {
        urlsSerializationSpec(
          makeUrl("/some/url")
        ) {
          s"""<?xml version="1.0" encoding="UTF-8"?>
            |<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:image="http://www.google.com/schemas/sitemap-image/1.1" xmlns:video="http://www.google.com/schemas/sitemap-video/1.1" xmlns:xhtml="http://www.w3.org/1999/xhtml">
            |  <url>
            |    <loc>https://url.host/some/url</loc>
            |    <lastmod>$CurrentDateMsFormat</lastmod>
            |    <changefreq>always</changefreq>
            |    <priority>1.0</priority>
            |  </url>
            |</urlset>""".stripMargin
        }
      },
      testM("correctly serialize url with images") {
        urlsSerializationSpec(
          makeUrl("/some/url", images = Seq("img1", "img2"))
        ) {
          s"""<?xml version="1.0" encoding="UTF-8"?>
             |<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:image="http://www.google.com/schemas/sitemap-image/1.1" xmlns:video="http://www.google.com/schemas/sitemap-video/1.1" xmlns:xhtml="http://www.w3.org/1999/xhtml">
             |  <url>
             |    <loc>https://url.host/some/url</loc>
             |    <image:image>
             |      <image:loc>img1</image:loc>
             |    </image:image>
             |    <image:image>
             |      <image:loc>img2</image:loc>
             |    </image:image>
             |    <lastmod>$CurrentDateMsFormat</lastmod>
             |    <changefreq>always</changefreq>
             |    <priority>1.0</priority>
             |  </url>
             |</urlset>""".stripMargin
        }
      },
      testM("correctly serialize arenda stand apart url") {
        urlsSerializationSpec(
          makeUrl("/some/url", target = FeedTarget.YandexRentStandApartSitemap)
        ) {
          s"""<?xml version="1.0" encoding="UTF-8"?>
             |<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:image="http://www.google.com/schemas/sitemap-image/1.1" xmlns:video="http://www.google.com/schemas/sitemap-video/1.1" xmlns:xhtml="http://www.w3.org/1999/xhtml">
             |  <url>
             |    <loc>https://$RentHost/some/url</loc>
             |    <lastmod>$CurrentDateMsFormat</lastmod>
             |    <changefreq>always</changefreq>
             |    <priority>1.0</priority>
             |  </url>
             |</urlset>""".stripMargin
        }
      }
    )
}
