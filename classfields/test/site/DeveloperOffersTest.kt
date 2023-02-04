package com.yandex.mobile.realty.test.site

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.DeveloperOffersActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.AreaDialogScreen
import com.yandex.mobile.realty.core.screen.BathroomDialogScreen
import com.yandex.mobile.realty.core.screen.DeveloperOffersScreen
import com.yandex.mobile.realty.core.screen.FloorDialogScreen
import com.yandex.mobile.realty.core.screen.OfferCardScreen
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
class DeveloperOffersTest : BaseTest() {

    private val activityTestRule = DeveloperOffersActivityTestRule(
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
            registerOffersSearch(params, responseFileName = "offersSearch0")
            params = params.copy(rooms = FILTER_ROOMS_UPDATED)
            registerOffersSearch(params, responseFileName = "offersSearch1")
            params = params.copy(price = FILTER_PRICE_UPDATED)
            registerOffersSearch(params, responseFileName = "offersSearch2")
            params = params.copy(houseIds = FILTER_BUILDINGS_UPDATED)
            registerOffersSearch(params, responseFileName = "offersSearch3")
            params = params.copy(area = FILTER_AREA_UPDATED)
            registerOffersSearch(params, responseFileName = "offersSearch4")
            params = params.copy(kitchenArea = FILTER_KITCHEN_AREA_UPDATED)
            registerOffersSearch(params, responseFileName = "offersSearch5")
            params = params.copy(bathroomUnit = FILTER_BATHROOM_UNIT_UPDATED)
            registerOffersSearch(params, responseFileName = "offersSearch6")
            params = params.copy(floor = FILTER_FLOOR_UPDATED)
            registerOffersSearch(params, responseFileName = "offersSearch7")
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
        onScreen<DeveloperOffersScreen> {
            snippetView("00").waitUntil { contentListView.contains(this) }
            showFiltersButton.waitUntil { isCompletelyDisplayed() }.click()
            filters.waitUntil { isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("initialFilters"))

            roomFilterButton(R.string.rooms_two).click()
            snippetView("10").waitUntil { contentListView.contains(this) }

            textFilterButton(R.string.price_filter).click()
            onScreen<PriceDialogScreen> {
                okButton.waitUntil { isCompletelyDisplayed() }
                valueFrom.replaceText(FILTER_PRICE_UPDATED.first.toString())
                valueTo.replaceText(FILTER_PRICE_UPDATED.last.toString())
                okButton.click()
            }
            snippetView("20").waitUntil { contentListView.contains(this) }

            textFilterButton(R.string.site_building_filter).click()
            onScreen<SiteBuildingDialogScreen> {
                buildingView(title = "2 кв. 2023", subtitle = "Корпус 6.2")
                    .waitUntil { isCompletelyDisplayed() }
                    .click()
                okButton.click()
            }
            snippetView("30").waitUntil { contentListView.contains(this) }

            textFilterButton(R.string.area_total_full_title).click()
            onScreen<AreaDialogScreen> {
                okButton.waitUntil { isCompletelyDisplayed() }
                valueFrom.replaceText(FILTER_AREA_UPDATED.first.toString())
                valueTo.replaceText(FILTER_AREA_UPDATED.last.toString())
                okButton.click()
            }
            snippetView("40").waitUntil { contentListView.contains(this) }

            textFilterButton(R.string.area_kitchen).click()
            onScreen<AreaDialogScreen> {
                okButton.waitUntil { isCompletelyDisplayed() }
                valueFrom.replaceText(FILTER_KITCHEN_AREA_UPDATED.first.toString())
                valueTo.replaceText(FILTER_KITCHEN_AREA_UPDATED.last.toString())
                okButton.click()
            }
            snippetView("50").waitUntil { contentListView.contains(this) }

            textFilterButton(R.string.bathroom_title).click()
            onScreen<BathroomDialogScreen> {
                separatedSelectorView
                    .waitUntil { isCompletelyDisplayed() }
                    .click()
            }
            snippetView("60").waitUntil { contentListView.contains(this) }

            textFilterButton(R.string.floor).click()
            onScreen<FloorDialogScreen> {
                okButton.waitUntil { isCompletelyDisplayed() }
                valueFrom.replaceText(FILTER_FLOOR_UPDATED.first.toString())
                valueTo.replaceText(FILTER_FLOOR_UPDATED.last.toString())
                okButton.click()
            }
            snippetView("70").waitUntil { contentListView.contains(this) }

            root.isViewStateMatches(getTestRelatedFilePath("finalFilters"))
            hideFiltersButton.click()
            contentListView.waitUntil { isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("content"))
        }
    }

