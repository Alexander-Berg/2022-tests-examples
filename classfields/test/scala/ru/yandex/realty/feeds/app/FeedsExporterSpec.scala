package ru.yandex.realty.feeds.app

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.{PropertyChecks, TableDrivenPropertyChecks}
import org.scalatest.{Matchers, WordSpec}
import ru.yandex.realty.feeds.OfferBuilder
import ru.yandex.realty.feeds.filters.Filter
import ru.yandex.realty.feeds.mock._
import ru.yandex.realty.feeds.services.FeedsExporter
import ru.yandex.realty.feeds.services.impl.FeedsExporterMetered
import ru.yandex.realty.feeds.services.impl.FeedsExporterMetered._
import ru.yandex.realty.model.feed.{FeedGeneratorOfferQuery, FeedType}
import ru.yandex.realty.model.offer.{Offer, OfferType}
import ru.yandex.vertis.ops.prometheus.{PrometheusRegistry, SimpleCompositeCollector}
import zio.{Runtime, ZEnv, ZLayer}

@RunWith(classOf[JUnitRunner])
class FeedsExporterSpec extends WordSpec with Matchers with PropertyChecks with TableDrivenPropertyChecks {
  private val zioRuntime: Runtime[ZEnv] = Runtime.default
  private def prometheusRegistry: PrometheusRegistry = new SimpleCompositeCollector()

  "FeedsExporter" should {
    "correctly export" in {
      val offerQuery = OffersQueries.empty("test", OfferType.SELL, FeedType.AdWords)
      val result = new StringBuilder

      zioRuntime.unsafeRunTask(
        FeedsExporter
          .`export`[Offer, FeedGeneratorOfferQuery, String](
            () => Seq(offerQuery),
            _.fileNamePrefix,
            new FakeEntityExporter(result),
            _ => Filter.empty[Offer],
            new MockFeedEntriesFetcher(Seq(OfferBuilder.build())),
            _ => Some(new MockFeedEntryPrinter[Offer](_ => "content")),
            _ => new StringObjectBuffer
          )
          .provideLayer(ZLayer.succeed(prometheusRegistry) >>> FeedsExporterMetered.layer)
      )

      result.toString shouldBe "header\ncontent\nfooter\n"
    }
    "correctly return prometheus stats with no suitable offers" in {
      val queryName = "test"
      val offerQuery = OffersQueries.empty(queryName, OfferType.SELL, FeedType.AdWords)
      val result = new StringBuilder
      val prometheus = prometheusRegistry

      zioRuntime.unsafeRunTask(
        FeedsExporter
          .`export`[Offer, FeedGeneratorOfferQuery, String](
            () => Seq(offerQuery),
            _.fileNamePrefix,
            new FakeEntityExporter(result),
            _ => Filter.apply[Offer]("rent-filter")(_.getOfferType == OfferType.RENT),
            new MockFeedEntriesFetcher(Seq(OfferBuilder.build())),
            _ => Some(new MockFeedEntryPrinter[Offer](_ => "content")),
            _ => new StringObjectBuffer
          )
          .provideLayer(ZLayer.succeed(prometheus) >>> FeedsExporterMetered.layer)
      )

      val collector = prometheus.asCollectorRegistry()
      val feedsRead = collector.getSampleValue(feedsReadMetricsName)
      val feedsWritten = collector.getSampleValue(feedsWrittenMetrics, Array("feed_name"), Array(queryName))
      val feedsFiltered = collector.getSampleValue(feedsFilteredMetrics, Array("feed_name"), Array(queryName))

      feedsRead shouldBe 1.0
      feedsFiltered shouldBe 0.0
      feedsWritten shouldBe 0.0
    }
    "correctly return prometheus stats" in {
      val queryName = "test"
      val offerQuery = OffersQueries.empty(queryName, OfferType.SELL, FeedType.AdWords)
      val result = new StringBuilder
      val prometheus = prometheusRegistry
      val offers = Seq(
        OfferBuilder.build(offerType = OfferType.RENT),
        OfferBuilder.build(),
        OfferBuilder.build(offerType = OfferType.RENT)
      )

      zioRuntime.unsafeRunTask(
        FeedsExporter
          .`export`[Offer, FeedGeneratorOfferQuery, String](
            () => Seq(offerQuery),
            _.fileNamePrefix,
            new FakeEntityExporter(result),
            _ => Filter.apply[Offer]("rent-filter")(_.getOfferType == OfferType.RENT),
            new MockFeedEntriesFetcher(offers),
            _ => Some(new MockFeedEntryPrinter[Offer](_ => "content")),
            _ => new StringObjectBuffer
          )
          .provideLayer(ZLayer.succeed(prometheus) >>> FeedsExporterMetered.layer)
      )

      val collector = prometheus.asCollectorRegistry()
      val feedsRead = collector.getSampleValue(feedsReadMetricsName)
      val feedsWritten = collector.getSampleValue(feedsWrittenMetrics, Array("feed_name"), Array(queryName))
      val feedsFiltered = collector.getSampleValue(feedsFilteredMetrics, Array("feed_name"), Array(queryName))

      feedsRead shouldBe 3.0
      feedsFiltered shouldBe 2.0
      feedsWritten shouldBe 2.0
    }
  }
}
