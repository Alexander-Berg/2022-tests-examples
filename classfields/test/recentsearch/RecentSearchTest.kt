package com.yandex.mobile.realty.test.recentsearch

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.FilterActivityTestRule
import com.yandex.mobile.realty.core.rule.DatabaseRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.FiltersScreen
import com.yandex.mobile.realty.core.screen.RecentSearchScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.domain.model.Range
import com.yandex.mobile.realty.domain.model.geo.GeoIntent
import com.yandex.mobile.realty.domain.model.geo.GeoObject
import com.yandex.mobile.realty.domain.model.geo.GeoPoint
import com.yandex.mobile.realty.domain.model.geo.GeoRegion
import com.yandex.mobile.realty.domain.model.geo.GeoType
import com.yandex.mobile.realty.domain.model.search.Filter
import com.yandex.mobile.realty.domain.model.search.RecentSearch
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author misha-kozlov on 5/20/21
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class RecentSearchTest {

    private val activityTestRule = FilterActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true),
        activityTestRule,
        DatabaseRule(
            DatabaseRule.createAddRecentSearchesEntryStatement(
                createRecentSearchWithLongGeo()
            )
        ),
        DatabaseRule(
            DatabaseRule.createAddRecentSearchesEntryStatement(
                createRecentSearchWithHiddenParams()
            )
        ),
        DatabaseRule(
            DatabaseRule.createAddRecentSearchesEntryStatement(
                createSkippedRecentSearch()
            )
        )
    )

    @Test
    fun shouldShowRecentSearch() {
        configureWebServer {
            registerSearchCountOnly()
            registerSearchCountOnly()
            registerRegionInfo()
            registerSearchList()
        }

        activityTestRule.launchActivity()

        onScreen<FiltersScreen> {
            waitUntil { listView.contains(recentSearchesItem) }
            recentSearchSnippet("Купить квартиру")
                .apply { recentSearchesListView.scrollTo(this) }
                .view {
                    isViewStateMatches("RecentSearchTest/shouldShowRecentSearch/snippet")
                    moreButton.click()
                }
        }
        onScreen<RecentSearchScreen> {
            waitUntil { submitButton.isTextEquals("Показать 5 объявлений") }
            root.isViewStateMatches("RecentSearchTest/shouldShowRecentSearch/bottomsheet")
            submitButton.click()
        }
        onScreen<SearchListScreen> {
            offerSnippet("0").waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldShowRecentSearchWithHiddenParams() {
        activityTestRule.launchActivity()

        onScreen<FiltersScreen> {
            waitUntil { listView.contains(recentSearchesItem) }
            recentSearchSnippet("Купить комнату")
                .apply { recentSearchesListView.scrollTo(this) }
                .view {
                    isViewStateMatches(
                        "RecentSearchTest/shouldShowRecentSearchWithHiddenParams/snippet"
                    )
                }
        }
    }

    @Test
    fun shouldCloseRecentSearch() {
        activityTestRule.launchActivity()

        onScreen<FiltersScreen> {
            waitUntil { listView.contains(recentSearchesItem) }
            recentSearchSnippet("Купить квартиру")
                .apply { recentSearchesListView.scrollTo(this) }
                .view {
                    moreButton.click()
                }
        }
        onScreen<RecentSearchScreen> {
            closeButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }
        onScreen<FiltersScreen> {
            waitUntil { listView.contains(recentSearchesItem) }
        }
    }

    private fun DispatcherRegistry.registerSearchCountOnly() {
        register(
            request {
                path("2.0/offers/number")
            },
            response {
                setBody("{\"response\":{\"number\":5}}")
            }
        )
    }

    private fun DispatcherRegistry.registerRegionInfo() {
        register(
            request {
                path("1.0/getRegionInfoV15.json")
                queryParam("rgid", "587795")
            },
            response {
                assetBody("recentSearchTest/regionInfoMoscow.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSearchList() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
            },
            response {
                assetBody("offerWithSiteSearchPage0.json")
            }
        )
    }

    companion object {

        private fun createRecentSearchWithLongGeo(): RecentSearch {
            val geoObjects = List(20) { createGeoObject() }
            return RecentSearch(
                filter = Filter.SellApartment(),
                geoIntent = GeoIntent.Objects(geoObjects),
                region = GeoRegion.DEFAULT
            )
        }

        private fun createRecentSearchWithHiddenParams(): RecentSearch {
            val geoObjects = List(8) { createGeoObject() }
            return RecentSearch(
                filter = Filter.SellRoom(
                    price = Range.valueOf(10, 20),
                    priceType = Filter.PriceType.PER_OFFER,
                    parkingType = setOf(Filter.ParkingType.OPEN)
                ),
                geoIntent = GeoIntent.Objects(geoObjects),
                region = GeoRegion.DEFAULT
            )
        }

        private fun createSkippedRecentSearch(): RecentSearch {
            return RecentSearch(
                filter = Filter.RentRoom(),
                geoIntent = GeoIntent.Objects.valueOf(GeoRegion.DEFAULT),
                region = GeoRegion.DEFAULT
            )
        }

        private fun createGeoObject(): GeoObject {
            return GeoObject(
                type = GeoType.METRO_STATION,
                fullName = "metro",
                shortName = "metro",
                scope = null,
                point = GeoPoint(10.0, 11.0),
                leftTop = GeoPoint(9.0, 10.0),
                rightBottom = GeoPoint(11.0, 12.0),
                colors = emptyList(),
                params = mapOf("param_key" to listOf("param_value"))
            )
        }
    }
}
