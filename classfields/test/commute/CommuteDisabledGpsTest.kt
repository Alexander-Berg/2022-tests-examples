package com.yandex.mobile.realty.test.commute

import android.Manifest.permission.ACCESS_FINE_LOCATION
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.activity.FilterActivityTestRule
import com.yandex.mobile.realty.core.robot.performOnAddressSelectScreen
import com.yandex.mobile.realty.core.robot.performOnFiltersScreen
import com.yandex.mobile.realty.core.rule.DisableGpsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author andrikeev on 20/11/2019.
 */
class CommuteDisabledGpsTest {

    private val activityTestRule = FilterActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        GrantPermissionRule.grant(ACCESS_FINE_LOCATION),
        DisableGpsRule(),
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldSetDefaultAddressWhenGpsDisabled() {
        val regionLatitude = 55.75322
        val regionLongitude = 37.62251
        configureWebServer {
            registerGetAddress(
                "latitude" to regionLatitude.toString(),
                "longitude" to regionLongitude.toString()
            )
        }

        activityTestRule.launchActivity()

        performOnFiltersScreen {
            scrollToPosition(lookup.matchesFieldCommute()).tapOn()

            performOnAddressSelectScreen {
                waitUntil { isAddressContainerShown() }
                isAddressEquals("Красная площадь")
            }
        }
    }

    private fun DispatcherRegistry.registerGetAddress(
        vararg params: Pair<String, String?>?
    ) {
        register(
            request {
                path("1.0/addressGeocoder.json")
                for (item in params) {
                    item?.let { (name, value) ->
                        value?.let { queryParam(name, it) }
                    }
                }
            },
            response {
                assetBody("geocoderAddressMoscow.json")
            }
        )
    }
}
