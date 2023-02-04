package ru.yandex.realty.feeds.filters

import org.junit.runner.RunWith
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.{PropertyChecks, TableDrivenPropertyChecks}
import ru.yandex.realty.feeds.OfferBuilder
import ru.yandex.realty.feeds.mock.OffersQueries
import ru.yandex.realty.feeds.services.ClickbaitFilterService
import ru.yandex.realty.feeds.services.impl.ClickbaitFilterServiceImpl
import ru.yandex.realty.geo.RegionGraphTestComponents
import ru.yandex.realty.model.feed.FeedGeneratorOfferQuery.ClickbaitFilterParams
import ru.yandex.realty.model.feed.FeedType
import ru.yandex.realty.model.offer.{CategoryType, OfferType}
import ru.yandex.realty.sites.SitesGroupingService
import ru.yandex.vertis.mockito.MockitoSupport

@RunWith(classOf[JUnitRunner])
class OfferFilterBuilderSpec
  extends WordSpec
  with Matchers
  with PropertyChecks
  with TableDrivenPropertyChecks
  with RegionGraphTestComponents
  with MockitoSupport {

  private val clickbaitFilterService: ClickbaitFilterService = mock[ClickbaitFilterServiceImpl]
  private val sitesGroupingService: SitesGroupingService = mock[SitesGroupingService]
  private val offerFilterBuilder =
    new OfferFilterBuilder(regionGraphProvider, clickbaitFilterService, sitesGroupingService)

  "OfferFilterBuilder" should {
    "builds category type filter" in {
      val offer = OfferBuilder.build()
      val query =
        OffersQueries.empty("test", OfferType.SELL, FeedType.AdWords).copy(categoryTypes = Seq(CategoryType.HOUSE))
      when(clickbaitFilterService.collect(query)).thenReturn(None)
      offerFilterBuilder.build(query).filter(offer) shouldBe false
    }

    "builds metro filter" in {
      val query = OffersQueries
        .empty("test", OfferType.SELL, FeedType.AdWords)
        .copy(timeToMetro = Some(15), metroTransport = Some("ON_FOOT"), categoryTypes = Seq(CategoryType.APARTMENT))
      when(clickbaitFilterService.collect(query)).thenReturn(None)
      val offer = OfferBuilder.build(metroMinutesOnFoot = Some(10))
      offerFilterBuilder.build(query).filter(offer) shouldBe true

      val offer2 = OfferBuilder.build(metroMinutesOnFoot = Some(20))
      offerFilterBuilder.build(query).filter(offer2) shouldBe false
    }

    "builds rid filter" in {
      val query = OffersQueries
        .empty("test", OfferType.SELL, FeedType.AdWords)
        .copy(regionIds = Seq(213), categoryTypes = Seq(CategoryType.APARTMENT))
      when(clickbaitFilterService.collect(query)).thenReturn(None)
      val offer = OfferBuilder.build()
      offerFilterBuilder.build(query).filter(offer) shouldBe true

      val offer2 = OfferBuilder.build(geoCode = 45)
      offerFilterBuilder.build(query).filter(offer2) shouldBe false
    }

    "builds isNew filter" in {
      val query = OffersQueries
        .empty("test", OfferType.SELL, FeedType.AdWords)
        .copy(isNew = Some(true), categoryTypes = Seq(CategoryType.APARTMENT))
      when(clickbaitFilterService.collect(query)).thenReturn(None)
      val offer = OfferBuilder.build()
      offerFilterBuilder.build(query).filter(offer) shouldBe true
      val query2 = OffersQueries
        .empty("test", OfferType.SELL, FeedType.AdWords)
        .copy(isNew = Some(false), categoryTypes = Seq(CategoryType.APARTMENT))
      when(clickbaitFilterService.collect(query2)).thenReturn(None)
      offerFilterBuilder.build(query2).filter(offer) shouldBe false
    }

    "builds clickbaitFilter filter" in {
      val id = 1
      val query = OffersQueries
        .empty("test", OfferType.SELL, FeedType.AdWords)
        .copy(
          categoryTypes = Seq(CategoryType.APARTMENT),
          clickbaitFilterParams = Some(ClickbaitFilterParams(1000, 0.1))
        )
      when(clickbaitFilterService.collect(query)).thenReturn(Some(Set(id.toString)))
      val clickbaitOffer = OfferBuilder.build(id = id)
      val offer = OfferBuilder.build(id = 2)
      offerFilterBuilder.build(query).filter(clickbaitOffer) shouldBe false
      offerFilterBuilder.build(query).filter(offer) shouldBe true
    }
  }

}
