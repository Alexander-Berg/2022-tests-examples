package com.yandex.mobile.realty.test.callButton

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.activity.SavedSearchOfferListActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.robot.performOnSavedSearchOffersScreen
import com.yandex.mobile.realty.core.rule.DatabaseRule
import com.yandex.mobile.realty.core.rule.DatabaseRule.Companion.createAddSavedSearchesEntryStatement
import com.yandex.mobile.realty.core.rule.MetricaEventsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.data.model.StoredSavedSearch
import com.yandex.mobile.realty.domain.model.geo.GeoIntent
import com.yandex.mobile.realty.domain.model.geo.GeoRegion
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
class SavedSearchOfferListCallButtonTest : CallButtonTest() {

    private val search = createStoredSavedSearch()

    private val activityTestRule = SavedSearchOfferListActivityTestRule(
        SEARCH_ID,
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        MetricaEventsRule(),
        DatabaseRule(createAddSavedSearchesEntryStatement(search)),
        GrantPermissionRule.grant(Permission.PHONE_CALL.value)
    )

    @Test
    fun shouldStartCallWhenOfferCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerOfferPhoneCallEvent(
            offerId = OFFER_ID,
            eventPlace = "SUBSCRIPTION_LISTING",
            currentScreen = "SUBSCRIPTIONS"
        )
        dispatcher.registerOffers()
        dispatcher.registerOfferPhone(OFFER_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = offerPhoneCallEvent(
            offerId = OFFER_ID,
            source = offerSnippet("в сохраненном поиске"),
            categories = jsonArrayOf("Sell", "SecondaryFlat_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnSavedSearchOffersScreen {
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
    fun shouldStartCallWhenSiteCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerSitePhoneCallEvent(
            siteId = SITE_ID,
            eventPlace = "SUBSCRIPTION_LISTING",
            currentScreen = "SUBSCRIPTIONS"
        )
        dispatcher.registerSites()
        dispatcher.registerSitePhone(SITE_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = sitePhoneCallEvent(
            siteId = SITE_ID,
            source = siteSnippet("в сохраненном поиске"),
            categories = jsonArrayOf("Sell", "ZhkNewbuilding_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnSavedSearchOffersScreen {
            waitUntil { containsSiteSnippet(SITE_ID) }
            performOnSiteSnippet(SITE_ID) {
                tapOn(lookup.matchesCallButton())
            }

            waitUntil { isCallStarted() }
            waitUntil { callMetricaEvent.isOccurred() }
            waitUntil { expectedCallRequest.isOccured() }
        }
    }

    @Test
    fun shouldStartCallWhenVillageCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerVillagePhoneCallEvent(
            villageId = VILLAGE_ID,
            eventPlace = "SUBSCRIPTION_LISTING",
            currentScreen = "SUBSCRIPTIONS"
        )
        dispatcher.registerVillages()
        dispatcher.registerVillagePhone(VILLAGE_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = villagePhoneCallEvent(
            villageId = VILLAGE_ID,
            source = villageSnippet("в сохраненном поиске"),
            categories = jsonArrayOf("Village_Sell", "Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnSavedSearchOffersScreen {
            waitUntil { containsVillageSnippet(VILLAGE_ID) }
            performOnVillageSnippet(VILLAGE_ID) {
                tapOn(lookup.matchesCallButton())
            }

            waitUntil { isCallStarted() }
            waitUntil { callMetricaEvent.isOccurred() }
            waitUntil { expectedCallRequest.isOccured() }
        }
    }

    private fun DispatcherRegistry.registerOffers() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
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
            },
            response {
                assetBody("offerWithSiteSearchVillage.json")
            }
        )
    }

    companion object {
        private const val SEARCH_ID = "a"
        private const val OFFER_ID = "0"
        private const val SITE_ID = "1"
        private const val VILLAGE_ID = "2"

        fun createStoredSavedSearch(): StoredSavedSearch {
            return StoredSavedSearch.of(
                SEARCH_ID,
                "test",
                Filter.SellApartment(),
                GeoIntent.Objects.valueOf(GeoRegion.DEFAULT)
            )
        }
    }
}
