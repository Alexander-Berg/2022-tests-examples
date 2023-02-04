package com.yandex.mobile.realty.test.site

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.DeveloperPlansActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.AreaDialogScreen
import com.yandex.mobile.realty.core.screen.BathroomDialogScreen
import com.yandex.mobile.realty.core.screen.DeveloperPlansScreen
import com.yandex.mobile.realty.core.screen.FloorDialogScreen
import com.yandex.mobile.realty.core.screen.OfferCardScreen
import com.yandex.mobile.realty.core.screen.PlanOffersScreen
import com.yandex.mobile.realty.core.screen.PriceDialogScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SiteBuildingDialogScreen
import com.yandex.mobile.realty.core.screen.SiteSortingDialogScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.error
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.domain.model.Range
import com.yandex.mobile.realty.domain.model.search.Filter
import com.yandex.mobile.realty.domain.model.site.HousingType
import com.yandex.mobile.realty.domain.model.site.SiteSubFilter
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.search.Sorting
import okhttp3.mockwebserver.MockResponse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author merionkov on 07.04.2022.
 */
@RunWith(AndroidJUnit4::class)
class DeveloperPlansTest : BaseTest() {

    private val activityTestRule = DeveloperPlansActivityTestRule(
        siteId = SITE_ID,
        siteName = SITE_NAME,
        launchActivity = false,
    )

    private val authorizationRule = AuthorizationRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(activityTestRule, authorizationRule)

    @Test
    fun shouldShowFilteredContent() {
        configureWebServer {
            registerOfferStat()
            var params = SearchParams(price = FILTER_PRICE_DEFAULT, rooms = FILTER_ROOMS_DEFAULT)
            registerPlansSearch(params, responseFileName = "plansSearch0")
            params = params.copy(rooms = FILTER_ROOMS_UPDATED)
            registerPlansSearch(params, responseFileName = "plansSearch1")
            params = params.copy(price = FILTER_PRICE_UPDATED)
            registerPlansSearch(params, responseFileName = "plansSearch2")
            params = params.copy(houseIds = FILTER_BUILDINGS_UPDATED)
            registerPlansSearch(params, responseFileName = "plansSearch3")
            params = params.copy(area = FILTER_AREA_UPDATED)
            registerPlansSearch(params, responseFileName = "plansSearch4")
            params = params.copy(kitchenArea = FILTER_KITCHEN_AREA_UPDATED)
            registerPlansSearch(params, responseFileName = "plansSearch5")
            params = params.copy(bathroomUnit = FILTER_BATHROOM_UNIT_UPDATED)
            registerPlansSearch(params, responseFileName = "plansSearch6")
            params = params.copy(floor = FILTER_FLOOR_UPDATED)
            registerPlansSearch(params, responseFileName = "plansSearch7")
        }
        activityTestRule.filter = SiteSubFilter(
            roomsCount = setOf(
                Filter.RoomsCount.ONE,
                Filter.RoomsCount.THREE,
            ),
            priceRange = Range.Closed(
                lower = FILTER_PRICE_DEFAULT.first,
                upper = FILTER_PRICE_DEFAULT.last,
            ),
            priceType = Filter.PriceType.PER_OFFER,
        )
        activityTestRule.launchActivity()
        onScreen<DeveloperPlansScreen> {
            snippetView("0").waitUntil { contentListView.contains(this) }
            showFiltersButton.waitUntil { isCompletelyDisplayed() }.click()
            bottomSheet.waitUntil { isCollapsed() }
            root.isViewStateMatches(getTestRelatedFilePath("initialFilters"))

            roomFilterButton(R.string.rooms_two).click()
            snippetView("1").waitUntil {
                contentListView.contains(this, completelyDisplayed = false)
            }

            textFilterButton(R.string.price_filter).click()
            onScreen<PriceDialogScreen> {
                okButton.waitUntil { isCompletelyDisplayed() }
                valueFrom.replaceText(FILTER_PRICE_UPDATED.first.toString())
                valueTo.replaceText(FILTER_PRICE_UPDATED.last.toString())
                okButton.click()
            }
            snippetView("2").waitUntil {
                contentListView.contains(this, completelyDisplayed = false)
            }

            textFilterButton(R.string.site_building_filter).click()
            onScreen<SiteBuildingDialogScreen> {
                buildingView(title = "2 кв. 2023", subtitle = "Корпус 6.2")
                    .waitUntil { isCompletelyDisplayed() }
                    .click()
                okButton.click()
            }
            snippetView("3").waitUntil {
                contentListView.contains(this, completelyDisplayed = false)
            }

            textFilterButton(R.string.area_total_full_title).click()
            onScreen<AreaDialogScreen> {
                okButton.waitUntil { isCompletelyDisplayed() }
                valueFrom.replaceText(FILTER_AREA_UPDATED.first.toString())
                valueTo.replaceText(FILTER_AREA_UPDATED.last.toString())
                okButton.click()
            }
            snippetView("4").waitUntil {
                contentListView.contains(this, completelyDisplayed = false)
            }

            textFilterButton(R.string.area_kitchen).click()
            onScreen<AreaDialogScreen> {
                okButton.waitUntil { isCompletelyDisplayed() }
                valueFrom.replaceText(FILTER_KITCHEN_AREA_UPDATED.first.toString())
                valueTo.replaceText(FILTER_KITCHEN_AREA_UPDATED.last.toString())
                okButton.click()
            }
            snippetView("5").waitUntil {
                contentListView.contains(this, completelyDisplayed = false)
            }

            textFilterButton(R.string.bathroom_title).click()
            onScreen<BathroomDialogScreen> {
                separatedSelectorView
                    .waitUntil { isCompletelyDisplayed() }
                    .click()
            }
            snippetView("6").waitUntil {
                contentListView.contains(this, completelyDisplayed = false)
            }

            textFilterButton(R.string.floor).click()
            onScreen<FloorDialogScreen> {
                okButton.waitUntil { isCompletelyDisplayed() }
                valueFrom.replaceText(FILTER_FLOOR_UPDATED.first.toString())
                valueTo.replaceText(FILTER_FLOOR_UPDATED.last.toString())
                okButton.click()
            }
            snippetView("7").waitUntil {
                contentListView.contains(this, completelyDisplayed = false)
            }

            root.isViewStateMatches(getTestRelatedFilePath("finalFilters"))
            hideFiltersButton.click()
            bottomSheet.waitUntil { isExpanded() }
            root.isViewStateMatches(getTestRelatedFilePath("content"))
        }
    }

