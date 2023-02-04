package com.yandex.mobile.realty.test.savedsearch

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnSaveSearchResultScreen
import com.yandex.mobile.realty.core.robot.performOnSaveSearchScreen
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchMapScreen
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
 * @author rogovalex on 26/06/2020.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SaveSearchOnMapTest {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = false, geoFilter = geoFilter),
        activityTestRule,
    )

    @Test
    fun shouldSaveSearchFromSearchMap() {
        val dispatcher = DispatcherRegistry()
        val expectedSaveRequest = dispatcher.register(
            request {
                method("PUT")
                path("2.0/savedSearch/saved-search-1")
            },
            response {
                assetBody("savedSearchesTest/saveSearchOnMap.json")
            }
        )
        val expectedSubscribeRequest = dispatcher.register(
            request {
                method("PUT")
                path("2.0/savedSearch/saved-search-1/subscription/push")
            },
            response {
                assetBody("savedSearchesTest/subscribeSavedSearchOnMap.json")
            }
        )
        configureWebServer(dispatcher)

        activityTestRule.launchActivity()

        onScreen<SearchMapScreen> {
            searchSubscribeButton.waitUntil { isCompletelyDisplayed() }
                .click()
        }

        performOnSaveSearchScreen {
            waitUntil {
                isNameEquals("Москва")
                isGeoRegionButtonHidden()
                isGeoDescriptionShown()
                isGeoDescriptionEquals("Область поиска: Москва")
            }

            tapOn(lookup.matchesSaveSearchButton())
        }

        performOnSaveSearchResultScreen {
            waitUntil { isSuccessViewShown() }

            tapOn(lookup.matchesDoneButton())
        }

        onScreen<SearchMapScreen> {
            mapView.waitUntil { isCompletelyDisplayed() }

            waitUntil { expectedSaveRequest.isOccured() }
            waitUntil { expectedSubscribeRequest.isOccured() }
        }
    }

    companion object {

        private val geoFilter = """
            [{
                "fullName": "Москва",
                "shortName": "Москва",
                "point": {"latitude": 55.75322,"longitude": 37.62251},
                "leftTop": {"latitude": 56.021286,"longitude": 36.803265},
                "rightBottom": {"latitude": 55.142227,"longitude": 37.967796},
                "type": "CITY",
                "params": {"rgid": ["587795"]}
            }]
        """.trimIndent()
    }
}
