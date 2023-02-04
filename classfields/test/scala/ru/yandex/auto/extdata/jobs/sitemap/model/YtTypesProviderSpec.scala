package ru.yandex.auto.extdata.jobs.sitemap.model

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.auto.extdata.service.sitemap.SitemapCanonicalRequest.Changefreqs
import ru.yandex.auto.traffic.model.{Url, UrlPath}
import ru.yandex.auto.traffic.yt.YtTypesProvider

import java.time.Instant
import java.util.Date

@RunWith(classOf[JUnitRunner])
class YtTypesProviderSpec extends WordSpec with Matchers {

  private def spec[T](converter: YtTypesProvider[T], elem: T) = {
    val actual = converter.fromMapNode(converter.toMapNode(elem))
    actual shouldBe elem
  }

  "SitemapFeed" should {
    "be correctly converted to-from mapNode" in spec(
      SitemapFeed,
      SitemapFeed(
        target = SitemapTarget.CostPlusAll,
        index = 100,
        urlsCount = 1337,
        gzipContent = "mama papa".getBytes("UTF-8")
      )
    )
  }

  "SitemapRow" should {
    "be correctly converted to-from mapNode" in spec(
      SitemapRow,
      SitemapRow(
        url = Url("https://m.auto.ru/cars/all/"),
        lastmod = Some(
          Date.from(Instant.now())
        ),
        changefreq = Some(Changefreqs.Always),
        priority = Some("0.75"),
        images = Seq(
          "image1",
          "image2"
        ),
        target = SitemapTarget.OffersCars
      )
    )
  }

  "SitemapRow" should {
    val entryWithUrl = SitemapEntry(
      url = SitemapEntry.EntryUrl.JustUrl(Url("https://m.auto.ru/cars/all/")),
      lastmod = Some(
        Date.from(Instant.now())
      ),
      changefreq = Some(Changefreqs.Always),
      priority = Some("0.75"),
      images = Seq(
        "image1",
        "image2"
      ),
      target = SitemapTarget.OffersCars
    )

    "be correctly converted to-from mapNode when url present" in spec(
      SitemapEntry,
      entryWithUrl
    )

    "be correctly converted to-from mapNode when urlpath present" in spec(
      SitemapEntry,
      entryWithUrl.copy(
        url = SitemapEntry.EntryUrl.Path(UrlPath("/some/path"))
      )
    )
  }

}
