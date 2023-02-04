package com.yandex.mobile.realty.test.commute

import android.Manifest
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.activity.FilterActivityTestRule
import com.yandex.mobile.realty.core.robot.performOnAddressSelectScreen
import com.yandex.mobile.realty.core.robot.performOnCommuteParamsScreen
import com.yandex.mobile.realty.core.robot.performOnFiltersScreen
import com.yandex.mobile.realty.core.rule.MockLocationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.filters.CommuteTime
import com.yandex.mobile.realty.test.filters.CommuteTime.FIFTEEN_MIN
import com.yandex.mobile.realty.test.filters.CommuteTime.FORTY_FIVE_MIN
import com.yandex.mobile.realty.test.filters.CommuteTime.TEN_MIN
import com.yandex.mobile.realty.test.filters.CommuteTime.THIRTY_MIN
import com.yandex.mobile.realty.test.filters.CommuteTime.TWENTY_MIN
import com.yandex.mobile.realty.test.filters.CommuteTransport
import com.yandex.mobile.realty.test.filters.CommuteTransport.AUTO
import com.yandex.mobile.realty.test.filters.CommuteTransport.BY_FOOT
import com.yandex.mobile.realty.test.filters.CommuteTransport.PUBLIC
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * @author andrikeev on 23/11/2019.
 */
@SdkSuppress(minSdkVersion = 23)
@RunWith(Parameterized::class)
class CommuteParamsTest(
    private val transport: CommuteTransport,
    private val time: CommuteTime
) {

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
    fun shouldChangeOffersCountWhenChangeCommuteParams() {
        configureWebServer {
            registerGetAuroraAddress()
            registerGetPolygonAndCount()
        }

        mockLocationRule.setMockLocation(AURORA_LATITUDE, AURORA_LONGITUDE)
        activityTestRule.launchActivity()

        performOnFiltersScreen {
            scrollToPosition(lookup.matchesFieldCommute()).tapOn()

            performOnAddressSelectScreen {
                waitUntil { isAddressContainerShown() }
                tapOn(lookup.matchesConfirmAddressButton())
            }
            performOnCommuteParamsScreen {
                waitUntil { isCommuteParamsShown() }
                tapOn(transport.matcher.invoke(lookup))
                tapOn(time.matcher.invoke(lookup))
                waitUntil { isOffersCountEquals(EXPECTED_OFFERS_COUNT) }
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

    private fun DispatcherRegistry.registerGetPolygonAndCount() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("polygonAndCount", "YES")
                queryParam("commutePointLatitude", AURORA_LATITUDE.toString())
                queryParam("commutePointLongitude", AURORA_LONGITUDE.toString())
                queryParam(transport.param.first, transport.param.second)
                queryParam(time.param.first, time.param.second)
            },
            response {
                setBody("{\"response\":{\"total\":$EXPECTED_OFFERS_COUNT}}")
            }
        )
    }

    private companion object {
        const val AURORA_LATITUDE = 55.734655
        const val AURORA_LONGITUDE = 37.642313
        const val EXPECTED_OFFERS_COUNT = 100

        @JvmStatic
        @Parameterized.Parameters(name = "{1} {0}")
        fun params(): Iterable<Array<*>> {
            return listOf(
                arrayOf(BY_FOOT, TEN_MIN),
                arrayOf(BY_FOOT, FIFTEEN_MIN),
                arrayOf(BY_FOOT, TWENTY_MIN),
                arrayOf(BY_FOOT, THIRTY_MIN),
                arrayOf(BY_FOOT, FORTY_FIVE_MIN),
                arrayOf(AUTO, TEN_MIN),
                arrayOf(AUTO, FIFTEEN_MIN),
                arrayOf(AUTO, TWENTY_MIN),
                arrayOf(AUTO, THIRTY_MIN),
                arrayOf(AUTO, FORTY_FIVE_MIN),
                arrayOf(PUBLIC, TEN_MIN),
                arrayOf(PUBLIC, FIFTEEN_MIN),
                arrayOf(PUBLIC, TWENTY_MIN),
                arrayOf(PUBLIC, THIRTY_MIN),
                arrayOf(PUBLIC, FORTY_FIVE_MIN)
            )
        }
    }
}
