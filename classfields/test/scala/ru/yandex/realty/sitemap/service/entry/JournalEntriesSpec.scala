package ru.yandex.realty.sitemap.service.entry

import ru.yandex.realty.canonical.base.request.AbsoluteDate
import ru.yandex.realty.journal.model.SitemapUrlPath
import ru.yandex.realty.journal.service.Journal
import ru.yandex.realty.model.url.UrlPath.UrlPath
import ru.yandex.realty.sitemap.model.entry.JournalEntry
import ru.yandex.realty.sitemap.model.{ChangeFrequency, FeedTarget, SitemapEntryRequest, SitemapUrl}
import ru.yandex.realty.sitemap.service.entry.live.JournalEntries
import ru.yandex.realty.sitemap.testkit.EntriesSpec
import zio.magic._
import zio.test.ZSpec
import zio.test.junit.JUnitRunnableSpec
import zio.test.mock.Expectation._
import zio.test.mock.mockable
import eu.timepit.refined.auto._

import java.time.Instant
import java.util.Date

class JournalEntriesSpec extends JUnitRunnableSpec {

  @mockable[Journal.Service]
  object JournalMock

  private def expectedEntryRequest(path: UrlPath, lastMod: Date) =
    SitemapEntryRequest[JournalEntry](
      SitemapUrl(
        path = path,
        lastMod = AbsoluteDate(lastMod).calculateModificationDate(),
        changeFrequency = ChangeFrequency.Daily,
        priority = 1.0,
        images = Seq.empty,
        target = FeedTarget.SitemapJournal
      )
    )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("JournalEntries") {
      testM("should correctly provide entries") {

        val date1 = Date.from(Instant.ofEpochSecond(1000))
        val date2 = Date.from(Instant.ofEpochSecond(2000))

        val returns = Seq(
          SitemapUrlPath(
            pathname = "/url1",
            changefreq = "aba",
            priority = 0.1,
            lastmod = date1
          ),
          SitemapUrlPath(
            pathname = "/url2",
            changefreq = "aba1",
            priority = 0.2,
            lastmod = date2
          )
        )

        EntriesSpec
          .specEffect[SitemapEntryRequest[JournalEntry]](
            Seq(
              expectedEntryRequest("/url1", date1),
              expectedEntryRequest("/url2", date2)
            ),
            loggedPrefix = Some("journal")
          )
          .inject(
            JournalEntries.live,
            JournalMock.Sitemap(value(returns))
          )
      }
    }
}
