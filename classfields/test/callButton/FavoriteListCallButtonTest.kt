package com.yandex.mobile.realty.test.callButton

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.activity.FavoriteListActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.metrica.MetricaEvents.isOccurred
import com.yandex.mobile.realty.core.robot.performOnFavoritesScreen
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
class FavoriteListCallButtonTest : CallButtonTest() {

    private val activityTestRule = FavoriteListActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        MetricaEventsRule(),
        GrantPermissionRule.grant(Permission.PHONE_CALL.value)
    )

    @Test
    fun shouldStartCallWhenOfferCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerOfferPhoneCallEvent(
            offerId = OFFER_ID,
            eventPlace = "FAVOURITES",
            currentScreen = "FAVOURITES"
        )
        dispatcher.registerOffers()
        dispatcher.registerFavoriteIds(OFFER_ID)
        dispatcher.registerFavoriteIds(OFFER_ID)
        dispatcher.registerOfferPhone(OFFER_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = offerPhoneCallEvent(
            offerId = OFFER_ID,
            source = offerSnippet("на экране избранного"),
            categories = jsonArrayOf("Sell", "SecondaryFlat_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnFavoritesScreen {
            performOnFavoriteOffersScreen {

                waitUntil { containsOfferSnippet(OFFER_ID) }

                performOnOfferSnippet(OFFER_ID) {
                    tapOn(lookup.matchesCallButton())
                }

                waitUntil { isCallStarted() }
                waitUntil { callMetricaEvent.isOccurred() }
                waitUntil { expectedCallRequest.isOccured() }
            }
        }
    }

    @Test
    fun shouldStartCallWhenSiteCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerSitePhoneCallEvent(
            siteId = SITE_ID,
            eventPlace = "FAVOURITES",
            currentScreen = "FAVOURITES"
        )
        dispatcher.registerSites()
        dispatcher.registerFavoriteIds("site_$SITE_ID")
        dispatcher.registerFavoriteIds("site_$SITE_ID")
        dispatcher.registerFavoriteIds("site_$SITE_ID")
        dispatcher.registerSitePhone(SITE_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = sitePhoneCallEvent(
            siteId = SITE_ID,
            source = siteSnippet("на экране избранного"),
            categories = jsonArrayOf("Sell", "ZhkNewbuilding_Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnFavoritesScreen {
            performOnFavoriteOffersScreen {
                waitUntil { containsSelectorListView() }

                scrollSelectorsToPosition(lookup.matchesSiteSelectorItemView()).tapOn()
                waitUntil { containsSiteSnippet(SITE_ID) }

                performOnSiteSnippet(SITE_ID) {
                    tapOn(lookup.matchesCallButton())
                }

                waitUntil { isCallStarted() }
                waitUntil { callMetricaEvent.isOccurred() }
                waitUntil { expectedCallRequest.isOccured() }
            }
        }
    }

    @Test
    fun shouldStartCallWhenVillageCallButtonPressed() {
        val dispatcher = DispatcherRegistry()
        val expectedCallRequest = dispatcher.registerVillagePhoneCallEvent(
            villageId = VILLAGE_ID,
            eventPlace = "FAVOURITES",
            currentScreen = "FAVOURITES"
        )
        dispatcher.registerVillages()
        dispatcher.registerFavoriteIds("village_$VILLAGE_ID")
        dispatcher.registerFavoriteIds("village_$VILLAGE_ID")
        dispatcher.registerFavoriteIds("village_$VILLAGE_ID")
        dispatcher.registerVillagePhone(VILLAGE_ID)
        configureWebServer(dispatcher)

        val callMetricaEvent = villagePhoneCallEvent(
            villageId = VILLAGE_ID,
            source = villageSnippet("на экране избранного"),
            categories = jsonArrayOf("Village_Sell", "Sell")
        )

        activityTestRule.launchActivity()
        registerCallIntent()

        performOnFavoritesScreen {
            performOnFavoriteOffersScreen {
                waitUntil { containsSelectorListView() }

                scrollSelectorsToPosition(lookup.matchesVillageSelectorItemView()).tapOn()
                waitUntil { containsVillageSnippet(VILLAGE_ID) }

                performOnVillageSnippet(VILLAGE_ID) {
                    tapOn(lookup.matchesCallButton())
                }

                waitUntil { isCallStarted() }
                waitUntil { callMetricaEvent.isOccurred() }
                waitUntil { expectedCallRequest.isOccured() }
            }
        }
    }

    private fun DispatcherRegistry.registerOffers() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("offerId", OFFER_ID)
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
                queryParam("siteId", SITE_ID)
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
                queryParam("villageId", VILLAGE_ID)
            },
            response {
                assetBody("offerWithSiteSearchVillage.json")
            }
        )
    }

    private fun DispatcherRegistry.registerFavoriteIds(id: String) {
        register(
            request {
                path("1.0/favorites.json")
            },
            response {
                setBody(
                    """
                                {
                                   "response":{
                                      "actual":[
                                         "$id"
                                      ],
                                      "outdated":[],
                                      "relevant":[
                                         "$id"
                                      ]
                                   }
                                }
                    """.trimIndent()
                )
            }
        )
    }

    companion object {
        private const val OFFER_ID = "0"
        private const val SITE_ID = "1"
        private const val VILLAGE_ID = "2"
    }
}
