package com.yandex.mobile.realty.test.services

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.ServicesActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.DatabaseRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.FavoriteScreen
import com.yandex.mobile.realty.core.screen.FiltersScreen
import com.yandex.mobile.realty.core.screen.RecentSearchScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.screen.SearchMapScreen
import com.yandex.mobile.realty.core.screen.ServicesScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.data.model.StoredSavedSearch
import com.yandex.mobile.realty.domain.model.geo.GeoIntent
import com.yandex.mobile.realty.domain.model.geo.GeoObject
import com.yandex.mobile.realty.domain.model.geo.GeoPoint
import com.yandex.mobile.realty.domain.model.geo.GeoRegion
import com.yandex.mobile.realty.domain.model.geo.GeoType
import com.yandex.mobile.realty.domain.model.search.Filter
import com.yandex.mobile.realty.domain.model.search.RecentSearch
import com.yandex.mobile.realty.test.BaseTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author pvl-zolotov on 19.04.2022
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ServicesSearchPresetsAllTest : BaseTest() {

    private val activityTestRule = ServicesActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true),
        DatabaseRule(
            DatabaseRule.createAddRecentSearchesEntryStatement(createRecentSearch()),
            DatabaseRule.createAddRecentSearchesEntryStatement(createRecentSearch()),
            DatabaseRule.createAddSavedSearchesEntryStatement(createStoredSavedSearch())
        ),
        activityTestRule
    )

    @Test
    fun testPresetsConfiguration() {
        activityTestRule.launchActivity()
        onScreen<ServicesScreen> {
            searchCard
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("presetsAll"))
        }
    }

    @Test
    fun testClickOnRentTwoRoomsPreset() {
        activityTestRule.launchActivity()
        onScreen<ServicesScreen> {
            waitUntil {
                listView.contains(searchCard)
            }
            rentTwoRoomPresetItem.click()
        }

        onScreen<SearchMapScreen> {
            waitUntil { filterButton.isCompletelyDisplayed() }
            filterButton.click()
        }

        onScreen<FiltersScreen> {
            isRentSelected()
            rentTimeSelectorLong.isChecked()
            roomsCountSelectorTwo.isChecked()
        }
    }

    @Test
    fun testClickOnBuySitePreset() {
        activityTestRule.launchActivity()
        onScreen<ServicesScreen> {
            waitUntil {
                listView.contains(searchCard)
            }
            buySitePresetItem.click()
        }

        onScreen<SearchMapScreen> {
            waitUntil { filterButton.isCompletelyDisplayed() }
            filterButton.click()
        }

        onScreen<FiltersScreen> {
            apartmentCategorySelectorNew.isChecked()
            priceValue.isTextEquals("от 1,4 млн \u20BD")
        }
    }

    @Test
    fun testClickOnBuyOneRoomPreset() {
        activityTestRule.launchActivity()
        onScreen<ServicesScreen> {
            waitUntil {
                listView.contains(searchCard)
            }
            buyOneRoomPresetItem.click()
        }

        onScreen<SearchMapScreen> {
            waitUntil { filterButton.isCompletelyDisplayed() }
            filterButton.click()
        }

        onScreen<FiltersScreen> {
            isBuySelected()
            apartmentCategorySelectorAny.isChecked()
            roomsCountSelectorOne.isChecked()
        }
    }

    @Test
    fun testClickOnSearchButton() {
        activityTestRule.launchActivity()
        onScreen<ServicesScreen> {
            waitUntil {
                listView.contains(searchCard)
            }
            searchButton.click()
        }

        onScreen<FiltersScreen> {
            waitUntil { submitButton.isCompletelyDisplayed() }
        }
    }

    @Test
    fun testClickOnSavedSearchesPreset() {
        activityTestRule.launchActivity()
        onScreen<ServicesScreen> {
            waitUntil {
                listView.contains(searchCard)
            }
            savedSearchesPresetItem.click()
        }

        onScreen<FavoriteScreen> {
            subscriptionsTabView.waitUntil { isSelected() }
        }
    }

    @Test
    fun testClickOnRecentSearchesPreset() {
        configureWebServer {
            registerSearchCountOnly()
            registerSearchCountOnly()
            registerRegionInfo()
            registerSearchList()
        }

        activityTestRule.launchActivity()
        onScreen<ServicesScreen> {
            waitUntil {
                listView.contains(searchCard)
            }
            recentSearchesPresetItem.click()
        }

        onScreen<RecentSearchScreen> {
            waitUntil { submitButton.isTextEquals("Показать 5 объявлений") }
            submitButton.click()
        }
        onScreen<SearchListScreen> {
            offerSnippet("0").waitUntil { listView.contains(this) }
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

        private const val SEARCH_ID = "abc"

        private fun createRecentSearch(): RecentSearch {
            val geoObjects = List(8) { createGeoObject() }
            return RecentSearch(
                filter = Filter.SellApartment(),
                geoIntent = GeoIntent.Objects(geoObjects),
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

        fun createStoredSavedSearch(): StoredSavedSearch {
            return StoredSavedSearch.of(
                SEARCH_ID,
                "test",
                Filter.SellHouse(),
                GeoIntent.Objects.valueOf(GeoRegion.DEFAULT)
            )
        }
    }
}
