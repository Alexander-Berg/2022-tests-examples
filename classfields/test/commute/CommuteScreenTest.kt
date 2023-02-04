package com.yandex.mobile.realty.test.commute

import android.Manifest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.activity.FilterActivityTestRule
import com.yandex.mobile.realty.core.interaction.NamedViewInteraction.Companion.onView
import com.yandex.mobile.realty.core.robot.performOnAddressSelectScreen
import com.yandex.mobile.realty.core.robot.performOnAddressSuggestScreen
import com.yandex.mobile.realty.core.robot.performOnCommuteParamsScreen
import com.yandex.mobile.realty.core.robot.performOnFiltersScreen
import com.yandex.mobile.realty.core.robot.performOnSearchMapScreen
import com.yandex.mobile.realty.core.rule.MockLocationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.filters.CommuteTime.TWENTY_MIN
import com.yandex.mobile.realty.test.filters.CommuteTransport.PUBLIC
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author andrikeev on 23/11/2019.
 */
@SdkSuppress(minSdkVersion = 23)
class CommuteScreenTest {

    private val activityTestRule = FilterActivityTestRule(launchActivity = false)
    private val mockLocationRule = MockLocationRule()

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        mockLocationRule,
        GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION),
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldChangeAddressFromSuggest() {
        configureWebServer {
            registerGetAuroraAddress()
            registerGetMoscowAddress()
            registerGetGeoPointSuggest()
            registerGetGeoPointSuggest()
        }

        mockLocationRule.setMockLocation(AURORA_LATITUDE, AURORA_LONGITUDE)
        activityTestRule.launchActivity()

        performOnFiltersScreen {
            scrollToPosition(lookup.matchesFieldCommute()).tapOn()

            performOnAddressSelectScreen {
                waitUntil { isAddressContainerShown() }
                isAddressEquals(EXPECTED_ADDRESS)
                tapOn(lookup.matchesAddressView())
            }
            performOnAddressSuggestScreen {
                isAddressSearchTextEquals(EXPECTED_ADDRESS)
                tapOn(lookup.matchesClearSearchTextButton())
                typeSearchText("Moscow")
                waitUntil { containsSuggest("Москва") }
                tapOn(lookup.matchesGeoPointSuggest("Москва"))
            }
            performOnCommuteParamsScreen {
                waitUntil { isCommuteParamsShown() }
                isAddressEquals("Москва")
                tapOn(lookup.matchesAddressView())
            }
            performOnAddressSuggestScreen {
                isAddressSearchTextEquals("Москва")
                tapOn(lookup.matchesClearSearchTextButton())
                typeSearchText("Moscow")
                waitUntil { containsSuggest("Аврора") }
                tapOn(lookup.matchesGeoPointSuggest("Аврора"))
            }
            performOnCommuteParamsScreen {
                waitUntil { isCommuteParamsShown() }
                isAddressEquals("Аврора")
            }
        }
    }

    @Test
    fun shouldChangeAddressFromMap() {
        configureWebServer {
            registerGetAuroraAddress()
            registerGetAuroraAddress()
            registerGetAuroraAddress()
            registerGetAuroraAddress()
            registerGetAuroraAddress()
        }

        mockLocationRule.setMockLocation(AURORA_LATITUDE, AURORA_LONGITUDE)
        activityTestRule.launchActivity()

        performOnFiltersScreen {
            scrollToPosition(lookup.matchesFieldCommute()).tapOn()

            performOnAddressSelectScreen {
                waitUntil { isAddressContainerShown() }
                isAddressEquals(EXPECTED_ADDRESS)
                tapOn(lookup.matchesConfirmAddressButton())
            }
            performOnCommuteParamsScreen {
                waitUntil { isCommuteParamsShown() }
                isAddressEquals(EXPECTED_ADDRESS)
                onView(lookup.matchesMapView()).tapOnTopHalf()
            }
            performOnAddressSelectScreen {
                waitUntil { isAddressContainerShown() }
                isAddressEquals(EXPECTED_ADDRESS)
                tapOn(lookup.matchesConfirmAddressButton())
            }
            performOnCommuteParamsScreen {
                waitUntil { isCommuteParamsShown() }
                isAddressEquals(EXPECTED_ADDRESS)
                tapOn(lookup.matchesAddressView())
            }
            performOnAddressSuggestScreen {
                waitUntilKeyboardAppear()
                isSelectOnMapButtonShown()
                tapOn(lookup.matchesSelectOnMapButton())
            }
            performOnAddressSelectScreen {
                waitUntil { isAddressContainerShown() }
                isAddressEquals(EXPECTED_ADDRESS)
            }
        }
    }

    @Test
    fun shouldNotChangeOffersCountWhenAddressNotSubmitted() {
        val expectedOffersCount = 100
        configureWebServer {
            registerGetAuroraAddress()
            registerSearchCountOnly(expectedOffersCount)
            registerSearchCountOnly(expectedOffersCount)
        }

        activityTestRule.launchActivity()

        performOnFiltersScreen {
            scrollToPosition(lookup.matchesFieldCommute()).tapOn()

            performOnAddressSelectScreen {
                waitUntil { isAddressContainerShown() }
                pressBack()
            }
            isCommuteLabelShown()
            offersCountEquals(expectedOffersCount)
        }
    }

    @Test
    fun shouldNotChangeOffersCountWhenCommuteReset() {
        val expectedOffersCount = 100
        configureWebServer {
            registerGetAuroraAddress()
            registerSearchCountOnly(expectedOffersCount)
            registerSearchCountOnly(expectedOffersCount)
        }

        mockLocationRule.setMockLocation(AURORA_LATITUDE, AURORA_LONGITUDE)
        activityTestRule.launchActivity()

        performOnFiltersScreen {
            scrollToPosition(lookup.matchesFieldCommute()).tapOn()

            performOnAddressSelectScreen {
                waitUntil { isAddressContainerShown() }
                isAddressEquals(EXPECTED_ADDRESS)
                tapOn(lookup.matchesConfirmAddressButton())
            }
            performOnCommuteParamsScreen {
                waitUntil { isCommuteParamsShown() }
                isAddressEquals(EXPECTED_ADDRESS)
                tapOn(lookup.matchesResetButton())
            }
            isCommuteLabelShown()
            offersCountEquals(expectedOffersCount)
        }
    }

    @Test
    fun shouldShowSearchMapWhenCommuteSubmitted() {
        val expectedOffersCount = 100
        configureWebServer {
            registerGetAuroraAddress()
            registerMapSearch(expectedOffersCount)
        }

        mockLocationRule.setMockLocation(AURORA_LATITUDE, AURORA_LONGITUDE)
        activityTestRule.launchActivity()

        performOnFiltersScreen {
            scrollToPosition(lookup.matchesFieldCommute()).tapOn()

            performOnAddressSelectScreen {
                waitUntil { isAddressContainerShown() }
                isAddressEquals(EXPECTED_ADDRESS)
                tapOn(lookup.matchesConfirmAddressButton())
            }
            performOnCommuteParamsScreen {
                waitUntil { isCommuteParamsShown() }
                isAddressEquals(EXPECTED_ADDRESS)
                tapOn(lookup.matchesShowOffersButton())
            }
        }
        performOnSearchMapScreen {
            waitUntil { isGeoTitleShown() }

            hasCommute(TWENTY_MIN.expected, PUBLIC.expected, EXPECTED_ADDRESS)
            waitUntil { offersCountEquals(expectedOffersCount, expectedOffersCount) }
        }
    }

    @Test
    fun shouldShowUserLocationWhenTapLocationButton() {
        configureWebServer {
            registerGetMoscowAddress()
            registerGetAuroraAddress()
        }

        mockLocationRule.setMockLocation(REGION_LATITUDE, REGION_LONGITUDE)
        activityTestRule.launchActivity()

        performOnFiltersScreen {
            scrollToPosition(lookup.matchesFieldCommute()).tapOn()

            performOnAddressSelectScreen {
                waitUntil { isAddressContainerShown() }
                isAddressEquals("Красная площадь")

                mockLocationRule.setMockLocation(AURORA_LATITUDE, AURORA_LONGITUDE)

                tapOn(lookup.matchesLocationButton())
                waitUntil { isAddressEquals(EXPECTED_ADDRESS) }
            }
        }
    }

    private fun DispatcherRegistry.registerGetAuroraAddress() {
        register(
            request {
                path("1.0/addressGeocoder.json")
                queryParam("latitude", AURORA_LATITUDE.toString())
                queryParam("longitude", AURORA_LONGITUDE.toString())
            },
            response {
                assetBody("geocoderAddressAurora.json")
            }
        )
    }

    private fun DispatcherRegistry.registerGetMoscowAddress() {
        register(
            request {
                path("1.0/addressGeocoder.json")
                queryParam("latitude", REGION_LATITUDE.toString())
                queryParam("longitude", REGION_LONGITUDE.toString())
            },
            response {
                assetBody("geocoderAddressMoscow.json")
            }
        )
    }

    private fun DispatcherRegistry.registerGetGeoPointSuggest() {
        register(
            request {
                path("2.0/suggest/geo")
                queryParam("text", "Moscow")
            },
            response {
                assetBody("geoPointSuggest.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSearchCountOnly(
        expectedCount: Int
    ) {
        register(
            request {
                path("2.0/offers/number")
            },
            response {
                setBody("{\"response\":{\"number\":$expectedCount}}")
            }
        )
    }

    private fun DispatcherRegistry.registerMapSearch(
        expectedCount: Int,
        vararg params: Pair<String, String?>?
    ) {
        register(
            request {
                path("1.0/pointStatisticSearch.json")
                for (item in params) {
                    item?.let { (name, value) ->
                        value?.let { queryParam(name, it) }
                    }
                }
            },
            response {
                setBody(
                    """
                            {
                              "response": {
                                "totalOffers":$expectedCount,
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

    companion object {
        const val AURORA_LATITUDE = 55.734655
        const val AURORA_LONGITUDE = 37.642313
        const val REGION_LATITUDE = 55.75322
        const val REGION_LONGITUDE = 37.62251
        const val EXPECTED_ADDRESS = "Садовническая улица, 82с2"
    }
}
