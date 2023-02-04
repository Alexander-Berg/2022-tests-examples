package ru.auto.ara.test.listing

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.search_offers.OfferLocatorCountersResponse
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher.Companion.isSearchRequestParamsWithExcludeRadiusOrRid
import ru.auto.ara.core.dispatchers.search_offers.getOfferCount
import ru.auto.ara.core.dispatchers.search_offers.getOfferLocatorCounters
import ru.auto.ara.core.dispatchers.search_offers.postSearchOffers
import ru.auto.ara.core.robot.searchfeed.checkScreenshotListingOffers
import ru.auto.ara.core.robot.searchfeed.checkScreenshotSearchFeed
import ru.auto.ara.core.robot.searchfeed.checkSearchFeed
import ru.auto.ara.core.robot.searchfeed.performFeedSort
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.di.GeoSuggestRule
import ru.auto.ara.core.rules.mock.ImmediateImageLoaderRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.data.model.filter.StateGroup
import ru.auto.data.network.scala.response.OfferListingResponse

@RunWith(AndroidJUnit4::class)
class EndlessListingTest : ListingTestSetup() {

    private val webServerRule = WebServerRule()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        ImmediateImageLoaderRule.mockPhotos(),
        activityRule,
        DisableAdsRule(),
        SetPreferencesRule(),
        GeoSuggestRule(geoArgs)
    )

    @Test
    fun shouldShowEndlessListingIfEmptyListOfAllFeedsAndDistanceWithoutRound() {
        webServerRule.routing {
            getOfferCount(count = 0)
            postSearchOffers("listing_offers/empty_feed_cars.json")
            postSearchOffers(
                bodyMatcher = isSearchRequestParamsWithExcludeRadiusOrRid(),
                mapper = { mapPagination(2).mapDistance(421) }
            ).watch { checkRequestWasCalled() }
            getOfferLocatorCounters(OfferLocatorCountersResponse.DEFAULT)
        }

        openCarsSearchFeed(StateGroup.ALL)

        performSearchFeed { scrollToEndlessListingTitle() }

        checkScreenshotSearchFeed {
            isEndlessListingTitleScreenshotSame("five_offers")
        }

        performSearchFeed { scrollToEnd() }

        checkScreenshotListingOffers {
            isFooterScreenshotSame("/listing/endless/footer_without_round.png")
        }
    }

    @Test
    fun shouldShowOfferDistanceWithoutRoundWhenOfferFromEndlessListing() {
        webServerRule.routing {
            getOfferCount(count = 0)
            postSearchOffers("listing_offers/empty_feed_cars.json")
            postSearchOffers(
                bodyMatcher = isSearchRequestParamsWithExcludeRadiusOrRid(),
                mapper = { mapPagination(2).mapDistance(421) }
            )
            getOfferLocatorCounters(OfferLocatorCountersResponse.DEFAULT)
        }

        openCarsSearchFeed(StateGroup.ALL)

        performSearchFeed {
            scrollToGeoRadiusFromBubbles("Москва")
            clickGeoRadiusItem(0)
            waitFirstPageLoaded(1)
            scrollToEnd()
        }

        checkScreenshotListingOffers {
            isFooterScreenshotSame("/listing/endless/footer_without_round_radius_0.png")
        }
    }

    @Test
    fun shouldShowEndlessListingIfEmptyListOfNewFeeds() {
        webServerRule.routing {
            getOfferCount(count = 0)
            postSearchOffers("listing_offers/empty_feed_cars.json")
            postSearchOffers(bodyMatcher = isSearchRequestParamsWithExcludeRadiusOrRid()).watch { checkRequestWasCalled() }
            getOfferLocatorCounters(OfferLocatorCountersResponse.DEFAULT)
        }

        openCarsSearchFeed(StateGroup.NEW)

        performSearchFeed { scrollToEndlessListingTitle() }

        checkScreenshotSearchFeed { isEndlessListingTitleScreenshotSame("five_offers") }
    }

    @Test
    fun shouldShowEndlessListingIfEmptyListOfUsedFeeds() {
        webServerRule.routing {
            getOfferCount(count = 0)
            postSearchOffers("listing_offers/empty_feed_cars.json")
            postSearchOffers(bodyMatcher = isSearchRequestParamsWithExcludeRadiusOrRid()).watch { checkRequestWasCalled() }
            getOfferLocatorCounters(OfferLocatorCountersResponse.DEFAULT)
        }

        openCarsSearchFeed(StateGroup.USED)

        performSearchFeed { scrollToEndlessListingTitle() }

        checkScreenshotSearchFeed { isEndlessListingTitleScreenshotSame("five_offers") }
    }

    @Test
    fun shouldShowEndlessListingIfNotEmptyListOfAllFeeds() {
        webServerRule.routing {
            getOfferCount(count = 2)
            postSearchOffers(mapper = { mapPagination(2) })
            postSearchOffers(bodyMatcher = isSearchRequestParamsWithExcludeRadiusOrRid()).watch { checkRequestWasCalled() }
            getOfferLocatorCounters(OfferLocatorCountersResponse.DEFAULT)
        }

        openCarsSearchFeed(StateGroup.ALL)

        performSearchFeed { scrollToEndlessListingTitle() }

        checkScreenshotSearchFeed { isEndlessListingTitleScreenshotSame("five_offers") }
    }

    @Test
    fun shouldShowEndlessListingIfNotEmptyListOfUsedFeeds() {
        webServerRule.routing {
            getOfferCount(count = 2)
            postSearchOffers(mapper = { mapPagination(2) })
            postSearchOffers(bodyMatcher = isSearchRequestParamsWithExcludeRadiusOrRid()).watch { checkRequestWasCalled() }
            getOfferLocatorCounters(OfferLocatorCountersResponse.DEFAULT)
        }

        openCarsSearchFeed(StateGroup.USED)

        performSearchFeed { scrollToEndlessListingTitle() }

        checkScreenshotSearchFeed { isEndlessListingTitleScreenshotSame("five_offers") }
    }

    @Test
    fun shouldShowEndlessListingIfTwoPagesOfAllFeedsAndSortingAddedToEndlessListing() {
        webServerRule.routing {
            getOfferCount(count = 40)
            postSearchOffers(assetPath = "listing_offers/generic_feed_cars_20_offers.json", page = 0)
            postSearchOffers(
                assetPath = "listing_offers/generic_feed_cars_20_offers.json",
                page = 1,
                mapper = { mapPagination(40) }
            )
            postSearchOffers(
                sort = "cr_date-desc",
                bodyMatcher = isSearchRequestParamsWithExcludeRadiusOrRid()
            ).watch { checkRequestWasCalled() }
            getOfferLocatorCounters(OfferLocatorCountersResponse.DEFAULT)
        }

        openCarsSearchFeed(StateGroup.ALL)

        performSearchFeed {
            openSort()
        }

        performFeedSort {
            selectSort("Дате размещения")
        }

        performSearchFeed {
            scrollToEnd()
            waitFirstPage()
            scrollToEndlessListingTitle()
        }

        checkScreenshotSearchFeed { isEndlessListingTitleScreenshotSame("five_offers") }
    }

    @Test
    fun shouldShowEndlessListingIfEmptyListOfAllFeedsForRegion() {
        webServerRule.routing {
            getOfferCount(count = 0)
            postSearchOffers("listing_offers/empty_feed_cars.json")
            postSearchOffers(
                bodyMatcher = isSearchRequestParamsWithExcludeRadiusOrRid(),
            ).watch { checkRequestWasCalled() }
            getOfferLocatorCounters(OfferLocatorCountersResponse.DEFAULT)
        }

        openCarsSearchFeed(StateGroup.ALL, geoRadiusSupport = false, MOSCOW_AREA)

        performSearchFeed { scrollToEndlessListingTitle() }

        checkScreenshotSearchFeed { isEndlessListingTitleScreenshotSame("five_offers") }
    }

    @Test
    fun shouldShowEndlessListingIfNotEmptyListOfAllFeedsForRegion() {
        webServerRule.routing {
            getOfferCount(count = 0)
            postSearchOffers(mapper = { mapPagination(2) })
            postSearchOffers(
                bodyMatcher = isSearchRequestParamsWithExcludeRadiusOrRid(),
            ).watch { checkRequestWasCalled() }
            getOfferLocatorCounters(OfferLocatorCountersResponse.DEFAULT)
        }

        openCarsSearchFeed(StateGroup.ALL, geoRadiusSupport = false, MOSCOW_AREA)

        performSearchFeed { scrollToEndlessListingTitle() }

        checkScreenshotSearchFeed { isEndlessListingTitleScreenshotSame("five_offers") }
    }

    @Test
    fun shouldNotShowEndlessListingIfNotEmptyListOfNewFeeds() {
        webServerRule.routing {
            getOfferCount(count = 2)
            postSearchOffers(mapper = { mapPagination(2) })
            postSearchOffers(bodyMatcher = isSearchRequestParamsWithExcludeRadiusOrRid()).watch { checkRequestWasNotCalled() }
            getOfferLocatorCounters(OfferLocatorCountersResponse.DEFAULT).watch { checkCallOnlyForGeoRadius() }
        }

        openCarsSearchFeed(StateGroup.NEW)

        performSearchFeed { scrollToEnd() }

        checkSearchFeed { isEndlessListingTitleNotDisplayed() }
    }

    @Test
    fun shouldNotShowEndlessListingIfEmptyListOfMotoFeeds() {
        webServerRule.routing {
            getOfferCount(category = "moto", count = 0)
            postSearchOffers(category = "moto", mapper = { mapPagination(2) })
            postSearchOffers(
                category = "moto",
                bodyMatcher = isSearchRequestParamsWithExcludeRadiusOrRid()
            ).watch { checkRequestWasNotCalled() }
            getOfferLocatorCounters(OfferLocatorCountersResponse.DEFAULT).watch { checkCallOnlyForGeoRadius() }
        }

        openMotoSearchFeed()

        performSearchFeed { scrollToEnd() }

        checkSearchFeed { isEndlessListingTitleNotDisplayed() }
    }

    @Test
    fun shouldNotShowEndlessListingIfEmptyListOfTrucksFeeds() {
        webServerRule.routing {
            getOfferCount(category = "trucks", count = 0)
            postSearchOffers(category = "trucks", mapper = { mapPagination(2) })
            postSearchOffers(
                category = "trucks",
                bodyMatcher = isSearchRequestParamsWithExcludeRadiusOrRid()
            ).watch { checkRequestWasNotCalled() }
            getOfferLocatorCounters(OfferLocatorCountersResponse.DEFAULT).watch { checkCallOnlyForGeoRadius() }
        }

        openTrucksSearchFeed()

        performSearchFeed { scrollToEnd() }

        checkSearchFeed { isEndlessListingTitleNotDisplayed() }
    }

    @Test
    fun shouldNotShowEndlessListingIfManyRegions() {
        webServerRule.routing {
            getOfferCount(count = 0)
            postSearchOffers(mapper = { mapPagination(2) })
            postSearchOffers(
                bodyMatcher = isSearchRequestParamsWithExcludeRadiusOrRid(),
            ).watch { checkRequestWasNotCalled() }
            getOfferLocatorCounters(OfferLocatorCountersResponse.DEFAULT).watch { checkRequestWasNotCalled() }
        }

        openCarsSearchFeed(StateGroup.ALL, geoRadiusSupport = true, MOSCOW, SAINT_PETERSBURG)

        performSearchFeed { scrollToEnd() }

        checkSearchFeed { isEndlessListingTitleNotDisplayed() }
    }

    private fun OfferListingResponse.mapDistance(distance: Int): OfferListingResponse =
        this.copy(offers = this.offers?.map { offer ->
            offer.copy(seller = offer.seller?.copy(
                location = offer.seller?.location?.copy(
                    distance_to_selected_geo = offer.seller?.location?.distance_to_selected_geo?.map { geo ->
                        geo.copy(distance = distance)
                    }
                ))
            )
        })

    private fun OfferListingResponse.mapPagination(count: Int): OfferListingResponse =
        this.copy(pagination = this.pagination?.copy(total_offers_count = count, total_page_count = this.pagination?.page))

    private fun RequestWatcher.checkCallOnlyForGeoRadius() = checkRequestsCount(1)

}
