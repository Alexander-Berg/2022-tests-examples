package com.yandex.mobile.realty.test.deeplink

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.core.DeepLinkIntentCommand
import com.yandex.mobile.realty.core.interaction.NamedIntents.intended
import com.yandex.mobile.realty.core.matchesMarketIntent
import com.yandex.mobile.realty.core.matchesShareIntent
import com.yandex.mobile.realty.core.registerMarketIntent
import com.yandex.mobile.realty.core.registerResultOkIntent
import com.yandex.mobile.realty.core.robot.performOnChatMessagesScreen
import com.yandex.mobile.realty.core.robot.performOnCommunicationScreen
import com.yandex.mobile.realty.core.robot.performOnFavoritesScreen
import com.yandex.mobile.realty.core.robot.performOnSearchMapScreen
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.IntentsRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.ConciergeProposalScreen
import com.yandex.mobile.realty.core.screen.DeepLinkParsingScreen
import com.yandex.mobile.realty.core.screen.FavoriteScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.ServicesScreen
import com.yandex.mobile.realty.core.screen.SharedFavoritesScreen
import com.yandex.mobile.realty.core.screen.SimilarOffersScreen
import com.yandex.mobile.realty.core.screen.UserOffersScreen
import com.yandex.mobile.realty.core.screen.WebViewScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.test.BaseTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author rogovalex on 24/04/2019.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class DeepLinkTest : BaseTest() {

    private val authorizationRule = AuthorizationRule()

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        IntentsRule()
    )

    @Test
    fun shouldRequestUpdate() {
        val deepLink = "yandexrealty://unknown"

        DeepLinkIntentCommand.execute(deepLink)
        registerMarketIntent()

        onScreen<DeepLinkParsingScreen> {
            waitUntil { titleView.isCompletelyDisplayed() }
            root.isViewStateMatches(getTestRelatedFilePath("screen"))

            updateButton.click()

            intended(matchesMarketIntent())
        }
    }

    @Test
    fun shouldOpenServices() {
        val deepLink = "yandexrealty://services"

        DeepLinkIntentCommand.execute(deepLink)

        onScreen<ServicesScreen> {
            rentPromoItem.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldOpenSharedFavorites() {
        val deepLink = "https://realty.yandex.ru/shared-favorites/G.R2MwZJHWR.ogDT2/"

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
        val deepLink = "https://realty.yandex.ru/offer/$offerId/similar"

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
    fun shouldOpenMapWithPriceSellLayer() {
        val deepLink = "https://realty.yandex.ru" +
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
        DeepLinkIntentCommand.execute("https://realty.yandex.ru/management")
        onScreen<UserOffersScreen> {
            contentView.isCompletelyDisplayed()
        }
    }

    @Test
    fun shouldOpenNewUserScreen() {
        DeepLinkIntentCommand.execute("https://realty.yandex.ru/management-new")
        onScreen<UserOffersScreen> {
            contentView.isCompletelyDisplayed()
        }
    }

    @Test
    fun shouldOpenFavoriteListScreen() {
        DeepLinkIntentCommand.execute("https://realty.yandex.ru/favorites")
        performOnFavoritesScreen {
            waitUntil { isFavoriteOffersTabSelected() }
        }
    }

    @Test
    fun shouldOpenSubscriptionsScreen() {
        DeepLinkIntentCommand.execute("https://realty.yandex.ru/subscriptions")
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
        DeepLinkIntentCommand.execute("https://realty.yandex.ru/chat/techsupport")
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
        DeepLinkIntentCommand.execute("yandexrealty://realty.yandex.ru/journal/")
        onScreen<WebViewScreen> {
            waitUntil {
                webView.isPageUrlEquals("https://realty.yandex.ru/journal/?only-content=true")
            }
            shareButton.isCompletelyDisplayed()
        }
    }

    @Test
    fun shouldOpenManualArticle() {
        DeepLinkIntentCommand.execute("yandexrealty://realty.yandex.ru/journal/123/")
        onScreen<WebViewScreen> {
            waitUntil {
                webView.isPageUrlEquals("https://realty.yandex.ru/journal/123/?only-content=true")
            }
            val shareUrl = "https://realty.yandex.ru/journal/123/"
            registerResultOkIntent(matchesShareIntent(shareUrl), null)
            shareButton.click(retryLongPress = true)
            intended(matchesShareIntent(shareUrl))
        }
    }

    @Test
    fun shouldOpenAlfabank() {
        DeepLinkIntentCommand.execute("yandexrealty://m.realty.yandex.ru/alfabank/")
        onScreen<WebViewScreen> {
            waitUntil {
                webView.isPageUrlEquals("https://m.realty.yandex.ru/alfabank/?only-content=true")
            }
            toolbarTitle.isTextEquals(R.string.alfa_mortgage_title)
        }
    }

    @Test
    fun shouldOpenConcierge() {
        DeepLinkIntentCommand.execute(
            deepLink = "yandexrealty://m.realty.yandex.ru/podbor-kvartiry-po-parametram/",
        )
        onScreen<ConciergeProposalScreen> {
            benefitsListView.waitUntil { isCompletelyDisplayed() }
        }
    }

    @Test
    fun shouldOpenRentOwnerLanding() {
        DeepLinkIntentCommand.execute(deepLink = "yandexrealty://arenda/owner-landing")
        val landingUrl = "https://arenda.test.vertis.yandex.ru/app/owner/?only-content=true"
        onScreen<WebViewScreen> {
            waitUntil { webView.isPageUrlEquals(landingUrl) }
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