    @Test
    fun shouldShowSortedContent() {
        configureWebServer {
            registerOfferStat()
            var params = SearchParams(sorting = Sorting.PRICE.value)
            registerPlansSearch(params, responseFileName = "plansSearch0")
            params = SearchParams(sorting = Sorting.AREA_DESC.value)
            registerPlansSearch(params, responseFileName = "plansSearch1")
            params = SearchParams(sorting = Sorting.PRICE_PER_SQUARE.value)
            registerPlansSearch(params, responseFileName = "plansSearch2")
        }
        activityTestRule.launchActivity()
        onScreen<DeveloperPlansScreen> {

            snippetView("0").waitUntil { contentListView.contains(this) }
            sortingView.click()
            onScreen<SiteSortingDialogScreen> {
                price.waitUntil { isChecked() }
                areaDesc.waitUntil { isCompletelyDisplayed() }.click()
            }

            snippetView("1").waitUntil { contentListView.contains(this) }
            sortingView.click()
            onScreen<SiteSortingDialogScreen> {
                areaDesc.waitUntil { isChecked() }
                pricePerMeter.waitUntil { isCompletelyDisplayed() }.click()
            }

            snippetView("2").waitUntil { contentListView.contains(this) }
            sortingView.click()
            onScreen<SiteSortingDialogScreen> {
                pricePerMeter.waitUntil { isChecked() }
            }
        }
    }

    @Test
    fun shouldOpenOfferScreen() {
        configureWebServer {
            registerOfferStat()
            registerPlansSearch(responseFileName = "plansSearch7")
        }
        activityTestRule.launchActivity()
        onScreen<DeveloperPlansScreen> {
            snippetView("7").waitUntil { contentListView.contains(this) }.click()
        }
        onScreen<OfferCardScreen> {
            galleryView.waitUntil { isCompletelyDisplayed() }
        }
    }

