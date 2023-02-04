package ru.yandex.realty.feeds

import org.scalacheck.Gen
import ru.yandex.realty.model.feed.LandingParams.LandingType
import ru.yandex.realty.model.feed.{FeedGeneratorSiteQuery, FeedType, SitesFeedTitleTemplates}

object SitesQueries {

  def offerQueryGen: Gen[FeedGeneratorSiteQuery] =
    for {
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
      isAdjust <- Gen.oneOf(false, true)
      isListing <- Gen.oneOf(false, true)
    } yield empty("test", feedType).copy(isAdjust = isAdjust, isListing = isListing)

  def empty(name: String, feedType: FeedType): FeedGeneratorSiteQuery =
    FeedGeneratorSiteQuery(
      fileNamePrefix = name,
      feedType = feedType,
      regionIds = Seq.empty,
      isAdjust = false,
      isListing = false,
      rooms = Seq.empty,
      priceFrom = None,
      priceTo = None,
      areaFrom = None,
      areaTo = None,
      decoration = None,
      timeToMetro = None,
      metroTransport = None,
      paidOnly = None,
      onlyCompaniesIds = Seq.empty,
      excludeCompaniesIds = Seq.empty,
      usePromo = false,
      developerOffersRequired = false,
      landingParams = Seq.empty[LandingType],
      titleTemplate = SitesFeedTitleTemplates.Common,
      imageResolutionAlias = None
    )

}
