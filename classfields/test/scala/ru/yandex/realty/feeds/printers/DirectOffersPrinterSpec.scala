package ru.yandex.realty.feeds.printers

import org.junit.runner.RunWith
import org.scalacheck.Prop.forAll
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.Checkers.check
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.feeds.OfferBuilder
import ru.yandex.realty.feeds.factories.OffersFeedPrinterFactory
import ru.yandex.realty.feeds.mock.OffersQueries
import ru.yandex.realty.geo.RegionGraphTestComponents
import ru.yandex.realty.model.feed.FeedType
import ru.yandex.realty.model.offer.OfferType
import ru.yandex.realty.picapica.MdsUrlBuilder
import ru.yandex.realty.sites.SitesGroupingService
import ru.yandex.realty.storage.CanonicalUrlsStorage

@RunWith(classOf[JUnitRunner])
class DirectOffersPrinterSpec extends WordSpec with MockFactory with Matchers with RegionGraphTestComponents {
  private val mockSites = mock[SitesGroupingService]
  private def provider[T](t: T): Provider[T] = () => t
  private val urlStorage = new CanonicalUrlsStorage(Map.empty)

  private val mdsBuilder = new MdsUrlBuilder("//realty.yandex.ru")
  private val offersPrinterFactory =
    new OffersFeedPrinterFactory(mockSites, provider(urlStorage), regionGraphProvider, mdsBuilder)

  private val directQuery = OffersQueries.empty("test", OfferType.SELL, FeedType.Direct)

  "DirectOffersPrinter" should {
    "print all offers" in {
      check(forAll(OfferBuilder.offerGen) { offer =>
        val printed = offersPrinterFactory.apply(directQuery).get.print(offer)
        printed.nonEmpty
      })
    }
  }
}
