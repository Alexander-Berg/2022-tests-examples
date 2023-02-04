package ru.yandex.realty.feeds.printers

import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.{PropertyChecks, TableDrivenPropertyChecks}
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
class AdWordsPrinterSpec
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

  private val adWordQuery = OffersQueries.empty("test", OfferType.SELL, FeedType.AdWords)

  "AdWordsPrinter" should {
    "print offer" in {
      val printed = offersPrinterFactory.apply(adWordQuery).get.print(OfferBuilder.build())
      val exp = "0,\"Квартира, 100 м²\",https://realty.test.vertis.yandex.ru/offer/0/,,Москва,,sell_new"
      printed shouldBe Some(exp)
    }
  }

}
