package com.yandex.mobile.realty.test.search

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.SimilarOffersActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.OfferCardScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SimilarOffersScreen
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
 * @author misha-kozlov on 12.07.2021
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SimilarOffersTest {

    private val activityTestRule = SimilarOffersActivityTestRule(
        offerId = OFFER_ID,
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldShowSimilarOffers() {
        configureWebServer {
            registerOfferPreview()
            registerSimilarOffers()
            registerOfferCard()
        }

        activityTestRule.launchActivity()

        onScreen<SimilarOffersScreen> {
            recentOfferView
                .waitUntil { listView.contains(this) }
                .isViewStateMatches("SimilarOffersTest/infoView")

            listView.contains(offerSnippet(SIMILAR_OFFER_ID))

            recentOfferView.view.click()
        }
        onScreen<OfferCardScreen> {
            priceInfoItem.view.waitUntil { isCompletelyDisplayed() }
        }
    }

    @Test
    fun shouldShowSimilarOffersIfOfferNotFound() {
        configureWebServer {
            registerOfferPreviewNotFound()
            registerSimilarOffers()
        }

        activityTestRule.launchActivity()

        onScreen<SimilarOffersScreen> {
            listView.waitUntil { contains(offerSnippet(SIMILAR_OFFER_ID)) }
                .doesNotContain(recentOfferView)
        }
    }

    @Test
    fun shouldShowErrorScreen() {
        activityTestRule.launchActivity()

        onScreen<SimilarOffersScreen> {
            fullscreenErrorView.waitUntil { isCompletelyDisplayed() }
            root.isViewStateMatches("SimilarOffersTest/error")
        }
    }

    @Test
    fun shouldShowEmptyScreen() {
        configureWebServer {
            registerOfferPreview()
            registerEmptySimilarOffers()
        }

        activityTestRule.launchActivity()

        onScreen<SimilarOffersScreen> {
            fullscreenEmptyView.waitUntil { isCompletelyDisplayed() }
            root.isViewStateMatches("SimilarOffersTest/empty")
        }
    }

    private fun DispatcherRegistry.registerOfferPreview() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("offerId", OFFER_ID)
            },
            response {
                assetBody("SimilarOffersTest/offerWithSiteSearch.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOfferPreviewNotFound() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("offerId", OFFER_ID)
            },
            response {
                assetBody("SimilarOffersTest/offerWithSiteSearchNotFound.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSimilarOffers() {
        register(
            request {
                path("1.0/offer/$OFFER_ID/similar")
            },
            response {
                assetBody("SimilarOffersTest/similar.json")
            }
        )
    }

    private fun DispatcherRegistry.registerEmptySimilarOffers() {
        register(
            request {
                path("1.0/offer/$OFFER_ID/similar")
            },
            response {
                setBody("""{ "response": { "offers": [] } }""")
            }
        )
    }

    private fun DispatcherRegistry.registerOfferCard() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("SimilarOffersTest/cardWithViews.json")
            }
        )
    }

    companion object {
        private const val OFFER_ID = "0"
        private const val SIMILAR_OFFER_ID = "1"
    }
}
