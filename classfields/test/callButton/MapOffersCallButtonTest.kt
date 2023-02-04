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
class MapOffersCallButtonTest : CallButtonTest() {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        MetricaEventsRule(),
        GrantPermissionRule.grant(Permission.PHONE_CALL.value)
    )

    @Test
    fun shouldStartCallWhenMapOfferCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerOfferPhoneCallEvent(
            offerId = OFFER_ID,
            eventPlace = "MAP",
            currentScreen = "MAP"
        )
        dispatcher.registerMapSearchWithOneOffer()
        dispatcher.registerOffer()
        dispatcher.registerOfferPhone(OFFER_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = offerPhoneCallEvent(
            offerId = OFFER_ID,
            source = offerSnippet("на карте"),
            categories = jsonArrayOf("Sell", "SecondaryFlat_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnSearchMapScreen {
            waitUntil { isMapViewShown() }
            moveMapTo(LATITUDE, LONGITUDE)
            waitUntil { containsPin(OFFER_ID) }
            tapOnPin(OFFER_ID)
        }
        performOnSearchBottomSheetScreen {
            waitUntil { isCollapsed() }
            waitUntil { containsOfferSnippet(OFFER_ID) }
            performOnOfferSnippet(OFFER_ID) {
                tapOn(lookup.matchesCallButton())
            }

            waitUntil { isCallStarted() }
            waitUntil { callMetricaEvent.isOccurred() }
            waitUntil { expectedCallRequest.isOccured() }
        }
    }

    @Test
    fun shouldCallWhenMultiHouseOfferCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerOfferPhoneCallEvent(
            offerId = OFFER_ID,
            eventPlace = "MAP",
            currentScreen = "MAP"
        )
        dispatcher.registerMapSearchWithMultiHouse()
        dispatcher.registerOffer()
        dispatcher.registerOfferPhone(OFFER_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = offerPhoneCallEvent(
            offerId = OFFER_ID,
            source = offerSnippet("на карте"),
            categories = jsonArrayOf("Sell", "SecondaryFlat_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnSearchMapScreen {
            waitUntil { isMapViewShown() }
            moveMapTo(LATITUDE, LONGITUDE)
            waitUntil { containsPin(MULTI_HOUSE_ID) }
            tapOnPin(MULTI_HOUSE_ID)
        }
        performOnSearchBottomSheetScreen {
            waitUntil { isCollapsed() }
            waitUntil { containsOfferSnippet(OFFER_ID) }
            expand()
            waitUntil { isExpanded() }
            performOnOfferSnippet(OFFER_ID) {
                tapOn(lookup.matchesCallButton())
            }

            waitUntil { isCallStarted() }
            waitUntil { callMetricaEvent.isOccurred() }
            waitUntil { expectedCallRequest.isOccured() }
        }
    }

    private fun DispatcherRegistry.registerMapSearchWithOneOffer() {
        register(
            request {
                path("1.0/pointStatisticSearch.json")
            },
            response {
                assetBody("callButtonTest/pointStatisticSearch.json")
            }
        )
    }

    private fun DispatcherRegistry.registerMapSearchWithMultiHouse() {
        register(
            request {
                path("1.0/pointStatisticSearch.json")
            },
            response {
                assetBody("callButtonTest/pointStatisticSearchMultiHouse.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOffer() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                excludeQueryParamKey("countOnly")
            },
            response {
                assetBody("callButtonTest/offerWithSiteSearch.json")
            }
        )
    }

    companion object {
        private const val OFFER_ID = "0"
        private const val MULTI_HOUSE_ID = "3"
        private const val LATITUDE = 55.75793
        private const val LONGITUDE = 37.597424
    }
}
