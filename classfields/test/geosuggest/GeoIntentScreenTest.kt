package com.yandex.mobile.realty.test.geosuggest

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.GeoIntentActivityTestRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.AllSuggestScreen
import com.yandex.mobile.realty.core.screen.DistrictSuggestScreen
import com.yandex.mobile.realty.core.screen.GeoIntentScreen
import com.yandex.mobile.realty.core.screen.MetroSuggestScreen
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
class GeoIntentScreenTest {

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(GeoIntentActivityTestRule())

    @Test
    fun shouldShowMapSearchDialog() {
        onScreen<GeoIntentScreen> {
            closeKeyboard()
            isViewStateMatches("GeoIntent/mapSearch")

            mapSearchButton.click()

            root.isViewStateMatches("GeoIntent/mapSearchDialog")
        }
    }

    @Test
    fun shouldAddAndRemoveGeoObjectFromTabAll() {
        configureWebServer {
            registerGeoSuggest()
        }

        onScreen<GeoIntentScreen> {
            searchView.typeText("metro")

            onScreen<AllSuggestScreen> {
                geoSuggestItem("метро Площадь Революции")
                    .waitUntil { view.isCompletelyDisplayed() }
                    .click()
            }

            isViewStateMatches("GeoIntent/geoObjectFromTabAll")

            geoObjectView("метро Площадь Революции").clickRemove()

            isViewStateMatches("GeoIntent/mapSearchAll")
        }
    }

    @Test
    fun shouldAddGeoObjectFromTabMetroAndReset() {
        configureWebServer {
            registerMetros()
        }

        onScreen<GeoIntentScreen> {
            addButton.click()
            closeKeyboard()

            metroTab.click()

            onScreen<MetroSuggestScreen> {
                metroSuggestItem("Station 1")
                    .waitUntil { view.isCompletelyDisplayed() }
                    .click()
            }

            isViewStateMatches("GeoIntent/geoObjectFromTabMetro")

            resetButton.click()

            isViewStateMatches("GeoIntent/mapSearchMetro")
        }
    }

    @Test
    fun shouldAddGeoObjectFromTabDistrictAndReset() {
        configureWebServer {
            registerDistricts()
        }

        onScreen<GeoIntentScreen> {
            addButton.click()
            closeKeyboard()

            districtTab.click()

            onScreen<DistrictSuggestScreen> {
                districtSuggestItem("District 1")
                    .waitUntil { view.isCompletelyDisplayed() }
                    .click()
            }

            isViewStateMatches("GeoIntent/geoObjectFromTabDistrict")

            resetButton.click()

            isViewStateMatches("GeoIntent/mapSearchDistrict")
        }
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
}
