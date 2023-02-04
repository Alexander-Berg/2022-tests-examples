package com.yandex.mobile.realty.test.notes

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.FavoriteListActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnFavoritesScreen
import com.yandex.mobile.realty.core.robot.performOnNoteScreen
import com.yandex.mobile.realty.core.robot.performOnOfferMenuDialog
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author misha-kozlov on 1/26/21
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class FavoriteListNotesTest : NotesTest() {

    private val activityTestRule = FavoriteListActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldAddNote() {
        configureWebServer {
            registerOffer()
            registerFavoriteIds(OFFER_ID)
            registerFavoriteIds(OFFER_ID)
            registerNoteSaving(OFFER_ID, TEXT)
        }

        activityTestRule.launchActivity()

        performOnFavoritesScreen {
            performOnFavoriteOffersScreen {
                waitUntil { containsOfferSnippet(OFFER_ID) }
                scrollByFloatingButtonHeight()
                performOnOfferSnippet(OFFER_ID) {
                    tapOn(lookup.matchesMenuButton())
                }
            }
        }
        performOnOfferMenuDialog {
            isAddNoteButtonShown()
            tapOn(lookup.matchesAddNoteButton())
        }
        performOnNoteScreen {
            typeText(lookup.matchesInputView(), TEXT)
            tapOn(lookup.matchesSubmitButton())
        }
        performOnFavoritesScreen {
            performOnFavoriteOffersScreen {
                waitUntil { containsOfferSnippet(OFFER_ID) }
                performOnOfferSnippet(OFFER_ID) {
                    isNoteShown(TEXT)
                }
            }
        }
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

    private fun DispatcherRegistry.registerOffer() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("offerId", OFFER_ID)
            },
            response {
                setBody(
                    """
                        {
                            "response": {
                                "offers": {
                                    "items": [
                                        {
                                            "offerId": "$OFFER_ID",
                                            "offerType": "SELL",
                                            "offerCategory": "APARTMENT",
                                            "active": true,
                                            "price": {
                                                "trend": "UNCHANGED",
                                                "price": {
                                                    "value": 14990000,
                                                    "currency": "RUB",
                                                    "priceType": "PER_OFFER",
                                                    "pricingPeriod": "WHOLE_LIFE"
                                                }
                                            }
                                        }
                                    ]
                                },
                                "pager": {
                                    "totalItems": 1
                                },
                                "searchQuery": {
                                    "logQueryId" : "b7128fa3fad87a0c",
                                    "url" : "offerSearchV2.json"
                                },
                                "timeStamp": "2020-02-03T09:07:02.980Z"
                            }
                        }
                    """.trimIndent()
                )
            }
        )
    }
}
