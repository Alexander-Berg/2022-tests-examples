package ru.yandex.auto.extdata.jobs.feeds.feed.utils

import ru.yandex.auto.extdata.jobs.feeds.feed.FeedProperties
import ru.yandex.auto.extdata.jobs.feeds.feed.FeedProperties.OfferUrlDirectly
import ru.yandex.auto.extdata.jobs.feeds.feed.LandingUrlType.Non

/**
  * Created by theninthowl on 12/13/21
  */
trait FeedPropertiesTest {
  protected val properties: FeedProperties = FeedProperties(
    service = "test",
    feedType = "test-feed",
    request = "",
    maxNumberOfOffers = Int.MaxValue,
    adjustUrlFormat = true,
    fileName = "test-file",
    recipient = "",
    useSimilarIds = true,
    listingWithoutGeneration = false,
    badgesRate = None,
    badgesPriority = Seq(),
    feedUrlProp = OfferUrlDirectly,
    onlyForDealers = Set(),
    photoClassPriority = Map(),
    forceListing = false,
    landingUrlType = Non,
    clickbaitFilter = None,
    descriptionTemplate = None,
    hasOnDealers = false,
    showDiscount = false,
    onlyAuction = false,
    minModelsCount = 0
  )
}
