package com.yandex.mobile.realty.test.callButton

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.robot.performOnSearchBottomSheetScreen
import com.yandex.mobile.realty.core.robot.performOnSearchMapScreen
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.domain.model.search.Filter
import com.yandex.mobile.realty.permission.Permission
import com.yandex.mobile.realty.utils.jsonArrayOf
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author misha-kozlov on 22.04.2020
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MapVillageCallButtonTest : CallButtonTest() {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(filter = Filter.VillageHouse()),
        activityTestRule,
        MetricaEventsRule(),
        GrantPermissionRule.grant(Permission.PHONE_CALL.value)
    )

    @Test
    fun shouldStartCallWhenMapVillageCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerVillagePhoneCallEvent(
            villageId = VILLAGE_ID,
            eventPlace = "VILLAGE_MAP",
            currentScreen = "MAP"
        )
        dispatcher.registerMapSearchWithOneVillage()
        dispatcher.registerVillage()
        dispatcher.registerVillagePhone(VILLAGE_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = villagePhoneCallEvent(
            villageId = VILLAGE_ID,
            source = villageSnippet("на карте"),
            categories = jsonArrayOf("Village_Sell", "Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnSearchMapScreen {
            waitUntil { isMapViewShown() }
            moveMapTo(LATITUDE, LONGITUDE)
            waitUntil { containsPin(VILLAGE_ID) }
            tapOnPin(VILLAGE_ID)
        }
        performOnSearchBottomSheetScreen {
            waitUntil { isCollapsed() }
            waitUntil { containsVillageSnippet(VILLAGE_ID) }
            performOnVillageSnippet(VILLAGE_ID) {
                tapOn(lookup.matchesCallButton())
            }

            waitUntil { isCallStarted() }
            waitUntil { callMetricaEvent.isOccurred() }
            waitUntil { expectedCallRequest.isOccured() }
        }
    }

    private fun DispatcherRegistry.registerMapSearchWithOneVillage() {
        register(
            request {
                path("2.0/village/pointSearch")
            },
            response {
                assetBody("callButtonTest/villagePointSearch.json")
            }
        )
    }

    private fun DispatcherRegistry.registerVillage() {
        register(
            request {
                method("GET")
                path("1.0/offerWithSiteSearch.json")
                excludeQueryParamKey("countOnly")
            },
            response {
                assetBody("offerWithSiteSearchVillage.json")
            }
        )
    }

    companion object {
        private const val VILLAGE_ID = "2"
        private const val LATITUDE = 55.75793
        private const val LONGITUDE = 37.597424
    }
}
