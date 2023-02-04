package com.yandex.mobile.realty.test.deeplink

import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.core.DeepLinkIntentCommand
import com.yandex.mobile.realty.core.interaction.NamedIntents.intended
import com.yandex.mobile.realty.core.matchesShareIntent
import com.yandex.mobile.realty.core.registerResultOkIntent
import com.yandex.mobile.realty.core.robot.performOnChatMessagesScreen
import com.yandex.mobile.realty.core.robot.performOnCommunicationScreen
import com.yandex.mobile.realty.core.robot.performOnFavoritesScreen
import com.yandex.mobile.realty.core.robot.performOnFiltersScreen
import com.yandex.mobile.realty.core.robot.performOnOfferCardScreen
import com.yandex.mobile.realty.core.robot.performOnSearchMapScreen
import com.yandex.mobile.realty.core.robot.performOnSiteCardScreen
import com.yandex.mobile.realty.core.robot.performOnUserOffersScreen
import com.yandex.mobile.realty.core.robot.performOnVillageCardScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.ConciergeProposalScreen
import com.yandex.mobile.realty.core.screen.FavoriteScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.screen.SharedFavoritesScreen
import com.yandex.mobile.realty.core.screen.SimilarOffersScreen
import com.yandex.mobile.realty.core.screen.WebViewScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.search.Sorting
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author pvl-zolotov on 10.12.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class AmpDeepLinkTest {

    private val authorizationRule = AuthorizationRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule
    )

    @Test
    fun shouldOpenSharedFavorites() {
        val deepLink = "https://realty.yandex.ru/amp/shared-favorites/G.R2MwZJHWR.ogDT2/"

        configureWebServer {
            registerDeepLink(deepLink, "deepLinkTest/deepLinkSharedFavorites.json")
        }

        DeepLinkIntentCommand.execute(deepLink)

        onScreen<SharedFavoritesScreen> {
            toolbarTitleView
                .waitUntil { isCompletelyDisplayed() }
                .isTextEquals(R.string.shared_favorites_offers)
            pressBack()
        }
        onScreen<FavoriteScreen> {
            favoriteOffersTabView.isSelected()
        }
    }

    @Test
    fun shouldOpenSimilarOffers() {
        val offerId = "0"
        val deepLink = "https://realty.yandex.ru/amp/offer/$offerId/similar"

        configureWebServer {
            register(
                request {
                    path("1.0/offerWithSiteSearch.json")
                    queryParam("offerId", offerId)
                },
                response {
                    assetBody("SimilarOffersTest/offerWithSiteSearch.json")
                }
            )
            register(
                request {
                    path("1.0/offer/$offerId/similar")
                },
                response {
                    assetBody("SimilarOffersTest/similar.json")
                }
            )
        }

        DeepLinkIntentCommand.execute(deepLink)

        onScreen<SimilarOffersScreen> {
            toolbarTitleView
                .waitUntil { isCompletelyDisplayed() }
                .isTextEquals(R.string.similar_offers_title)

            recentOfferView
                .waitUntil { listView.contains(this) }
                .isViewStateMatches("SimilarOffersTest/infoView")
        }
    }

    @Test
    fun shouldOpenRentRoomOffersInSpbSearchList() {
        val deepLink = "https://realty.yandex.ru/amp/sankt-peterburg/snyat/komnata"

        configureWebServer {
            registerDeepLink(deepLink, "deepLinkTest/deepLinkRentRoomSpb.json")
            registerRegionInfoSPB()
        }

        DeepLinkIntentCommand.execute(deepLink)

        onScreen<SearchListScreen> {
            listView.waitUntil { isCompletelyDisplayed() }
            filterButton.click()
        }

        performOnFiltersScreen {
            isRentSelected()
            isRoomSelected()
            geoSuggestEquals("Город Санкт-Петербург")
        }
    }

    @Test
    fun shouldOpenSitesInSpbSearchList() {
        val deepLink = "https://realty.yandex.ru/amp/sankt-peterburg/kupit/novostrojka"

        configureWebServer {
            registerDeepLink(deepLink, "deepLinkTest/deepLinkSiteListSpb.json")
            registerRegionInfoSPB()
        }

        DeepLinkIntentCommand.execute(deepLink)

        onScreen<SearchListScreen> {
            listView.waitUntil { isCompletelyDisplayed() }
            filterButton.click()
        }

        performOnFiltersScreen {
            isBuySelected()
            isApartmentSelected()
            isSiteSelected()
            geoSuggestEquals("Город Санкт-Петербург")
        }
    }

    @Test
    fun shouldOpenVillagesInSpbSearchList() {
        val deepLink = "https://realty.yandex.ru/amp/sankt-peterburg/kupit/kottedzhnye-poselki"

        configureWebServer {
            registerDeepLink(deepLink, "deepLinkTest/deepLinkVillageListSpb.json")
            registerRegionInfoSPB()
        }

        DeepLinkIntentCommand.execute(deepLink)

        onScreen<SearchListScreen> {
            listView.waitUntil { isCompletelyDisplayed() }
            filterButton.click()
        }

        performOnFiltersScreen {
            isBuySelected()
            isHouseSelected()
            isVillageSelected()
            geoSuggestEquals("Город Санкт-Петербург")
        }
    }

    @Test
    fun shouldOpenRentApartmentOffersSearchListWithConfidenceSort() {
        val deepLink = "https://realty.yandex.ru/amp" +
            "/sankt-peterburg/snyat/kvartira/?sort=CONFIDENCE"

        configureWebServer {
            registerDeepLink(deepLink, "deepLinkTest/deepLinkRentApartmentWithSort.json")
            registerRegionInfoSPB()
            registerSearchWithSorting(Sorting.CONFIDENCE, "offerWithSiteSearchSelectedSorting.json")
        }

        DeepLinkIntentCommand.execute(deepLink)

        onScreen<SearchListScreen> {
            sortingItem
                .waitUntil { listView.contains(this) }
                .invoke {
                    labelView.isTextEquals(Sorting.CONFIDENCE.expected)
                }
        }
    }

    @Test
    fun shouldOpenOfferCard() {
        configureWebServer {
            register(
                request {
                    path("1.0/cardWithViews.json")
                },
                response {
                    assetBody("cardWithViews.json")
                }
            )
        }

        DeepLinkIntentCommand.execute("https://realty.yandex.ru/amp/offer/7777777/")

        performOnOfferCardScreen {
            waitUntil { isPriceEquals("1 531 231 \u20BD") }
        }
    }

    @Test
    fun shouldOpenSiteCard() {
        val deepLink = "https://realty.yandex.ru/amp" +
            "/sankt-peterburg/kupit/novostrojka/jk-novostrojka/"

        configureWebServer {
            registerDeepLink(deepLink, "deepLinkTest/deepLinkSiteCard.json")
            registerRegionInfoSPB()
            register(
                request {
                    path("1.0/siteWithOffersStat.json")
                },
                response {
                    assetBody("siteWithOfferStat.json")
                }
            )
        }

        DeepLinkIntentCommand.execute(deepLink)

        performOnSiteCardScreen {
            waitUntil { isSiteTitleEquals("ЖК «Имя»") }
        }
    }

    @Test
    fun shouldOpenVillageCard() {
        val deepLink = "https://realty.yandex.ru/amp" +
            "/sankt-peterburg/kupit/kottedzhnye-poselki/poselok/?id=55555"

        configureWebServer {
            registerDeepLink(deepLink, "deepLinkTest/deepLinkVillageCard.json")
            registerRegionInfoSPB()
            register(
                request {
                    path("2.0/village/55555/card")
                },
                response {
                    assetBody("villageCard.json")
                }
            )
        }

        DeepLinkIntentCommand.execute(deepLink)

        performOnVillageCardScreen {
            waitUntil { isVillageTitleEquals("Коттеджный посёлок «Name»") }
        }
    }

    @Test
    fun shouldOpenMapWithPriceSellLayer() {
        val deepLink = "https://realty.yandex.ru/amp" +
            "/sankt-peterburg/kupit/kvartira/karta/?layer=price-sell"

        configureWebServer {
            registerDeepLink(deepLink, "deepLinkTest/deepLinkPriceSellMapLayer.json")
            registerRegionInfoSPB()
        }

        DeepLinkIntentCommand.execute(deepLink)

        performOnSearchMapScreen {
            waitUntil { isMapViewShown() }
            waitUntil { isLayerPriceSellActivated() }
            checkLayerButtonViewState("DeepLinkTest/shouldOpenMapWithPriceSellLayer")
            isLayerLegendVisible()
        }
    }

    @Test
    fun shouldOpenUserScreen() {
        configureWebServer { }
        DeepLinkIntentCommand.execute("https://realty.yandex.ru/amp/management-new")
        performOnUserOffersScreen {
            waitUntil { isRootViewShown() }
        }
    }

    @Test
    fun shouldOpenFavoriteListScreen() {
        DeepLinkIntentCommand.execute("https://realty.yandex.ru/amp/favorites")
        performOnFavoritesScreen {
            waitUntil { isFavoriteOffersTabSelected() }
        }
    }

    @Test
    fun shouldOpenSubscriptionsScreen() {
        DeepLinkIntentCommand.execute("https://realty.yandex.ru/amp/subscriptions")
        performOnFavoritesScreen {
            waitUntil { isSubscriptionsTabSelected() }
        }
    }

    @Test
    fun shouldOpenSupportChatScreen() {
        configureWebServer {
            registerTechSupportChat()
        }
        authorizationRule.setUserAuthorized()
        DeepLinkIntentCommand.execute("https://realty.yandex.ru/amp/chat/techsupport")
        performOnChatMessagesScreen {
            waitUntil { isSupportChatTitleShown() }
            pressBack()
        }
        performOnCommunicationScreen {
            waitUntil { isMessagesTabSelected() }
        }
    }

    @Test
    fun shouldOpenManual() {
        DeepLinkIntentCommand.execute("https://m.realty.yandex.ru/amp/journal/")
        onScreen<WebViewScreen> {
            waitUntil {
                webView.isPageUrlEquals("https://m.realty.yandex.ru/amp/journal/?only-content=true")
            }
            shareButton.isCompletelyDisplayed()
        }
    }

    @Test
    fun shouldOpenManualArticle() {
        Intents.init()
        DeepLinkIntentCommand.execute("https://m.realty.yandex.ru/amp/journal/123/")
        onScreen<WebViewScreen> {
            waitUntil {
                webView.isPageUrlEquals(
                    "https://m.realty.yandex.ru/amp/journal/123/?only-content=true"
                )
            }
            val shareUrl = "https://m.realty.yandex.ru/amp/journal/123/"
            registerResultOkIntent(matchesShareIntent(shareUrl), null)
            shareButton.click(retryLongPress = true)
            intended(matchesShareIntent(shareUrl))
        }
        Intents.release()
    }

    @Test
    fun shouldOpenAlfabank() {
        DeepLinkIntentCommand.execute("https://m.realty.yandex.ru/amp/alfabank/")
        onScreen<WebViewScreen> {
            waitUntil {
                webView.isPageUrlEquals(
                    "https://m.realty.yandex.ru/amp/alfabank/?only-content=true"
                )
            }
            toolbarTitle.isTextEquals(R.string.alfa_mortgage_title)
        }
    }

    @Test
    fun shouldOpenConcierge() {
        DeepLinkIntentCommand.execute(
            deepLink = "https://m.realty.yandex.ru/amp/podbor-kvartiry-po-parametram/",
        )
        onScreen<ConciergeProposalScreen> {
            benefitsListView.waitUntil { isCompletelyDisplayed() }
        }
    }

    private fun DispatcherRegistry.registerDeepLink(deepLink: String, asset: String) {
        register(
            request {
                path("1.0/deeplink.json")
                body("{\"url\":\"$deepLink\"}")
            },
            response {
                assetBody(asset)
            }
        )
    }

    private fun DispatcherRegistry.registerRegionInfoSPB() {
        register(
            request {
                path("1.0/getRegionInfoV15.json")
            },
            response {
                assetBody("regionInfo417899.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSearchWithSorting(
        sorting: Sorting,
        responseFileName: String
    ) {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
                queryParam("sort", sorting.value)
            },
            response {
                assetBody(responseFileName)
            }
        )
    }

    private fun DispatcherRegistry.registerTechSupportChat() {
        register(
            request {
                path("2.0/chat/room/tech-support")
            },
            response {
                assetBody("techSupportChatCommon.json")
            }
        )
    }
}
