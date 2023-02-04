package com.yandex.mobile.realty.test.offer

import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.OfferCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.OfferCardScreen
import com.yandex.mobile.realty.core.screen.PriceHistoryScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.ExpectedRequest
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author shpigun on 12.05.2021
 */
class PriceHistoryTest {

    private val activityTestRule = OfferCardActivityTestRule(OFFER_ID, launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldSubscribeToPriceChanges() {
        val dispatcher = DispatcherRegistry()
        val expectedAddFavoritesRequest = dispatcher.registerFavoritesPatch()
        dispatcher.registerOfferWithPriceHistory()
        configureWebServer(dispatcher)
        activityTestRule.launchActivity()
        onScreen<OfferCardScreen> {
            priceInfoItem.waitUntil { listView.contains(this) }
            priceHistoryButton.click()
            onScreen<PriceHistoryScreen> {
                subscribeButton.waitUntil { isCompletelyDisplayed() }
                    .click()
                waitUntil { expectedAddFavoritesRequest.isOccured() }
                subscriptionLabel.waitUntil { isCompletelyDisplayed() }
                    .isViewStateMatches(
                        "PriceHistoryTest/shouldSubscribeToPriceChanges/subscriptionLabel"
                    )
                closeButton.click()
            }
            priceHistoryButton.waitUntil { isTextEquals(R.string.offer_price_history) }
                .isViewStateMatches(
                    "PriceHistoryTest/shouldSubscribeToPriceChanges/priceHistoryLabel"
                )
        }
    }

    private fun DispatcherRegistry.registerOfferWithPriceHistory() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("PriceHistoryTest/cardWithViewsWithPriceHistory.json")
            }
        )
    }

    private fun DispatcherRegistry.registerFavoritesPatch(): ExpectedRequest {
        return register(
            request {
                method("PATCH")
                path("1.0/favorites.json")
                body("{\"add\": [\"$OFFER_ID\"]}")
            },
            response {
                setBody("{\"response\": {\"relevant\": [\"$OFFER_ID\"]]}}")
            }

        )
    }

    companion object {
        private const val OFFER_ID = "0"
    }
}
