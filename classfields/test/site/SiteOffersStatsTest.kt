package com.yandex.mobile.realty.test.site

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.SiteCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.*
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.viewMatchers.getResourceDimen
import com.yandex.mobile.realty.core.webserver.*
import com.yandex.mobile.realty.domain.model.Range
import com.yandex.mobile.realty.domain.model.common.CommissioningState
import com.yandex.mobile.realty.domain.model.common.Quarter
import com.yandex.mobile.realty.domain.model.common.QuarterOfYear
import com.yandex.mobile.realty.domain.model.search.Filter
import com.yandex.mobile.realty.test.BaseTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author merionkov on 07.04.2022.
 */
@RunWith(AndroidJUnit4::class)
class SiteOffersStatsTest : BaseTest() {

    private val activityTestRule = SiteCardActivityTestRule(
        siteId = SITE_ID,
        filter = Filter.SiteApartment(
            commissioningState = CommissioningState.BeingBuilt(
                date = QuarterOfYear(year = 2023, quarter = Quarter.III),
            ),
            roomsCount = setOf(
                Filter.RoomsCount.ONE,
                Filter.RoomsCount.THREE,
            ),
            price = Range.Closed(
                lower = FILTER_PRICE_DEFAULT.first,
                upper = FILTER_PRICE_DEFAULT.last,
            ),
            priceType = Filter.PriceType.PER_OFFER,
        ),
        launchActivity = false,
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(activityTestRule)

    @Test
    fun shouldShowOffersStats() {
        configureWebServer {
            registerSiteWithOffersStat()
            registerNonFilteredOfferStat()
            registerFilteredOfferStat()
            registerFilterChanges()
        }
        activityTestRule.launchActivity()
        onScreen<SiteCardScreen> {
            listView.isOffersStatsStateMatches(getTestRelatedFilePath("statsWithDefaultFilter"))
            listView.scrollTo(roomsFilterItem)
            changeStatsFilters()
            listView.isOffersStatsStateMatches(getTestRelatedFilePath("statsWithChangedFilter"))
        }
    }

    @Test
    fun shouldOpenPlansSearch() {
        configureWebServer {
            registerSiteWithOffersStat()
            registerNonFilteredOfferStat()
            registerFilteredOfferStat()
            registerFilterChanges()
            registerPlansSearch()
        }
        activityTestRule.launchActivity()
        onScreen<SiteCardScreen> {
            listView.scrollTo(showOffersButtonItem)
            listView.scrollByFloatingButtonHeight()
            changeStatsFilters()
            listView.scrollTo(plansSearchItem)
            listView.scrollByFloatingButtonHeight()
            plansSearchItem.view.click()
        }
        onScreen<DeveloperPlansScreen> {
            showFiltersButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
            filters
                .waitUntil { isCompletelyDisplayed() }
                .isViewStateMatches(getTestRelatedFilePath("plansFilters"))
        }
    }

    @Test
    fun shouldOpenOffersSearchFromFilters() {
        configureWebServer {
            registerSiteWithOffersStat()
            registerNonFilteredOfferStat()
            registerFilteredOfferStat()
            registerFilterChanges()
            registerFilteredOffersSearch()
        }
        activityTestRule.launchActivity()
        onScreen<SiteCardScreen> {
            listView.scrollTo(showOffersButtonItem)
            listView.scrollByFloatingButtonHeight()
            changeStatsFilters()
            listView.scrollVerticallyBy(getResourceDimen(R.dimen.button_large_height))
            showOffersButtonItem.view.click()
        }
        onScreen<DeveloperOffersScreen> {
            showFiltersButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
            filters
                .waitUntil { isCompletelyDisplayed() }
                .isViewStateMatches(getTestRelatedFilePath("offersWithCardFilters"))
        }
    }

    @Test
    fun shouldOpenOffersSearchFromPresets() {
        configureWebServer {
            registerSiteWithOffersStat()
            registerNonFilteredOfferStat()
            registerFilteredOfferStat()
            registerFilterChanges()
            registerPresetOffersSearch()
        }
        activityTestRule.launchActivity()
        onScreen<SiteCardScreen> {
            listView.scrollTo(showOffersButtonItem)
            listView.scrollByFloatingButtonHeight()
            changeStatsFilters()
            listView.scrollTo(resellerOffersItem)
            roomFilterPresetView("2-комнатные").click()
        }
        onScreen<DeveloperOffersScreen> {
            showFiltersButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
            filters
                .waitUntil { isCompletelyDisplayed() }
                .isViewStateMatches(getTestRelatedFilePath("offersWithPresetFilters"))
        }
    }

    @Test
    fun shouldOpenResellersSearch() {
        configureWebServer {
            registerSiteWithOffersStat()
            registerNonFilteredOfferStat()
            registerFilteredOfferStat()
            registerFilterChanges()
            registerResellerOffersSearch()
        }
        activityTestRule.launchActivity()
        onScreen<SiteCardScreen> {
            listView.scrollTo(showOffersButtonItem)
            listView.scrollByFloatingButtonHeight()
            changeStatsFilters()
            listView.scrollTo(resellerOffersItem)
            listView.scrollVerticallyBy(getResourceDimen(R.dimen.button_large_height))
            listView.scrollByFloatingButtonHeight()
            resellerOffersItem.view.click()
        }
        onScreen<SiteResellerOffersScreen> {
            listView.waitUntil { contains(offerSnippet(RESELLER_OFFER_ID)) }
        }
    }

    @Test
    fun shouldNotShowRoomsAndBuildingsFilters() {
        configureWebServer {
            registerSiteWithOffersStat()
            registerOfferStatWithoutRoomsAndBuildings()
            registerOfferStatWithoutRoomsAndBuildings()
        }
        activityTestRule.launchActivity()
        onScreen<SiteCardScreen> {
            listView.waitUntil { contains(plansSearchItem, completelyDisplayed = false) }
            listView.isOffersStatsStateMatches(getTestRelatedFilePath("statsWithPriceFilterOnly"))
        }
    }

    @Test
    fun shouldResetFilters() {
        configureWebServer {
            registerSiteWithOffersStat()
            registerNonFilteredOfferStat()
            registerFilteredEmptyOfferStat()
            registerNonFilteredOfferStat()
        }
        activityTestRule.launchActivity()
        onScreen<SiteCardScreen> {
            listView.waitUntil { contains(plansSearchItem, completelyDisplayed = false) }
            listView.isOffersStatsStateMatches(getTestRelatedFilePath("statsWithEmptyFilteredStat"))
            listView.scrollTo(resetFiltersButtonItem).click()
            listView.waitUntil { contains(showOffersButtonItem) }
            listView.isOffersStatsStateMatches(getTestRelatedFilePath("statsWithNotFilteredStat"))
        }
    }

    @Test
    fun shouldRetryAfterSiteCardError() {
        configureWebServer {
            registerSiteWithOffersStatError()
            registerSiteWithOffersStat()
            registerNonFilteredOfferStat()
            registerFilteredOfferStat()
        }
        activityTestRule.launchActivity()
        onScreen<SiteCardScreen> {
            cardError.waitUntil { listView.contains(this) }.click()
            plansSearchItem.waitUntil { listView.contains(this, completelyDisplayed = false) }
            listView.isOffersStatsStateMatches(getTestRelatedFilePath("statsWithDefaultFilter"))
        }
    }

    @Test
    fun shouldRetryAfterOffersError() {
        configureWebServer {
            registerSiteWithOffersStat()
            registerNonFilteredOfferStatError()
            registerNonFilteredOfferStat()
            registerFilteredOfferStat()
        }
        activityTestRule.launchActivity()
        onScreen<SiteCardScreen> {
            listView.waitUntil { contains(offersError) }
            listView.scrollByFloatingButtonHeight()
            offersError.view.retryButton.click()
            listView.waitUntil { contains(plansSearchItem, completelyDisplayed = false) }
            listView.isOffersStatsStateMatches(getTestRelatedFilePath("statsWithDefaultFilter"))
        }
    }

    @Test
    fun shouldDisplayShowFlatsButton() {
        configureWebServer {
            registerSiteStatWithFlats()
            registerNonFilteredOfferStat()
            registerFilteredOfferStat(fileName = "offerStatsWith1Offer")
            registerFilteredOfferStat(fileName = "offerStatsWith3Offers", rooms = setOf(1))
            registerFilteredOfferStat(fileName = "offerStatsWith5Offers", rooms = emptySet())
        }
        activityTestRule.launchActivity()
        onScreen<SiteCardScreen> {
            listView.scrollTo(showOffersButtonItem)
            showOffersButton.isTextEquals("Показать 1 квартиру")
            roomFilterButton(R.string.rooms_three).click()
            showOffersButton.isTextEquals("Показать 3 квартиры")
            roomFilterButton(R.string.rooms_one).click()
            showOffersButton.isTextEquals("Показать 5 квартир")
        }
    }

    @Test
    fun shouldDisplayShowOffersButton() {
        configureWebServer {
            registerSiteStatWithFlatsAndApartments()
            registerNonFilteredOfferStat()
            registerFilteredOfferStat(fileName = "offerStatsWith1Offer")
            registerFilteredOfferStat(fileName = "offerStatsWith3Offers", rooms = setOf(1))
            registerFilteredOfferStat(fileName = "offerStatsWith5Offers", rooms = emptySet())
        }
        activityTestRule.launchActivity()
        onScreen<SiteCardScreen> {
            listView.scrollTo(showOffersButtonItem)
            showOffersButton.isTextEquals("Показать 1 предложение")
            roomFilterButton(R.string.rooms_three).click()
            showOffersButton.isTextEquals("Показать 3 предложения")
            roomFilterButton(R.string.rooms_one).click()
            showOffersButton.isTextEquals("Показать 5 предложений")
        }
    }

    private fun SiteCardScreen.changeStatsFilters() {
        roomFilterButton(R.string.rooms_two).click()
        priceFilterItem.view.click()
        onScreen<PriceDialogScreen> {
            okButton.waitUntil { isCompletelyDisplayed() }
            valueFrom.replaceText(FILTER_PRICE_UPDATED.first.toString())
            valueTo.replaceText(FILTER_PRICE_UPDATED.last.toString())
            okButton.click()
        }
        buildingsFilterItem.view.click()
        onScreen<SiteBuildingDialogScreen> {
            buildingView(title = "Сдан", subtitle = "Корпус 5.2").click()
            buildingView(title = "2 кв. 2023", subtitle = "Корпус 6.2").click()
            buildingView(title = "4 кв. 2023", subtitle = "Корпус 6.1").click()
            okButton.click()
        }
    }

    private fun DispatcherRegistry.registerFilterChanges() {
        registerFilteredOfferStat(
            fileName = "offerStatsFilteredStep1",
            rooms = FILTER_ROOMS_UPDATED,
        )
        registerFilteredOfferStat(
            fileName = "offerStatsFilteredStep2",
            rooms = FILTER_ROOMS_UPDATED,
            price = FILTER_PRICE_UPDATED,
        )
        registerFilteredOfferStat(
            fileName = "offerStatsFilteredStep3",
            rooms = FILTER_ROOMS_UPDATED,
            price = FILTER_PRICE_UPDATED,
            houseIds = FILTER_HOUSE_IDS_UPDATED,
        )
    }

    private fun DispatcherRegistry.registerSiteWithOffersStat() {
        register(
            request {
                method("GET")
                path("1.0/siteWithOffersStat.json")
                queryParam("siteId", SITE_ID)
            },
            response {
                assetBody("siteWithOfferStat.json")
            },
        )
    }

    private fun DispatcherRegistry.registerSiteWithOffersStatError() {
        register(
            request {
                method("GET")
                path("1.0/siteWithOffersStat.json")
                queryParam("siteId", SITE_ID)
            },
            response {
                error()
            },
        )
    }

    private fun DispatcherRegistry.registerSiteStatWithFlats() {
        register(
            request {
                method("GET")
                path("1.0/siteWithOffersStat.json")
                queryParam("siteId", SITE_ID)
            },
            response {
                assetBody("SiteOffersStatsTest/siteStatWithFlats.json")
            },
        )
    }

    private fun DispatcherRegistry.registerSiteStatWithFlatsAndApartments() {
        register(
            request {
                method("GET")
                path("1.0/siteWithOffersStat.json")
                queryParam("siteId", SITE_ID)
            },
            response {
                assetBody("SiteOffersStatsTest/siteStatWithFlatsAndApartments.json")
            },
        )
    }

    private fun DispatcherRegistry.registerPlansSearch() {
        register(
            request {
                method("GET")
                path("2.0/site/$SITE_ID/planSearch")
                queryParam("priceType", "PER_OFFER")
                queryParam("priceMin", FILTER_PRICE_DEFAULT.first.toString())
                queryParam("priceMax", FILTER_PRICE_DEFAULT.last.toString())
                FILTER_ROOMS_DEFAULT.forEach { room ->
                    queryParam("roomsTotal", room.toString())
                }
                FILTER_HOUSE_IDS_DEFAULT.forEach { houseId ->
                    queryParam("houseId", houseId)
                }
            },
            response {
                assetBody("SiteOffersStatsTest/searchPlans.json")
            },
        )
        registerNonFilteredOfferStat()
    }

    private fun DispatcherRegistry.registerFilteredOffersSearch() {
        register(
            request {
                method("GET")
                path("1.0/offerWithSiteSearch.json")
                queryParam("siteId", SITE_ID)
                queryParam("priceType", "PER_OFFER")
                queryParam("priceMin", FILTER_PRICE_UPDATED.first.toString())
                queryParam("priceMax", FILTER_PRICE_UPDATED.last.toString())
                FILTER_ROOMS_UPDATED.forEach { room ->
                    queryParam("roomsTotal", room.toString())
                }
                FILTER_HOUSE_IDS_UPDATED.forEach { houseId ->
                    queryParam("houseId", houseId)
                }
            },
            response {
                assetBody("SiteOffersStatsTest/searchOffers.json")
            },
        )
        registerNonFilteredOfferStat()
    }

    private fun DispatcherRegistry.registerPresetOffersSearch() {
        register(
            request {
                method("GET")
                path("1.0/offerWithSiteSearch.json")
                queryParam("siteId", SITE_ID)
                queryParam("roomsTotal", "2")
                excludeQueryParamKey("priceMin")
                excludeQueryParamKey("priceMax")
                excludeQueryParamKey("houseId")
            },
            response {
                assetBody("SiteOffersStatsTest/searchOffers.json")
            },
        )
        registerNonFilteredOfferStat()
    }

    private fun DispatcherRegistry.registerResellerOffersSearch() {
        register(
            request {
                method("GET")
                path("1.0/offerWithSiteSearch.json")
                queryParam("siteId", SITE_ID)
                excludeQueryParamKey("roomsTotal")
                excludeQueryParamKey("priceMin")
                excludeQueryParamKey("priceMax")
                excludeQueryParamKey("houseId")
            },
            response {
                assetBody("SiteOffersStatsTest/searchOffers.json")
            },
        )
    }

    private fun DispatcherRegistry.registerFilteredOfferStat(
        fileName: String = "offerStatsFilteredStep0",
        price: LongRange = FILTER_PRICE_DEFAULT,
        rooms: Set<Int> = FILTER_ROOMS_DEFAULT,
        houseIds: Set<String> = FILTER_HOUSE_IDS_DEFAULT,
    ) {
        register(
            request {
                method("GET")
                path("2.0/site/$SITE_ID/offerStat")
                queryParam("priceType", "PER_OFFER")
                queryParam("priceMin", price.first.toString())
                queryParam("priceMax", price.last.toString())
                rooms.forEach { room -> queryParam("roomsTotal", room.toString()) }
                houseIds.forEach { houseId -> queryParam("houseId", houseId) }
            },
            response {
                assetBody("SiteOffersStatsTest/$fileName.json")
            },
        )
    }

    private fun DispatcherRegistry.registerFilteredEmptyOfferStat() {
        register(
            request {
                method("GET")
                path("2.0/site/$SITE_ID/offerStat")
                queryParam("priceType", "PER_OFFER")
                queryParam("priceMin", FILTER_PRICE_DEFAULT.first.toString())
                queryParam("priceMax", FILTER_PRICE_DEFAULT.last.toString())
                FILTER_ROOMS_DEFAULT.forEach { room ->
                    queryParam("roomsTotal", room.toString())
                }
                FILTER_HOUSE_IDS_DEFAULT.forEach { houseId ->
                    queryParam("houseId", houseId)
                }
            },
            response {
                assetBody("SiteOffersStatsTest/offerStatsFilteredEmpty.json")
            },
        )
    }

    private fun DispatcherRegistry.registerNonFilteredOfferStat() {
        register(
            request {
                method("GET")
                path("2.0/site/$SITE_ID/offerStat")
                excludeQueryParamKey("priceMin")
                excludeQueryParamKey("priceMax")
                excludeQueryParamKey("roomsTotal")
                excludeQueryParamKey("houseId")
            },
            response {
                assetBody("SiteOffersStatsTest/offerStatsNonFiltered.json")
            },
        )
    }

    private fun DispatcherRegistry.registerNonFilteredOfferStatError() {
        register(
            request {
                method("GET")
                path("2.0/site/$SITE_ID/offerStat")
                excludeQueryParamKey("priceMin")
                excludeQueryParamKey("priceMax")
                excludeQueryParamKey("roomsTotal")
                excludeQueryParamKey("houseId")
            },
            response {
                error()
            },
        )
    }

    private fun DispatcherRegistry.registerOfferStatWithoutRoomsAndBuildings() {
        register(
            request {
                method("GET")
                path("2.0/site/$SITE_ID/offerStat")
            },
            response {
                assetBody("SiteOffersStatsTest/offerStatsWithoutRoomsAndBuildings.json")
            },
        )
    }

    private companion object {
        const val SITE_ID = "1"
        const val RESELLER_OFFER_ID = "0"
        val FILTER_PRICE_DEFAULT = 2_500_000L..7_500_000L
        val FILTER_PRICE_UPDATED = 3_500_000L..8_500_000L
        val FILTER_ROOMS_DEFAULT: Set<Int> = setOf(1, 3)
        val FILTER_ROOMS_UPDATED: Set<Int> = setOf(1, 2, 3)
        val FILTER_HOUSE_IDS_DEFAULT: Set<String> = setOf("1", "2")
        val FILTER_HOUSE_IDS_UPDATED: Set<String> = setOf("0")
    }
}
