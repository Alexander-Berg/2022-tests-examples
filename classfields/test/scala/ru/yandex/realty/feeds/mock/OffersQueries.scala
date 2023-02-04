package ru.yandex.realty.feeds.mock

import org.scalacheck.Gen
import ru.yandex.realty.model.feed.LandingParams.LandingType
import ru.yandex.realty.model.feed.{FeedGeneratorOfferQuery, FeedType}
import ru.yandex.realty.model.offer.{CategoryType, OfferType}
import ru.yandex.realty.model.region.Regions

object OffersQueries {

  def offerQueryGen: Gen[FeedGeneratorOfferQuery] =
    for {
      offerType <- Gen.oneOf(OfferType.RENT, OfferType.RENT)
      feedType <- Gen.oneOf(
        FeedType.DirectMarket,
        FeedType.Direct,
        FeedType.FBHomeListing,
        FeedType.MyTargetApp,
        FeedType.MyTarget,
        FeedType.Facebook,
        FeedType.Criteo,
        FeedType.AdWords
      )
      regionId <- Gen.oneOf(
        Regions.MOSCOW,
        Regions.RUSSIA,
        Regions.SPB,
        Regions.SPB_AND_LEN_OBLAST,
        Regions.MSK_AND_MOS_OBLAST
      )
      isAdjust <- Gen.oneOf(false, true)
      isListing <- Gen.oneOf(false, true)
      category <- Gen.oneOf(CategoryType.values())
    } yield FeedGeneratorOfferQuery(
      "test",
      offerType,
      feedType,
      Seq(regionId),
      isAdjust = isAdjust,
      isListing = isListing,
      categoryTypes = Seq(category),
      isNew = None,
      isNoAgent = None,
      isShortRent = None,
      rooms = Seq.empty,
      priceFrom = None,
      priceTo = None,
      areaFrom = None,
      areaTo = None,
      renovations = Seq.empty,
      timeToMetro = None,
      metroTransport = None,
      houseTypes = Seq.empty,
      distanceToMoscowInKM = None,
      qualityPortion = None,
      paidOnly = None,
      isYandexRent = None,
      landingType = Seq.empty[LandingType],
      isDeveloper = None,
      clickbaitFilterParams = None,
      imageResolutionAlias = None,
      titleTemplate = None,
      onlyCompaniesIds = Seq.empty,
      excludeCompaniesIds = Seq.empty
    )

  def empty(name: String, offerType: OfferType, feedType: FeedType): FeedGeneratorOfferQuery =
    FeedGeneratorOfferQuery(
      name,
      offerType,
      feedType,
      Seq.empty,
      isAdjust = false,
      isListing = false,
      categoryTypes = Seq.empty,
      isNew = None,
      isNoAgent = None,
      isShortRent = None,
      rooms = Seq.empty,
      priceFrom = None,
      priceTo = None,
      areaFrom = None,
      areaTo = None,
      renovations = Seq.empty,
      timeToMetro = None,
      metroTransport = None,
      houseTypes = Seq.empty,
      distanceToMoscowInKM = None,
      qualityPortion = None,
      paidOnly = None,
      isYandexRent = None,
      landingType = Seq.empty[LandingType],
      isDeveloper = None,
      clickbaitFilterParams = None,
      imageResolutionAlias = None,
      titleTemplate = None,
      onlyCompaniesIds = Seq.empty,
      excludeCompaniesIds = Seq.empty
    )

}
