package com.yandex.mobile.realty.test.callButton

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.robot.performOnPhonesDialog
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
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
class SearchListCallButtonTest : CallButtonTest() {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true),
        MetricaEventsRule(),
        activityTestRule,
        GrantPermissionRule.grant(Permission.PHONE_CALL.value)
    )

    @Test
    fun shouldStartCallWhenOfferCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerOfferPhoneCallEvent(
            offerId = OFFER_ID,
            eventPlace = "LISTING",
            currentScreen = "LISTING"
        )
        dispatcher.registerOffers()
        dispatcher.registerOfferPhone(OFFER_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = offerPhoneCallEvent(
            offerId = OFFER_ID,
            source = offerSnippet("в листинге"),
            categories = jsonArrayOf("Sell", "SecondaryFlat_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        onScreen<SearchListScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .invoke {
                    callButton.click()
                }

            waitUntil { isCallStarted() }
            waitUntil { expectedCallRequest.isOccured() }
            waitUntil { callMetricaEvent.isOccurred() }
        }
    }

    @Test
    fun shouldStartCallWhenFewPhoneNumbersExists() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerOfferPhoneCallEvent(
            offerId = OFFER_ID,
            eventPlace = "LISTING",
            currentScreen = "LISTING"
        )
        dispatcher.registerOffers()
        dispatcher.registerFewOfferPhones(OFFER_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = offerPhoneCallEvent(
            offerId = OFFER_ID,
            source = offerSnippet("в листинге"),
            categories = jsonArrayOf("Sell", "SecondaryFlat_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        onScreen<SearchListScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .invoke {
                    callButton.click()
                }
        }
        performOnPhonesDialog {
            waitUntil { isPhoneShown(PHONE) }
            tapOn(lookup.matchesPhone(PHONE))

            waitUntil { isCallStarted() }
            waitUntil { expectedCallRequest.isOccured() }
            waitUntil { callMetricaEvent.isOccurred() }
        }
    }

    @Test
    fun shouldStartCallWhenSiteCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerSitePhoneCallEvent(
            siteId = SITE_ID,
            eventPlace = "NEWBUILDING_LISTING",
            currentScreen = "LISTING"
        )
        dispatcher.registerSites()
        dispatcher.registerSitePhone(SITE_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = sitePhoneCallEvent(
            siteId = SITE_ID,
            source = siteSnippet("в листинге"),
            categories = jsonArrayOf("Sell", "ZhkNewbuilding_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        onScreen<SearchListScreen> {
            siteSnippet(SITE_ID)
                .waitUntil { listView.contains(this) }
                .invoke {
                    callButton.click()
                }

            waitUntil { isCallStarted() }
            waitUntil { expectedCallRequest.isOccured() }
            waitUntil { callMetricaEvent.isOccurred() }
        }
    }

    @Test
    fun shouldStartCallWhenVillageCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerVillagePhoneCallEvent(
            villageId = VILLAGE_ID,
            eventPlace = "VILLAGE_LISTING",
            currentScreen = "LISTING"
        )
        dispatcher.registerVillages()
        dispatcher.registerVillagePhone(VILLAGE_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = villagePhoneCallEvent(
            villageId = VILLAGE_ID,
            source = villageSnippet("в листинге"),
            categories = jsonArrayOf("Village_Sell", "Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        onScreen<SearchListScreen> {
            villageSnippet(VILLAGE_ID)
                .waitUntil { listView.contains(this) }
                .invoke {
                    callButton.click()
                }

            waitUntil { isCallStarted() }
            waitUntil { expectedCallRequest.isOccured() }
            waitUntil { callMetricaEvent.isOccurred() }
        }
    }

    private fun DispatcherRegistry.registerOffers() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
            },
            response {
                assetBody("callButtonTest/offerWithSiteSearch.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSites() {
        register(
            request {
                method("GET")
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
            },
            response {
                assetBody("offerWithSiteSearchSite.json")
            }
        )
    }

    private fun DispatcherRegistry.registerVillages() {
        register(
            request {
                method("GET")
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
            },
            response {
                assetBody("offerWithSiteSearchVillage.json")
            }
        )
    }

    companion object {
        private const val OFFER_ID = "0"
        private const val SITE_ID = "1"
        private const val VILLAGE_ID = "2"
    }
}
