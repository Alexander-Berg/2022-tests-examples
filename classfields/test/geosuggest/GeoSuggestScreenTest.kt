package com.yandex.mobile.realty.test.geosuggest

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.GeoIntentActivityTestRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.AllSuggestScreen
import com.yandex.mobile.realty.core.screen.DistrictSuggestScreen
import com.yandex.mobile.realty.core.screen.GeoIntentScreen
import com.yandex.mobile.realty.core.screen.MetroSuggestScreen
import com.yandex.mobile.realty.core.screen.RegionSuggestScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author misha-kozlov on 25.09.2020
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class GeoSuggestScreenTest {

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(GeoIntentActivityTestRule())

    @Test
    fun shouldShowTabAllInitial() {
        onScreen<GeoIntentScreen> {
            addButton.click()
            closeKeyboard()

            onScreen<AllSuggestScreen> {
                listView.isCompletelyDisplayed()
                isViewStateMatches("GeoSuggest/tabAllContentInitial")
            }
        }
    }

    @Test
    fun shouldShowTabAllError() {
        onScreen<GeoIntentScreen> {
            searchView.typeText("test")

            onScreen<AllSuggestScreen> {
                waitUntil { errorView.isCompletelyDisplayed() }
                isViewStateMatches("GeoSuggest/tabAllContentError")
            }
        }
    }

    @Test
    fun shouldShowTabAllNotFound() {
        configureWebServer {
            registerEmptyGeoSuggest()
        }

        onScreen<GeoIntentScreen> {
            searchView.typeText("test")

            onScreen<AllSuggestScreen> {
                waitUntil { emptyView.isCompletelyDisplayed() }
                isViewStateMatches("GeoSuggest/tabAllContentNotFound")
            }
        }
    }

    @Test
    fun shouldShowTabAllSearchResult() {
        configureWebServer {
            registerGeoSuggest()
        }

        onScreen<GeoIntentScreen> {
            searchView.typeText("metro")

            onScreen<AllSuggestScreen> {
                geoSuggestItem("метро Площадь Революции")
                    .waitUntil { view.isCompletelyDisplayed() }
                    .also { isViewStateMatches("GeoSuggest/tabAllSearchResult") }
                    .click()
            }

            isViewStateMatches("GeoSuggest/geoSuggestSelected")
        }
    }

    @Test
    fun shouldShowTabMetroInitial() {
        configureWebServer {
            registerMetros()
        }

        onScreen<GeoIntentScreen> {
            addButton.click()
            closeKeyboard()

            metroTab.click()

            onScreen<MetroSuggestScreen> {
                metroSuggestItem("Group")
                    .waitUntil { view.isCompletelyDisplayed() }
                isViewStateMatches("GeoSuggest/tabMetroContentInitial")
            }
        }
    }

    @Test
    fun shouldShowTabMetroError() {
        onScreen<GeoIntentScreen> {
            addButton.click()
            closeKeyboard()

            metroTab.click()

            onScreen<MetroSuggestScreen> {
                waitUntil { errorView.isCompletelyDisplayed() }
                isViewStateMatches("GeoSuggest/tabMetroContentError")
            }
        }
    }

    @Test
    fun shouldShowTabMetroNotFound() {
        configureWebServer {
            registerMetros()
        }

        onScreen<GeoIntentScreen> {
            searchView.typeText("test")

            metroTab.click()

            onScreen<MetroSuggestScreen> {
                waitUntil { emptyView.isCompletelyDisplayed() }
                isViewStateMatches("GeoSuggest/tabMetroContentNotFound")
            }
        }
    }

    @Test
    fun shouldShowTabMetroSearchResult() {
        configureWebServer {
            registerMetros()
        }

        onScreen<GeoIntentScreen> {
            searchView.typeText("Station 2")

            metroTab.click()

            onScreen<MetroSuggestScreen> {
                metroSuggestItem("Station 2")
                    .waitUntil { view.isCompletelyDisplayed() }
                    .also { isViewStateMatches("GeoSuggest/tabMetroSearchResult") }
                    .click()
            }

            isViewStateMatches("GeoSuggest/metroSuggestSelected")
        }
    }

    @Test
    fun shouldShowTabDistrictInitial() {
        configureWebServer {
            registerDistricts()
        }

        onScreen<GeoIntentScreen> {
            addButton.click()
            closeKeyboard()

            districtTab.click()

            onScreen<DistrictSuggestScreen> {
                districtSuggestItem("ЦАО")
                    .waitUntil { view.isCompletelyDisplayed() }
                isViewStateMatches("GeoSuggest/tabDistrictContentInitial")
            }
        }
    }

    @Test
    fun shouldShowTabDistrictError() {
        onScreen<GeoIntentScreen> {
            addButton.click()
            closeKeyboard()

            districtTab.click()

            onScreen<DistrictSuggestScreen> {
                waitUntil { errorView.isCompletelyDisplayed() }
                isViewStateMatches("GeoSuggest/tabDistrictContentError")
            }
        }
    }

    @Test
    fun shouldShowTabDistrictNotFound() {
        configureWebServer {
            registerDistricts()
        }

        onScreen<GeoIntentScreen> {
            searchView.typeText("test")

            districtTab.click()

            onScreen<DistrictSuggestScreen> {
                waitUntil { emptyView.isCompletelyDisplayed() }
                isViewStateMatches("GeoSuggest/tabDistrictContentNotFound")
            }
        }
    }

    @Test
    fun shouldShowTabDistrictSearchResult() {
        configureWebServer {
            registerDistricts()
        }

        onScreen<GeoIntentScreen> {
            searchView.typeText("District 2")

            districtTab.click()

            onScreen<DistrictSuggestScreen> {
                districtSuggestItem("District 2")
                    .waitUntil { view.isCompletelyDisplayed() }
                    .also { isViewStateMatches("GeoSuggest/tabDistrictSearchResult") }
                    .click()
            }

            isViewStateMatches("GeoSuggest/districtSuggestSelected")
        }
    }

    @Test
    fun shouldHideMetroTabWhenRegionChanged() {
        configureWebServer {
            registerRegionList()
            registerSochiRegionInfo()
        }

        onScreen<GeoIntentScreen> {
            addButton.click()
            closeKeyboard()

            tabsView.isViewStateMatches("GeoSuggest/allTabsShown")

            pressBack()

            regionButton.click()
        }

        onScreen<RegionSuggestScreen> {
            closeKeyboard()
            regionSuggest("Сочи")
                .waitUntil { listView.contains(this) }
                .click()
        }

        onScreen<GeoIntentScreen> {
            waitUntil { addButton.isCompletelyDisplayed() }
            addButton.click()

            tabsView.isViewStateMatches("GeoSuggest/tabMetroHiddenAndTabAllSelected")
        }
    }

    private fun DispatcherRegistry.registerEmptyGeoSuggest() {
        register(
            request {
                path("1.0/geosuggest.json")
                queryParam("text", "test")
            },
            response {
                assetBody("geoSuggestTest/geoSuggestEmpty.json")
            }
        )
    }

    private fun DispatcherRegistry.registerGeoSuggest() {
        register(
            request {
                path("1.0/geosuggest.json")
                queryParam("text", "metro")
            },
            response {
                assetBody("geoSuggestMetro.json")
            }
        )
    }

    private fun DispatcherRegistry.registerMetros() {
        register(
            request {
                path("2.0/suggest/regionMetros")
            },
            response {
                assetBody("geoSuggestTest/regionMetros.json")
            }
        )
    }

    private fun DispatcherRegistry.registerDistricts() {
        register(
            request {
                path("2.0/suggest/regionDistricts")
            },
            response {
                assetBody("geoSuggestTest/regionDistricts.json")
            }
        )
    }

    private fun DispatcherRegistry.registerRegionList() {
        register(
            request {
                path("1.0/regionList.json")
            },
            response {
                assetBody("geoSuggestTest/regionList.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSochiRegionInfo() {
        register(
            request {
                path("1.0/getRegionInfoV15.json")
                queryParam("rgid", "17244963")
            },
            response {
                assetBody("geoSuggestTest/regionInfoSochi.json")
            }
        )
    }
}
