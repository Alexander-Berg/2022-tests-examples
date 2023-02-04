package com.yandex.mobile.realty.test.chats

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchBottomSheetScreen
import com.yandex.mobile.realty.core.screen.SearchMapScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author rogovalex on 15/03/2021.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MapOffersChatButtonTest : ChatButtonTest() {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)
    private val authorizationRule = AuthorizationRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        authorizationRule
    )

    @Test
    fun shouldStartChatWhenMapOfferChatButtonPressed() {
        configureWebServer {
            registerMapSearchWithOneOffer(SNIPPET_OFFER_ID)
            registerOffers()
            registerSnippetOfferChat()
            registerSnippetOfferChatMessages()
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<SearchMapScreen> {
            waitUntil { mapView.isCompletelyDisplayed() }
            mapView.moveTo(LATITUDE, LONGITUDE)
            waitUntil { mapView.containsPlacemark(SNIPPET_OFFER_ID) }
            mapView.clickOnPlacemark(SNIPPET_OFFER_ID)

            onScreen<SearchBottomSheetScreen> {
                waitUntil { bottomSheet.isCollapsed() }

                offerSnippet(SNIPPET_OFFER_ID)
                    .waitUntil { listView.contains(this) }
                    .chatButton
                    .click()
            }
        }

        checkSnippetOfferChatViewState()
    }

    @Test
    fun shouldStartChatWhenMapSiteOfferChatButtonPressed() {
        configureWebServer {
            registerMapSearchWithOneOffer(SNIPPET_SITE_OFFER_ID)
            registerSiteOffers()
            registerSnippetSiteOfferChat()
            registerSnippetSiteOfferChatMessages()
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<SearchMapScreen> {
            waitUntil { mapView.isCompletelyDisplayed() }
            mapView.moveTo(LATITUDE, LONGITUDE)
            waitUntil { mapView.containsPlacemark(SNIPPET_SITE_OFFER_ID) }
            mapView.clickOnPlacemark(SNIPPET_SITE_OFFER_ID)

            onScreen<SearchBottomSheetScreen> {
                waitUntil { bottomSheet.isCollapsed() }

                offerSnippet(SNIPPET_SITE_OFFER_ID)
                    .waitUntil { listView.contains(this) }
                    .chatButton
                    .click()
            }
        }

        checkSnippetSiteOfferChatViewState()
    }

    @Test
    fun shouldStartChatWhenMultiHouseOfferChatButtonPressed() {
        configureWebServer {
            registerMapSearchWithMultiHouse()
            registerOffers()
            registerSnippetOfferChat()
            registerSnippetOfferChatMessages()
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<SearchMapScreen> {
            waitUntil { mapView.isCompletelyDisplayed() }
            mapView.moveTo(LATITUDE, LONGITUDE)
            waitUntil { mapView.containsPlacemark(MULTI_HOUSE_ID) }
            mapView.clickOnPlacemark(MULTI_HOUSE_ID)

            onScreen<SearchBottomSheetScreen> {
                waitUntil { bottomSheet.isCollapsed() }
                waitUntil { listView.contains(sortingButtonItem) }
                bottomSheet.expand()
                waitUntil { bottomSheet.isExpanded() }

                offerSnippet(SNIPPET_OFFER_ID)
                    .view
                    .chatButton
                    .click()
            }
        }

        checkSnippetOfferChatViewState()
    }

    @Test
    fun shouldStartChatWhenMultiHouseSiteOfferChatButtonPressed() {
        configureWebServer {
            registerMapSearchWithMultiHouse()
            registerSiteOffers()
            registerSnippetSiteOfferChat()
            registerSnippetSiteOfferChatMessages()
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<SearchMapScreen> {
            waitUntil { mapView.isCompletelyDisplayed() }
            mapView.moveTo(LATITUDE, LONGITUDE)
            waitUntil { mapView.containsPlacemark(MULTI_HOUSE_ID) }
            mapView.clickOnPlacemark(MULTI_HOUSE_ID)

            onScreen<SearchBottomSheetScreen> {
                waitUntil { bottomSheet.isCollapsed() }
                waitUntil { listView.contains(sortingButtonItem) }
                bottomSheet.expand()
                waitUntil { bottomSheet.isExpanded() }

                offerSnippet(SNIPPET_SITE_OFFER_ID)
                    .view
                    .chatButton
                    .click()
            }
        }

        checkSnippetSiteOfferChatViewState()
    }

    @Test
    fun shouldStartChatWhenMapYandexRentOfferChatButtonPressed() {
        configureWebServer {
            registerMapSearchWithOneOffer(YANDEX_RENT_OFFER_ID)
            registerYandexRentOffers()
            registerYandexRentOfferChat()
            registerYandexRentOfferChatMessages()
        }

        activityTestRule.launchActivity()
        authorizationRule.registerAuthorizationIntent()

        onScreen<SearchMapScreen> {
            waitUntil { mapView.isCompletelyDisplayed() }
            mapView.moveTo(LATITUDE, LONGITUDE)
            waitUntil { mapView.containsPlacemark(YANDEX_RENT_OFFER_ID) }
            mapView.clickOnPlacemark(YANDEX_RENT_OFFER_ID)

            onScreen<SearchBottomSheetScreen> {
                waitUntil { bottomSheet.isCollapsed() }

                offerSnippet(YANDEX_RENT_OFFER_ID)
                    .waitUntil { listView.contains(this) }
                    .chatButton
                    .click()
            }
        }

        checkSnippetYandexRentOfferChatViewState()
    }

    private fun DispatcherRegistry.registerMapSearchWithOneOffer(offerId: String) {
        register(
            request {
                path("1.0/pointStatisticSearch.json")
            },
            response {
                setBody(
                    """
                        {
                          "response" : {
                            "points" : [ {
                              "offerId" : "$offerId",
                              "latitude" : $LATITUDE,
                              "longitude" : $LONGITUDE,
                              "price" : 3608556,
                              "offersCount": 1
                            } ],
                            "totalOffers" : 1,
                            "favoriteLoadSuccess" : true,
                            "searchQuery" : {
                              "logQueryId" : "1b1c6273dd636cfa0c88f6581ad01001",
                              "url" : "offerSearchV2.json"
                            }
                          }
                        }
                    """.trimIndent()
                )
            }
        )
    }

    private fun DispatcherRegistry.registerMapSearchWithMultiHouse() {
        register(
            request {
                path("1.0/pointStatisticSearch.json")
            },
            response {
                setBody(
                    """
                        {
                          "response" : {
                            "points" : [ {
                              "offerId" : "$MULTI_HOUSE_ID",
                              "latitude" : $LATITUDE,
                              "longitude" : $LONGITUDE,
                              "price" : 3608556,
                              "offersCount": 1
                            } ],
                            "totalOffers" : 1,
                            "favoriteLoadSuccess" : true,
                            "searchQuery" : {
                              "logQueryId" : "1b1c6273dd636cfa0c88f6581ad01001",
                              "url" : "offerSearchV2.json"
                            }
                          }
                        }
                    """.trimIndent()
                )
                assetBody("MapOffersChatButtonTest/pointStatisticSearchMultiHouse.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOffers() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                excludeQueryParamKey("countOnly")
            },
            response {
                assetBody("ChatButtonTest/offerWithSiteSearch.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSiteOffers() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                excludeQueryParamKey("countOnly")
            },
            response {
                assetBody("ChatButtonTest/siteOfferSearch.json")
            }
        )
    }

    private fun DispatcherRegistry.registerYandexRentOffers() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                excludeQueryParamKey("countOnly")
            },
            response {
                assetBody("ChatButtonTest/yandexRentOfferSearch.json")
            }
        )
    }

    private companion object {

        private const val MULTI_HOUSE_ID = "asd"
        private const val LATITUDE = 67.43049
        private const val LONGITUDE = 32.70827
    }
}
