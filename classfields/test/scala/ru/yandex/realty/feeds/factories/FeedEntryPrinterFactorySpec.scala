package ru.yandex.realty.feeds.factories

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.{PropertyChecks, TableDrivenPropertyChecks}
import ru.yandex.realty.sites.SitesGroupingService
import ru.yandex.extdata.core.lego.Provider
import ru.yandex.realty.feeds.mock.OffersQueries
import ru.yandex.realty.feeds.printers.direct.{DirectMarketOffersPrinter, DirectOffersPrinter}
import ru.yandex.realty.feeds.printers.{
  AdWordsOffersPrinter,
  CriteoOffersPrinter,
  FBHomeListingOfferPrinter,
  FacebookOffersPrinter,
  FeedEntryPrinter,
  MyTargetAppOffersPrinter,
  MyTargetOffersPrinter
}
import ru.yandex.realty.geo.RegionGraphTestComponents
import ru.yandex.realty.model.feed.FeedType
import ru.yandex.realty.model.offer.{Offer, OfferType}
import ru.yandex.realty.picapica.MdsUrlBuilder
import ru.yandex.realty.storage.CanonicalUrlsStorage

@RunWith(classOf[JUnitRunner])
class FeedEntryPrinterFactorySpec
  extends WordSpec
  with MockFactory
  with Matchers
  with PropertyChecks
  with TableDrivenPropertyChecks
  with RegionGraphTestComponents {

  private val mockSites = mock[SitesGroupingService]
  private def provider[T](t: T): Provider[T] = () => t
  private val urlStorage = new CanonicalUrlsStorage(Map.empty)

  private val mdsBuilder = new MdsUrlBuilder("//realty:80")
  private val offersPrinterFactory =
    new OffersFeedPrinterFactory(mockSites, provider(urlStorage), regionGraphProvider, mdsBuilder)

  "OffersFeedPrinterFactory" should {
    "correct returns printer" in {
      def getPrinter(feedType: FeedType): Option[FeedEntryPrinter[Offer]] =
        offersPrinterFactory(OffersQueries.empty("test", OfferType.SELL, feedType))
      getPrinter(FeedType.AdWords).get shouldBe a[AdWordsOffersPrinter]
      getPrinter(FeedType.Criteo).get shouldBe a[CriteoOffersPrinter]
      getPrinter(FeedType.Direct).get shouldBe a[DirectOffersPrinter]
      getPrinter(FeedType.Facebook).get shouldBe a[FacebookOffersPrinter]
      getPrinter(FeedType.MyTarget).get shouldBe a[MyTargetOffersPrinter]
      getPrinter(FeedType.MyTargetApp).get shouldBe a[MyTargetAppOffersPrinter]
      getPrinter(FeedType.FBHomeListing).get shouldBe a[FBHomeListingOfferPrinter]
      getPrinter(FeedType.DirectMarket).get shouldBe a[DirectMarketOffersPrinter]
    }

  }

}