    @Test
    fun shouldShowSortedContent() {
        configureWebServer {
            registerOfferStat()
            var params = SearchParams(sorting = Sorting.PRICE.value)
            registerOffersSearch(params, responseFileName = "offersSearch0")
            params = SearchParams(sorting = Sorting.AREA_DESC.value)
            registerOffersSearch(params, responseFileName = "offersSearch1")
            params = SearchParams(sorting = Sorting.PRICE_PER_SQUARE.value)
            registerOffersSearch(params, responseFileName = "offersSearch2")
        }
        activityTestRule.launchActivity()
        onScreen<DeveloperOffersScreen> {

            snippetView("00").waitUntil { contentListView.contains(this) }
            sortingView.click()
            onScreen<SiteSortingDialogScreen> {
                price.waitUntil { isChecked() }
                areaDesc.waitUntil { isCompletelyDisplayed() }.click()
            }

            snippetView("10").waitUntil { contentListView.contains(this) }
            sortingView.click()
            onScreen<SiteSortingDialogScreen> {
                areaDesc.waitUntil { isChecked() }
                pricePerMeter.waitUntil { isCompletelyDisplayed() }.click()
            }

            snippetView("20").waitUntil { contentListView.contains(this) }
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
            registerOffersSearch(responseFileName = "offersSearch0")
        }
        activityTestRule.launchActivity()
        onScreen<DeveloperOffersScreen> {
            snippetView("00")
                .waitUntil { contentListView.contains(this) }
                .click()
        }
        onScreen<OfferCardScreen> {
            galleryView.waitUntil { isCompletelyDisplayed() }
        }
    }

