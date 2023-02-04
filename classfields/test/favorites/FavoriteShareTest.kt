package com.yandex.mobile.realty.test.favorites

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.FavoriteListActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.interaction.NamedIntents.intended
import com.yandex.mobile.realty.core.matchesShareIntent
import com.yandex.mobile.realty.core.registerResultOkIntent
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.FavoriteOffersScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class FavoriteShareTest {

    private val activityTestRule = FavoriteListActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldShareFavoriteOffers() {
        configureWebServer {
            registerFavoriteIds(OFFER_ID)
            registerFavoriteIds(OFFER_ID)
            registerOffer()
            registerShortLink(objectId = OFFER_ID, type = TYPE_OFFER)
        }

        activityTestRule.launchActivity()
        registerResultOkIntent(matchesShareIntent(SHORT_LINK), null)

        onScreen<FavoriteOffersScreen> {
            shareButton
                .waitUntil { isCompletelyDisplayed() }
                .also {
                    root.isViewStateMatches(
                        "FavoriteShareTest/shouldShareFavoriteOffers/shareButton"
                    )
                }
                .click()

            intended(matchesShareIntent(SHORT_LINK))
        }
    }

    @Test
    fun shouldShareFavoriteSites() {
        configureWebServer {
            registerFavoriteIds("site_$SITE_ID")
            registerFavoriteIds("site_$SITE_ID")
            registerFavoriteIds("site_$SITE_ID")
            registerSite()
            registerShortLink(objectId = SITE_ID, type = TYPE_SITE)
        }

        activityTestRule.launchActivity()
        registerResultOkIntent(matchesShareIntent(SHORT_LINK), null)

        onScreen<FavoriteOffersScreen> {
            selectorListView
                .waitUntil { listView.contains(this) }
                .scrollTo(siteSelectorItem)
                .click()

            siteSnippet(SITE_ID)
                .waitUntil { listView.contains(this) }

            root.isViewStateMatches("FavoriteShareTest/shouldShareFavoriteSites/shareButton")
            shareButton.click()

            intended(matchesShareIntent(SHORT_LINK))
        }
    }

    @Test
    fun shouldShareFavoriteVillages() {
        configureWebServer {
            registerFavoriteIds("village_$VILLAGE_ID")
            registerFavoriteIds("village_$VILLAGE_ID")
            registerFavoriteIds("village_$VILLAGE_ID")
            registerVillage()
            registerShortLink(objectId = VILLAGE_ID, type = TYPE_VILLAGE)
        }

        activityTestRule.launchActivity()
        registerResultOkIntent(matchesShareIntent(SHORT_LINK), null)

        onScreen<FavoriteOffersScreen> {
            selectorListView
                .waitUntil { listView.contains(this) }
                .scrollTo(villageSelectorItem)
                .click()

            villageSnippet(VILLAGE_ID)
                .waitUntil { listView.contains(this) }

            root.isViewStateMatches("FavoriteShareTest/shouldShareFavoriteVillages/shareButton")
            shareButton.click()

            intended(matchesShareIntent(SHORT_LINK))
        }
    }

    private fun DispatcherRegistry.registerShortLink(objectId: String, type: String) {
        register(
            request {
                path("2.0/favorites/shortLink")
                method("POST")
                body(
                    """
                    {
                        "objectIds": [ "$objectId" ],
                        "type": "$type"
                    }
                    """.trimIndent()
                )
            },
            response {
                setBody("""{ "response": { "url": "$SHORT_LINK" } }""")
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

    private fun DispatcherRegistry.registerOffer() {
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

    private fun DispatcherRegistry.registerSite() {
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

    private fun DispatcherRegistry.registerVillage() {
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

    companion object {
        private const val OFFER_ID = "0"
        private const val SITE_ID = "1"
        private const val VILLAGE_ID = "2"

        private const val TYPE_OFFER = "OFFER"
        private const val TYPE_SITE = "NEWBUILDING"
        private const val TYPE_VILLAGE = "VILLAGE"

        private const val SHORT_LINK = "https://realty.yandex.ru/shared-favorites/abracadabra/"
    }
}
