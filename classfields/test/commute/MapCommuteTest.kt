package com.yandex.mobile.realty.test.commute

import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnCommuteParamsScreen
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchMapScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.filters.CommuteTime
import com.yandex.mobile.realty.test.filters.CommuteTransport
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author andrikeev on 06/12/2019.
 */
class MapCommuteTest {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(
            commute = """
                                      {
                                        point: {
                                          latitude: $LATITUDE,
                                          longitude: $LONGITUDE
                                        },
                                        address: "$EXPECTED_COMMUTE_ADDRESS",
                                        transport: ${TRANSPORT.value},
                                        time: ${TIME.value}
                                      }
            """.trimIndent()
        ),
        activityTestRule
    )

    @Test
    fun shouldChangeOffersCountWhenCommuteResetFromMapBadge() {
        configureWebServer {
            registerMapSearchIncludeCommute()
            registerMapSearchExcludeCommute()
        }

        activityTestRule.launchActivity()

        onScreen<SearchMapScreen> {
            val geo = "${TIME.expected} минут ${TRANSPORT.expected} до $EXPECTED_COMMUTE_ADDRESS"
            waitUntil { geoTitleView.isTextEquals(geo) }
            waitUntil { offersCountEquals(EXPECTED_COMMUTE_COUNT, EXPECTED_COMMUTE_COUNT) }
            geoResetButton.click()
            waitUntil { offersCountEquals(EXPECTED_NO_COMMUTE_COUNT, EXPECTED_NO_COMMUTE_COUNT) }
        }
    }

    @Test
    fun shouldShowCommuteParamsWhenClickOnBadge() {
        activityTestRule.launchActivity()

        onScreen<SearchMapScreen> {
            val geo = "${TIME.expected} минут ${TRANSPORT.expected} до $EXPECTED_COMMUTE_ADDRESS"
            waitUntil { geoTitleView.isTextEquals(geo) }
            geoFieldView.click()
            performOnCommuteParamsScreen {
                waitUntil { isCommuteParamsShown() }
                isAddressEquals(EXPECTED_COMMUTE_ADDRESS)
                isPublicTransportSelected()
                isTwentyMinTimeSelected()
            }
        }
    }

    private fun DispatcherRegistry.registerMapSearchIncludeCommute() {
        register(
            request {
                path("1.0/pointStatisticSearch.json")
                queryParam("commutePointLatitude", LATITUDE.toString())
                queryParam("commutePointLongitude", LONGITUDE.toString())
                TRANSPORT.param.run { queryParam(first, second) }
                TIME.param.run { queryParam(first, second) }
            },
            response {
                setBody(
                    """
                            {
                              "response": {
                                "totalOffers":$EXPECTED_COMMUTE_COUNT,
                                "searchQuery" : {
                                  "logQueryId" : "b7128fa3fad87a0c",
                                  "url" : "offerSearchV2.json"
                                }
                              }
                            }
                    """.trimIndent()
                )
            }
        )
    }

    private fun DispatcherRegistry.registerMapSearchExcludeCommute() {
        register(
            request {
                path("1.0/pointStatisticSearch.json")
                excludeQueryParamKey("commutePointLatitude")
                excludeQueryParamKey("commutePointLongitude")
                excludeQueryParamKey(TRANSPORT.param.first)
                excludeQueryParamKey(TIME.param.first)
            },
            response {
                setBody(
                    """
                            {
                              "response": {
                                "totalOffers":$EXPECTED_NO_COMMUTE_COUNT,
                                "searchQuery" : {
                                  "logQueryId" : "b7128fa3fad87a0c",
                                  "url" : "offerSearchV2.json"
                                }
                              }
                            }
                    """.trimIndent()
                )
            }
        )
    }

    private companion object {
        const val LATITUDE = 55.734655
        const val LONGITUDE = 37.642313
        val TRANSPORT = CommuteTransport.PUBLIC
        val TIME = CommuteTime.TWENTY_MIN
        const val EXPECTED_COMMUTE_ADDRESS = "Садовническая улица, 82с2"
        const val EXPECTED_COMMUTE_COUNT = 100
        const val EXPECTED_NO_COMMUTE_COUNT = 500
    }
}
