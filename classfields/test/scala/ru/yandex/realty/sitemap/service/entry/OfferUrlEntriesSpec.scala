package ru.yandex.realty.sitemap.service.entry

import ru.yandex.realty.canonical.base.request.ModificationDate
import ru.yandex.realty.model.url.UrlPath
import ru.yandex.realty.sitemap.model.entry.OfferUrlEntry
import ru.yandex.realty.sitemap.model.{ChangeFrequency, FeedTarget, SitemapEntryRequest, SitemapUrl}
import ru.yandex.realty.sitemap.service.entry.live.OfferUrlEntries
import ru.yandex.realty.sitemap.testkit.EntriesSpec
import ru.yandex.realty.urls.common.ShortOfferInfo
import zio.ZLayer
import zio.magic._
import zio.test.ZSpec
import zio.test.junit.JUnitRunnableSpec

import java.time.Instant
import java.util.Date

class OfferUrlEntriesSpec extends JUnitRunnableSpec {

  private def shortOfferInfo(id: Long, date: Date) =
    ShortOfferInfo(
      id = id,
      creationDate = date,
      regionGraphId = 1,
      author = None
    )

  private def expectedEntry(id: Long, date: Date) =
    SitemapEntryRequest[OfferUrlEntry](
      SitemapUrl(
        path = UrlPath.make(s"/offer/$id/").get,
        lastMod = ModificationDate.dateUpdatedInThreeDays(date).calculateModificationDate(),
        changeFrequency = ChangeFrequency.Weekly,
        priority = 0.7,
        images = Seq.empty,
        target = FeedTarget.SitemapOffers
      )
    )

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("OfferUrlEntries") {
      testM("should correctly produce entries") {

        val d1 = Date.from(Instant.ofEpochSecond(1000))
        val d2 = Date.from(Instant.ofEpochSecond(2000))
        val d3 = Date.from(Instant.ofEpochSecond(3000))

        val offers = Seq(
          shortOfferInfo(1, d1),
          shortOfferInfo(2, d2),
          shortOfferInfo(3, d3)
        )

        EntriesSpec
          .specEffect[SitemapEntryRequest[OfferUrlEntry]](
            Seq(
              expectedEntry(1, d1),
              expectedEntry(2, d2),
              expectedEntry(3, d3)
            ),
            loggedPrefix = Some("offers")
          )
          .inject(
            ZLayer.succeed(offers),
            OfferUrlEntries.layer
          )
      }
    }
}
