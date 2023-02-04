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
class MapSiteCallButtonTest : CallButtonTest() {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(filter = Filter.SiteApartment()),
        activityTestRule,
        MetricaEventsRule(),
        GrantPermissionRule.grant(Permission.PHONE_CALL.value)
    )

    @Test
    fun shouldStartCallWhenMapSiteCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerSitePhoneCallEvent(
            siteId = SITE_ID,
            eventPlace = "NEWBUILDING_MAP",
            currentScreen = "MAP"
        )
        dispatcher.registerMapSearchWithOneSite()
        dispatcher.registerSite()
        dispatcher.registerSitePhone(SITE_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = sitePhoneCallEvent(
            siteId = SITE_ID,
            source = siteSnippet("на карте"),
            categories = jsonArrayOf("Sell", "ZhkNewbuilding_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnSearchMapScreen {
            waitUntil { isMapViewShown() }
            moveMapTo(LATITUDE, LONGITUDE)
            waitUntil { containsPin(SITE_ID) }
            tapOnPin(SITE_ID)
        }
        performOnSearchBottomSheetScreen {
            waitUntil { isCollapsed() }
            waitUntil { containsSiteSnippet(SITE_ID) }
            performOnSiteSnippet(SITE_ID) {
                tapOn(lookup.matchesCallButton())
            }

            waitUntil { isCallStarted() }
            waitUntil { callMetricaEvent.isOccurred() }
            waitUntil { expectedCallRequest.isOccured() }
        }
    }

    private fun DispatcherRegistry.registerMapSearchWithOneSite() {
        register(
            request {
                path("2.0/newbuilding/pointSearch")
            },
            response {
                assetBody("callButtonTest/newbuildingPointSearch.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSite() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                excludeQueryParamKey("countOnly")
            },
            response {
                assetBody("offerWithSiteSearchSite.json")
            }
        )
    }

    companion object {
        private const val SITE_ID = "1"
        private const val LATITUDE = 55.75793
        private const val LONGITUDE = 37.597424
    }
}