    @Test
    fun shouldOpenOffersListScreen() {
        configureWebServer {
            registerOfferStat()
            var params = SearchParams(price = FILTER_PRICE_DEFAULT, rooms = FILTER_ROOMS_DEFAULT)
            registerPlansSearch(params, responseFileName = "plansSearch6")
            params = params.copy(area = FILTER_AREA_UPDATED)
            registerPlansSearch(params, responseFileName = "plansSearch7")
            registerFilteredOffersSearch()
        }
        activityTestRule.filter = SiteSubFilter(
            roomsCount = setOf(
                Filter.RoomsCount.ONE,
                Filter.RoomsCount.THREE,
            ),
            priceRange = Range.Closed(
                lower = FILTER_PRICE_DEFAULT.first,
                upper = FILTER_PRICE_DEFAULT.last,
            ),
            priceType = Filter.PriceType.PER_OFFER,
        )
        activityTestRule.launchActivity()
        onScreen<DeveloperPlansScreen> {
            snippetView("6").waitUntil { contentListView.contains(this) }
            showFiltersButton.waitUntil { isCompletelyDisplayed() }.click()
            bottomSheet.waitUntil { isCollapsed() }
            textFilterButton(R.string.area_total_full_title).click()
            onScreen<AreaDialogScreen> {
                okButton.waitUntil { isCompletelyDisplayed() }
                valueFrom.replaceText(FILTER_AREA_UPDATED.first.toString())
                valueTo.replaceText(FILTER_AREA_UPDATED.last.toString())
                okButton.click()
            }
            hideFiltersButton.waitUntil { isCompletelyDisplayed() }.click()
            bottomSheet.waitUntil { isExpanded() }
            snippetView("8").waitUntil { contentListView.contains(this) }.click()
        }
        onScreen<PlanOffersScreen> {
            planOfferSnippet("0").waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldResetFiltersIfResultIsEmpty() {
        configureWebServer {
            registerOfferStat()
            var params = SearchParams(price = FILTER_PRICE_DEFAULT, rooms = FILTER_ROOMS_DEFAULT)
            registerPlansSearch(params, responseFileName = "plansSearch0")
            params = params.copy(rooms = FILTER_ROOMS_UPDATED)
            registerPlansSearch(params, responseFileName = "plansSearch1")
            params = params.copy(price = FILTER_PRICE_UPDATED)
            registerPlansSearch(params, responseFileName = "plansSearch2")
            params = params.copy(houseIds = FILTER_BUILDINGS_UPDATED)
            registerPlansSearch(params, responseFileName = "plansSearch3")
            params = params.copy(area = FILTER_AREA_UPDATED)
            registerPlansSearch(params, responseFileName = "plansSearch4")
            params = params.copy(kitchenArea = FILTER_KITCHEN_AREA_UPDATED)
            registerPlansSearch(params, responseFileName = "plansSearch5")
            params = params.copy(bathroomUnit = FILTER_BATHROOM_UNIT_UPDATED)
            registerPlansSearch(params, responseFileName = "plansSearch6")
            params = params.copy(floor = FILTER_FLOOR_UPDATED)
            registerPlansSearch(params, responseFileName = "plansSearchEmpty")
            params = SearchParams()
            registerPlansSearch(params, responseFileName = "plansSearch1")
        }
        activityTestRule.filter = SiteSubFilter(
            roomsCount = setOf(
                Filter.RoomsCount.ONE,
                Filter.RoomsCount.THREE,
            ),
            priceRange = Range.Closed(
                lower = FILTER_PRICE_DEFAULT.first,
                upper = FILTER_PRICE_DEFAULT.last,
            ),
            priceType = Filter.PriceType.PER_OFFER,
        )
        activityTestRule.launchActivity()
        onScreen<DeveloperPlansScreen> {
            snippetView("0").waitUntil { contentListView.contains(this) }
            showFiltersButton.waitUntil { isCompletelyDisplayed() }.click()

            roomFilterButton(R.string.rooms_two).click()
            snippetView("1").waitUntil {
                contentListView.contains(this, completelyDisplayed = false)
            }

            textFilterButton(R.string.price_filter).click()
            onScreen<PriceDialogScreen> {
                okButton.waitUntil { isCompletelyDisplayed() }
                valueFrom.replaceText(FILTER_PRICE_UPDATED.first.toString())
                valueTo.replaceText(FILTER_PRICE_UPDATED.last.toString())
                okButton.click()
            }
            snippetView("2").waitUntil {
                contentListView.contains(this, completelyDisplayed = false)
            }

            textFilterButton(R.string.site_building_filter).click()
            onScreen<SiteBuildingDialogScreen> {
                buildingView(title = "2 кв. 2023", subtitle = "Корпус 6.2")
                    .waitUntil { isCompletelyDisplayed() }
                    .click()
                okButton.click()
            }
            snippetView("3").waitUntil {
                contentListView.contains(this, completelyDisplayed = false)
            }

            textFilterButton(R.string.area_total_full_title).click()
            onScreen<AreaDialogScreen> {
                okButton.waitUntil { isCompletelyDisplayed() }
                valueFrom.replaceText(FILTER_AREA_UPDATED.first.toString())
                valueTo.replaceText(FILTER_AREA_UPDATED.last.toString())
                okButton.click()
            }
            snippetView("4").waitUntil {
                contentListView.contains(this, completelyDisplayed = false)
            }

            textFilterButton(R.string.area_kitchen).click()
            onScreen<AreaDialogScreen> {
                okButton.waitUntil { isCompletelyDisplayed() }
                valueFrom.replaceText(FILTER_KITCHEN_AREA_UPDATED.first.toString())
                valueTo.replaceText(FILTER_KITCHEN_AREA_UPDATED.last.toString())
                okButton.click()
            }
            snippetView("5").waitUntil {
                contentListView.contains(this, completelyDisplayed = false)
            }

            textFilterButton(R.string.bathroom_title).click()
            onScreen<BathroomDialogScreen> {
                separatedSelectorView
                    .waitUntil { isCompletelyDisplayed() }
                    .click()
            }
            snippetView("6").waitUntil {
                contentListView.contains(this, completelyDisplayed = false)
            }

            textFilterButton(R.string.floor).click()
            onScreen<FloorDialogScreen> {
                okButton.waitUntil { isCompletelyDisplayed() }
                valueFrom.replaceText(FILTER_FLOOR_UPDATED.first.toString())
                valueTo.replaceText(FILTER_FLOOR_UPDATED.last.toString())
                okButton.click()
            }
            emptyListItem.waitUntil {
                contentListView.contains(this, completelyDisplayed = false)
            }

            hideFiltersButton.click()
            bottomSheet.waitUntil { isExpanded() }
            root.isViewStateMatches(getTestRelatedFilePath("emptyContent"))
            resetFiltersButton.click()
            snippetView("1").waitUntil { contentListView.contains(this) }
            showFiltersButton.click()
            bottomSheet.waitUntil { isCollapsed() }
            filters.isViewStateMatches(getTestRelatedFilePath("emptyFilters"))
        }
    }

    @Test
    fun shouldNotShowUnavailableFilters() {
        configureWebServer {
            registerEmptyOfferStat()
            registerPlansSearch(SearchParams(), responseFileName = "plansSearch0")
        }
        activityTestRule.launchActivity()
        onScreen<DeveloperPlansScreen> {
            snippetView("0").waitUntil { contentListView.contains(this) }
            showFiltersButton.waitUntil { isCompletelyDisplayed() }.click()
            bottomSheet.waitUntil { isCollapsed() }
            filters.isViewStateMatches(getTestRelatedFilePath("filtersWithoutUnavailableItems"))
        }
    }

    @Test
    fun shouldPerformRetryAfterError() {
        configureWebServer {
            registerOfferStat()
            val params = SearchParams(price = FILTER_PRICE_DEFAULT, rooms = FILTER_ROOMS_DEFAULT)
            registerPlansSearchError(params)
            registerPlansSearch(params, responseFileName = "plansSearch0")
        }
        activityTestRule.filter = SiteSubFilter(
            roomsCount = setOf(
                Filter.RoomsCount.ONE,
                Filter.RoomsCount.THREE,
            ),
            priceRange = Range.Closed(
                lower = FILTER_PRICE_DEFAULT.first,
                upper = FILTER_PRICE_DEFAULT.last,
            ),
            priceType = Filter.PriceType.PER_OFFER,
        )
        activityTestRule.launchActivity()
        onScreen<DeveloperPlansScreen> {
            contentListView.waitUntil { contains(errorItem) }
            root.isViewStateMatches(getTestRelatedFilePath("error"))
            errorItem.view.click()
            snippetView("0").waitUntil { contentListView.contains(this) }
        }
    }

    @Test
    fun shouldShowFiltersIndicator() {
        configureWebServer {
            registerOfferStat()
            registerPlansSearch(
                params = SearchParams(price = FILTER_PRICE_DEFAULT),
                responseFileName = "plansSearch0",
            )
            registerPlansSearch(
                params = SearchParams(),
                responseFileName = "plansSearch1",
            )
        }
        activityTestRule.filter = SiteSubFilter(
            priceRange = Range.Closed(
                lower = FILTER_PRICE_DEFAULT.first,
                upper = FILTER_PRICE_DEFAULT.last,
            ),
            priceType = Filter.PriceType.PER_OFFER,
        )
        activityTestRule.launchActivity()
        onScreen<DeveloperPlansScreen> {
            snippetView("0").waitUntil { contentListView.contains(this) }

            toolbar
                .waitUntil { isCompletelyDisplayed() }
                .isViewStateMatches(getTestRelatedFilePath("toolbarWithFiltersIndicator"))

            showFiltersButton.click()
            bottomSheet.waitUntil { isCollapsed() }

            textFilterButton(R.string.price_filter).click()
            onScreen<PriceDialogScreen> {
                okButton.waitUntil { isCompletelyDisplayed() }
                valueFrom.clearText()
                valueTo.clearText()
                okButton.click()
            }
            snippetView("1").waitUntil {
                contentListView.contains(this, completelyDisplayed = false)
            }

            hideFiltersButton.click()
            bottomSheet.waitUntil { isExpanded() }

            toolbar
                .waitUntil { isCompletelyDisplayed() }
                .isViewStateMatches(getTestRelatedFilePath("toolbarWithoutFiltersIndicator"))
        }
    }

    @Test
    fun shouldDisplayShowFlatsButtonOnSnippet() {
        configureWebServer {
            registerOfferStat()
            registerPlansSearch(SearchParams(), responseFileName = "plansSearch9")
        }
        activityTestRule.housingType = HousingType.FLATS
        activityTestRule.launchActivity()
        onScreen<DeveloperPlansScreen> {
            snippetView("0")
                .waitUntil { contentListView.contains(this) }
                .showOffersButton.isTextEquals("Смотреть 1 квартиру")
            snippetView("1")
                .waitUntil { contentListView.contains(this) }
                .showOffersButton.isTextEquals("Смотреть 3 квартиры")
            snippetView("2")
                .waitUntil { contentListView.contains(this) }
                .showOffersButton.isTextEquals("Смотреть 5 квартир")
        }
    }

    @Test
    fun shouldDisplayShowOffersButtonOnSnippet() {
        configureWebServer {
            registerOfferStat()
            registerPlansSearch(SearchParams(), responseFileName = "plansSearch9")
        }
        activityTestRule.housingType = HousingType.APARTMENTS_AND_FLATS
        activityTestRule.launchActivity()
        onScreen<DeveloperPlansScreen> {
            snippetView("0")
                .waitUntil { contentListView.contains(this) }
                .showOffersButton.isTextEquals("Смотреть 1 предложение")
            snippetView("1")
                .waitUntil { contentListView.contains(this) }
                .showOffersButton.isTextEquals("Смотреть 3 предложения")
            snippetView("2")
                .waitUntil { contentListView.contains(this) }
                .showOffersButton.isTextEquals("Смотреть 5 предложений")
        }
    }

    @Test
    fun shouldDisplayVirtualTourBadge() {
        configureWebServer {
            registerOfferStat()
            registerPlansSearch(SearchParams(), responseFileName = "plansSearch8")
        }
        activityTestRule.launchActivity()
        onScreen<DeveloperPlansScreen> {
            snippetView("0")
                .waitUntil { contentListView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("snippetWithVirtualTourBadge"))
            snippetView("1")
                .waitUntil { contentListView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("snippetWithoutVirtualTourBadge"))
        }
    }

    private fun DispatcherRegistry.registerPlansSearch(
        params: SearchParams = SearchParams(),
        responseFileName: String,
    ) {
        registerPlansSearch(params) {
            assetBody("DeveloperPlansTest/$responseFileName.json")
        }
    }

    private fun DispatcherRegistry.registerPlansSearchError(
        params: SearchParams = SearchParams(),
    ) {
        registerPlansSearch(params) {
            error()
        }
    }

    private fun DispatcherRegistry.registerPlansSearch(
        params: SearchParams = SearchParams(),
        response: MockResponse.() -> Unit,
    ) {
        register(
            request {
                method("GET")
                path("2.0/site/$SITE_ID/planSearch")
                params.price?.let {
                    queryParam("priceType", "PER_OFFER")
                    queryParam("priceMin", params.price.first.toString())
                    queryParam("priceMax", params.price.last.toString())
                }
                params.floor?.let {
                    queryParam("floorMin", params.floor.first.toString())
                    queryParam("floorMax", params.floor.last.toString())
                }
                params.area?.let {
                    queryParam("areaMin", params.area.first.toString())
                    queryParam("areaMax", params.area.last.toString())
                }
                params.kitchenArea?.let {
                    queryParam("kitchenSpaceMin", params.kitchenArea.first.toString())
                    queryParam("kitchenSpaceMax", params.kitchenArea.last.toString())
                }
                params.rooms?.forEach { room -> queryParam("roomsTotal", room.toString()) }
                params.houseIds?.forEach { houseId -> queryParam("houseId", houseId) }
                params.bathroomUnit?.let { queryParam("bathroomUnit", params.bathroomUnit) }
                params.sorting?.let { queryParam("sort", params.sorting) }
            },
            response(response),
        )
    }

    private fun DispatcherRegistry.registerOfferStat() {
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
                assetBody("DeveloperPlansTest/offerStats.json")
            },
        )
    }

