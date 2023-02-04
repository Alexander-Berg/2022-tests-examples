package ru.yandex.realty.sitemap.service.entry

import eu.timepit.refined.auto._
import ru.yandex.realty.canonical.base.request.{ModificationDate, RelativeDate}
import ru.yandex.realty.clients.frontend.seo.model.UrlWithoutParams
import ru.yandex.realty.clients.frontend.seo.testkit.FrontendSeoMock
import ru.yandex.realty.model.url.UrlPath.UrlPath
import ru.yandex.realty.sitemap.model.entry.CommonUrlEntry
import ru.yandex.realty.sitemap.model.{ChangeFrequency, FeedTarget, SitemapEntryRequest, SitemapUrl}
import ru.yandex.realty.sitemap.service.entry.live.CommonUrlEntries
import ru.yandex.realty.sitemap.testkit.EntriesSpec
import zio.magic._
import zio.test._
import zio.test.junit.JUnitRunnableSpec

class CommonUrlEntriesSpec extends JUnitRunnableSpec {

  private def expectedEntry(
    target: FeedTarget,
    path: UrlPath,
    lastMod: ModificationDate,
    changeFreq: ChangeFrequency,
    priority: Double
  ) =
    SitemapEntryRequest[CommonUrlEntry](
      SitemapUrl(
        path = path,
        lastMod = lastMod.calculateModificationDate(),
        changeFrequency = changeFreq,
        priority = priority,
        images = Seq.empty,
        target = target
      )
    )

  private def expectedCommonEntry(path: UrlPath) =
    expectedEntry(FeedTarget.SitemapCommons, path, ModificationDate.everyMonday, ChangeFrequency.Daily, 1.0)

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("CommonUrlEntries") {
      testM("should correctly produce entries") {
        val routerReturns = Seq(
          UrlWithoutParams(
            "/url-common",
            "page1"
          ),
          UrlWithoutParams(
            "/url-common-2",
            "page2"
          ),
          UrlWithoutParams(
            "/url-samolet",
            "samolet"
          )
        )

        EntriesSpec
          .specEffect[SitemapEntryRequest[CommonUrlEntry]](
            Seq(
              expectedCommonEntry("/url-common"),
              expectedCommonEntry("/url-common-2"),
              expectedEntry(FeedTarget.SitemapSamolet, "/url-samolet", RelativeDate.Today, ChangeFrequency.Weekly, 0.7)
            ),
            loggedPrefix = Some("common")
          )
          .inject(
            CommonUrlEntries.layer,
            FrontendSeoMock.onSend(CommonUrlEntry.Instance).returns(routerReturns)
          )
      }
    }
}
