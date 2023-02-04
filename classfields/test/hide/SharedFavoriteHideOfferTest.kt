package com.yandex.mobile.realty.test.hide

import com.yandex.mobile.realty.activity.SharedFavoritesActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.ConfirmationDialogScreen
import com.yandex.mobile.realty.core.screen.OfferMenuDialogScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SharedFavoritesScreen
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.domain.model.ModelType
import org.junit.Rule
import org.junit.Test

/**
 * @author merionkov on 02.11.2021.
 */
class SharedFavoriteHideOfferTest {

    private val activityTestRule = SharedFavoritesActivityTestRule(
        modelType = ModelType.OFFER,
        objectIds = listOf(PAGE_ID),
        launchActivity = false,
    )

    @JvmField
    @Rule
    val ruleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
    )

    @Test
    fun shouldHideOffer() {

        configureWebServer {
            registerOffers()
            registerHideOffer()
        }

        activityTestRule.launchActivity()

        onScreen<SharedFavoritesScreen> {
            offerSnippet(VISIBLE_OFFER_ID).waitUntil { listView.contains(this) }
            offerSnippet(HIDDEN_OFFER_ID).waitUntil { listView.contains(this) }
            offerSnippet(HIDDEN_OFFER_ID).view.menuButton.click()
        }
        onScreen<OfferMenuDialogScreen> {
            hideButton.waitUntil { isCompletelyDisplayed() }.click()
        }
        onScreen<ConfirmationDialogScreen> {
            confirmButton.waitUntil { isCompletelyDisplayed() }.click()
        }
        onScreen<SharedFavoritesScreen> {
            offerSnippet(VISIBLE_OFFER_ID).waitUntil { listView.contains(this) }
            offerSnippet(HIDDEN_OFFER_ID).run { listView.doesNotContain(this) }
        }
    }

    private fun DispatcherRegistry.registerOffers() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("offerId", PAGE_ID)
            },
            response {
                assetBody("hideOfferTest/sharedFavoriteOffers.json")
            },
        )
    }

    private fun DispatcherRegistry.registerHideOffer() {
        register(
            request {
                path("1.0/user/me/personalization/hideOffers")
                queryParam("offerId", HIDDEN_OFFER_ID)
            },
            response {
                setBody("{}")
            }
        )
    }

    private companion object {

        const val PAGE_ID = "0"
        const val HIDDEN_OFFER_ID = "0"
        const val VISIBLE_OFFER_ID = "1"
    }
}