    private fun DispatcherRegistry.registerEmptyOfferStat() {
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
                assetBody("DeveloperPlansTest/offerStatsEmpty.json")
            },
        )
    }

    private fun DispatcherRegistry.registerFilteredOffersSearch() {
        register(
            request {
                method("GET")
                path("1.0/offerWithSiteSearch.json")
                queryParam("siteFlatPlanId", "8")
                queryParam("priceType", "PER_OFFER")
                queryParam("priceMin", FILTER_PRICE_DEFAULT.first.toString())
                queryParam("priceMax", FILTER_PRICE_DEFAULT.last.toString())
                queryParam("areaMin", FILTER_AREA_UPDATED.first.toString())
                queryParam("areaMax", FILTER_AREA_UPDATED.last.toString())
                FILTER_ROOMS_DEFAULT.forEach { room ->
                    queryParam("roomsTotal", room.toString())
                }
            },
            response {
                assetBody("DeveloperPlansTest/offersSearch.json")
            },
        )
    }

    private data class SearchParams(
        val price: LongRange? = null,
        val rooms: Set<Int>? = null,
        val houseIds: Set<String>? = null,
        val area: IntRange? = null,
        val kitchenArea: IntRange? = null,
        val bathroomUnit: String? = null,
        val floor: IntRange? = null,
        val sorting: String? = null,
    )

    private companion object {
        const val SITE_ID = "0"
        const val SITE_NAME = "stub"
        const val FILTER_BATHROOM_UNIT_UPDATED = "SEPARATED"
        val FILTER_ROOMS_DEFAULT: Set<Int> = setOf(1, 3)
        val FILTER_ROOMS_UPDATED: Set<Int> = setOf(1, 2, 3)
        val FILTER_PRICE_DEFAULT = 2_500_000L..7_500_000L
        val FILTER_PRICE_UPDATED = 3_500_000L..8_500_000L
        val FILTER_BUILDINGS_UPDATED: Set<String> = setOf("1")
        val FILTER_AREA_UPDATED = 50..100
        val FILTER_KITCHEN_AREA_UPDATED = 10..20
        val FILTER_FLOOR_UPDATED = 5..10
    }
}
