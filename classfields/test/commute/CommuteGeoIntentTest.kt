package com.yandex.mobile.realty.test.commute

import com.yandex.mobile.realty.activity.FilterActivityTestRule
import com.yandex.mobile.realty.core.robot.performOnConfirmationDialog
import com.yandex.mobile.realty.core.robot.performOnFiltersScreen
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.AllSuggestScreen
import com.yandex.mobile.realty.core.screen.GeoIntentScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.filters.CommuteTime
import com.yandex.mobile.realty.test.filters.CommuteTransport
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author andrikeev on 22/11/2019.
 */
class CommuteGeoIntentTest {

    private val activityTestRule = FilterActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(
            commute = """{
                      point: {
                        latitude: $LATITUDE,
                        longitude: $LONGITUDE
                      },
                      address: "Садовническая улица, 82с2",
                      transport: ${TRANSPORT.value},
                      time: ${TIME.value}
                    }"""
        ),
        activityTestRule
    )

    @Test
    fun shouldResetCommuteWhenGeoSuggestSet() {
        configureWebServer {
            registerGeoSuggest()
            registerSearchCountOnlyCommute()
            registerSearchCountOnlyExcludeCommute()
            registerSearchCountOnlyExcludeCommute()
            registerSearchCountOnlyExcludeCommute()
        }

        activityTestRule.launchActivity()

        performOnFiltersScreen {
            scrollToPosition(lookup.matchesFieldCommute())
            isCommuteValueEquals(TIME.expected, TRANSPORT.expected, "Садовническая улица, 82с2")
            waitUntil { offersCountEquals(EXPECTED_COMMUTE_COUNT) }

            scrollToPosition(lookup.matchesGeoSuggestField()).tapOn()

            performOnConfirmationDialog {
                confirm()
            }
        }

        onScreen<GeoIntentScreen> {
            searchView.typeText("metro")

            onScreen<AllSuggestScreen> {
                geoSuggestItem("метро Площадь Революции")
                    .waitUntil { view.isCompletelyDisplayed() }
                    .click()
            }

            pressBack()
        }

        performOnFiltersScreen {
            scrollToPosition(lookup.matchesFieldCommute())
            isCommuteLabelShown()
            waitUntil { offersCountEquals(EXPECTED_NO_COMMUTE_COUNT) }
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

    private fun DispatcherRegistry.registerSearchCountOnlyCommute() {
        register(
            request {
                path("2.0/offers/number")
                queryParam("commutePointLatitude", LATITUDE.toString())
                queryParam("commutePointLongitude", LONGITUDE.toString())
                TRANSPORT.param.run { queryParam(first, second) }
                TIME.param.run { queryParam(first, second) }
            },
            response {
                setBody("{\"response\":{\"number\":$EXPECTED_COMMUTE_COUNT}}")
            }
        )
    }

    private fun DispatcherRegistry.registerSearchCountOnlyExcludeCommute() {
        register(
            request {
                path("2.0/offers/number")
                excludeQueryParamKey("commutePointLatitude")
                excludeQueryParamKey("commutePointLongitude")
                excludeQueryParamKey(TRANSPORT.param.first)
                excludeQueryParamKey(TIME.param.first)
            },
            response {
                setBody("{\"response\":{\"number\":$EXPECTED_NO_COMMUTE_COUNT}}")
            }
        )
    }

    private companion object {

        const val LATITUDE = 55.734655
        const val LONGITUDE = 37.642313
        val TRANSPORT = CommuteTransport.PUBLIC
        val TIME = CommuteTime.TWENTY_MIN
        const val EXPECTED_COMMUTE_COUNT = 100
        const val EXPECTED_NO_COMMUTE_COUNT = 50
    }
}