    @Test
    fun shouldResetFiltersIfResultIsEmpty() {
        configureWebServer {
            registerOfferStat()
            var params = SearchParams(price = FILTER_PRICE_DEFAULT, rooms = FILTER_ROOMS_DEFAULT)
            registerOffersSearch(params, responseFileName = "offersSearch0")
            params = params.copy(rooms = FILTER_ROOMS_UPDATED)
            registerOffersSearch(params, responseFileName = "offersSearch1")
            params = params.copy(price = FILTER_PRICE_UPDATED)
            registerOffersSearch(params, responseFileName = "offersSearch2")
            params = params.copy(houseIds = FILTER_BUILDINGS_UPDATED)
            registerOffersSearch(params, responseFileName = "offersSearch3")
            params = params.copy(area = FILTER_AREA_UPDATED)
            registerOffersSearch(params, responseFileName = "offersSearch4")
            params = params.copy(kitchenArea = FILTER_KITCHEN_AREA_UPDATED)
            registerOffersSearch(params, responseFileName = "offersSearch5")
            params = params.copy(bathroomUnit = FILTER_BATHROOM_UNIT_UPDATED)
            registerOffersSearch(params, responseFileName = "offersSearch6")
            params = params.copy(floor = FILTER_FLOOR_UPDATED)
            registerOffersSearch(params, responseFileName = "offersSearchEmpty")
            params = SearchParams()
            registerOffersSearch(params, responseFileName = "offersSearch1")
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
        onScreen<DeveloperOffersScreen> {
            snippetView("00").waitUntil { contentListView.contains(this) }
            showFiltersButton.waitUntil { isCompletelyDisplayed() }.click()

            roomFilterButton(R.string.rooms_two).click()
            snippetView("10").waitUntil { contentListView.contains(this) }

            textFilterButton(R.string.price_filter).click()
            onScreen<PriceDialogScreen> {
                okButton.waitUntil { isCompletelyDisplayed() }
                valueFrom.replaceText(FILTER_PRICE_UPDATED.first.toString())
                valueTo.replaceText(FILTER_PRICE_UPDATED.last.toString())
                okButton.click()
            }
            snippetView("20").waitUntil { contentListView.contains(this) }

            textFilterButton(R.string.site_building_filter).click()
            onScreen<SiteBuildingDialogScreen> {
                buildingView(title = "2 кв. 2023", subtitle = "Корпус 6.2")
                    .waitUntil { isCompletelyDisplayed() }
                    .click()
                okButton.click()
            }
            snippetView("30").waitUntil { contentListView.contains(this) }

            textFilterButton(R.string.area_total_full_title).click()
            onScreen<AreaDialogScreen> {
                okButton.waitUntil { isCompletelyDisplayed() }
                valueFrom.replaceText(FILTER_AREA_UPDATED.first.toString())
                valueTo.replaceText(FILTER_AREA_UPDATED.last.toString())
                okButton.click()
            }
            snippetView("40").waitUntil { contentListView.contains(this) }

            textFilterButton(R.string.area_kitchen).click()
            onScreen<AreaDialogScreen> {
                okButton.waitUntil { isCompletelyDisplayed() }
                valueFrom.replaceText(FILTER_KITCHEN_AREA_UPDATED.first.toString())
                valueTo.replaceText(FILTER_KITCHEN_AREA_UPDATED.last.toString())
                okButton.click()
            }
            snippetView("50").waitUntil { contentListView.contains(this) }

            textFilterButton(R.string.bathroom_title).click()
            onScreen<BathroomDialogScreen> {
                separatedSelectorView
                    .waitUntil { isCompletelyDisplayed() }
                    .click()
            }
            snippetView("60").waitUntil { contentListView.contains(this) }

            textFilterButton(R.string.floor).click()
            onScreen<FloorDialogScreen> {
                okButton.waitUntil { isCompletelyDisplayed() }
                valueFrom.replaceText(FILTER_FLOOR_UPDATED.first.toString())
                valueTo.replaceText(FILTER_FLOOR_UPDATED.last.toString())
                okButton.click()
            }
            emptyListItem.waitUntil { contentListView.contains(this, completelyDisplayed = false) }

            hideFiltersButton.click()
            contentListView.waitUntil { isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("emptyContent"))
            resetFiltersButton.click()
            snippetView("10").waitUntil { contentListView.contains(this) }
            showFiltersButton.click()
            filters.waitUntil { isCompletelyDisplayed() }
            filters.isViewStateMatches(getTestRelatedFilePath("emptyFilters"))
        }
    }

    @Test
    fun shouldNotShowUnavailableFilters() {
        configureWebServer {
            registerEmptyOfferStat()
            registerOffersSearch(SearchParams(), responseFileName = "offersSearch0")
        }
        activityTestRule.launchActivity()
        onScreen<DeveloperOffersScreen> {
            snippetView("00").waitUntil { contentListView.contains(this) }
            showFiltersButton.waitUntil { isCompletelyDisplayed() }.click()
            filters.waitUntil { isCompletelyDisplayed() }
            filters.isViewStateMatches(getTestRelatedFilePath("filtersWithoutUnavailableItems"))
        }
    }

    @Test
    fun shouldPerformRetryAfterError() {
        configureWebServer {
            registerOfferStat()
            val params = SearchParams(price = FILTER_PRICE_DEFAULT, rooms = FILTER_ROOMS_DEFAULT)
            registerOffersSearchError(params)
            registerOffersSearch(params, responseFileName = "offersSearch0")
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
        onScreen<DeveloperOffersScreen> {
            contentListView.waitUntil { contains(errorItem) }
            root.isViewStateMatches(getTestRelatedFilePath("error"))
            errorItem.view.click()
            snippetView("00").waitUntil { contentListView.contains(this) }
        }
    }

    @Test
    fun shouldShowFiltersIndicator() {
        configureWebServer {
            registerOfferStat()
            registerOffersSearch(
                params = SearchParams(price = FILTER_PRICE_DEFAULT),
                responseFileName = "offersSearch0",
            )
            registerOffersSearch(
                params = SearchParams(),
                responseFileName = "offersSearch1",
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
        onScreen<DeveloperOffersScreen> {
            snippetView("00").waitUntil { contentListView.contains(this) }

            toolbar
                .waitUntil { isCompletelyDisplayed() }
                .isViewStateMatches(getTestRelatedFilePath("toolbarWithFiltersIndicator"))

            showFiltersButton.click()
            filters.waitUntil { isCompletelyDisplayed() }

            textFilterButton(R.string.price_filter).click()
            onScreen<PriceDialogScreen> {
                okButton.waitUntil { isCompletelyDisplayed() }
                valueFrom.clearText()
                valueTo.clearText()
                okButton.click()
            }
            snippetView("10").waitUntil { contentListView.contains(this) }

            hideFiltersButton.click()
            contentListView.waitUntil { isCompletelyDisplayed() }

            toolbar
                .waitUntil { isCompletelyDisplayed() }
                .isViewStateMatches(getTestRelatedFilePath("toolbarWithoutFiltersIndicator"))
        }
    }

    @Test
    fun shouldShowFlatsTitle() {
        configureWebServer {
            registerEmptyOfferStat()
            registerOffersSearch(responseFileName = "offersSearch0")
        }
        activityTestRule.housingType = HousingType.FLATS
        activityTestRule.launchActivity()
        onScreen<DeveloperOffersScreen> {
            toolbar.waitUntil { hasTitle("Квартиры") }
        }
    }

    @Test
    fun shouldShowApartmentsTitle() {
        configureWebServer {
            registerEmptyOfferStat()
            registerOffersSearch(responseFileName = "offersSearch0")
        }
        activityTestRule.housingType = HousingType.APARTMENTS
        activityTestRule.launchActivity()
        onScreen<DeveloperOffersScreen> {
            toolbar.waitUntil { hasTitle("Апартаменты") }
        }
    }

    @Test
    fun shouldShowFlatsAndApartmentsTitle() {
        configureWebServer {
            registerEmptyOfferStat()
            registerOffersSearch(responseFileName = "offersSearch0")
        }
        activityTestRule.housingType = HousingType.APARTMENTS_AND_FLATS
        activityTestRule.launchActivity()
        onScreen<DeveloperOffersScreen> {
            toolbar.waitUntil { hasTitle("Квартиры и апартаменты") }
        }
    }

    @Test
    fun shouldDisplayVirtualTourBadge() {
        configureWebServer {
            registerOfferStat()
            registerOffersSearch(responseFileName = "offersSearch8")
        }
        activityTestRule.launchActivity()
        onScreen<DeveloperOffersScreen> {
            snippetView("80")
                .waitUntil { contentListView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("snippetWithVirtualTourBadge"))
            snippetView("81")
                .waitUntil { contentListView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("snippetWithoutVirtualTourBadge"))
        }
    }

    private fun DispatcherRegistry.registerOffersSearchError(
        params: SearchParams = SearchParams(),
    ) {
        registerOffersSearch(params) {
            error()
        }
    }

    private fun DispatcherRegistry.registerOffersSearch(
        params: SearchParams = SearchParams(),
        responseFileName: String,
    ) {
        registerOffersSearch(params) {
            assetBody("DeveloperOffersTest/$responseFileName.json")
        }
    }

    private fun DispatcherRegistry.registerOffersSearch(
        params: SearchParams = SearchParams(),
        response: MockResponse.() -> Unit,
    ) {
        register(
            request {
                method("GET")
                path("1.0/offerWithSiteSearch.json")
                queryParam("siteId", SITE_ID)
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
                assetBody("DeveloperOffersTest/offerStats.json")
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
                assetBody("DeveloperOffersTest/offerStatsEmpty.json")
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
