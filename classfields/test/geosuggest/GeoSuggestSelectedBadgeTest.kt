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
 * @author misha-kozlov on 23.10.2020
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class GeoSuggestSelectedBadgeTest {

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(GeoIntentActivityTestRule())

    @Test
    fun shouldShowSelectedBadgeInTabAll() {
        configureWebServer {
            registerGeoSuggest()
            registerGeoSuggest()
        }

        onScreen<GeoIntentScreen> {
            searchView.typeText("metro")

            onScreen<AllSuggestScreen> {
                geoSuggestItem("метро Площадь Революции")
                    .waitUntil { view.isCompletelyDisplayed() }
                    .click()
            }

            searchView.typeText("metro")

            onScreen<AllSuggestScreen> {
                geoSuggestItem("метро Площадь Революции")
                    .waitUntil { view.isCompletelyDisplayed() }
                    .isViewStateMatches(
                        "GeoSuggestSelectedBadgeTest/shouldShowSelectedBadgeInTabAll/" +
                            "selectedBadge"
                    )
            }
        }
    }

    @Test
    fun shouldShowSelectedBadgeInTabMetro() {
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
                    .click()
            }

            addButton.click()
            closeKeyboard()

            onScreen<MetroSuggestScreen> {
                metroSuggestItem("Group")
                    .waitUntil { view.isCompletelyDisplayed() }

                listView.isViewStateMatches(
                    "GeoSuggestSelectedBadgeTest/shouldShowSelectedBadgeInTabMetro/" +
                        "selectedBadge"
                )
            }
        }
    }

    @Test
    fun shouldShowSelectedBadgeInTabDistrict() {
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
                    .click()
            }

            addButton.click()
            closeKeyboard()

            onScreen<DistrictSuggestScreen> {
                districtSuggestItem("ЦАО")
                    .waitUntil { view.isCompletelyDisplayed() }

                listView.isViewStateMatches(
                    "GeoSuggestSelectedBadgeTest/shouldShowSelectedBadgeInTabDistrict/" +
                        "selectedBadge"
                )
            }
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
