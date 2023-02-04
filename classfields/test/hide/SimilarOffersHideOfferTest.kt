package com.yandex.mobile.realty.test.hide

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.SimilarOffersActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.ConfirmationDialogScreen
import com.yandex.mobile.realty.core.screen.OfferMenuDialogScreen
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
class SimilarOffersHideOfferTest {

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
    fun shouldAddNote() {
        configureWebServer {
            registerOffer()
            registerSimilarOffers()
            registerHideOffer()
        }

        activityTestRule.launchActivity()

        onScreen<SimilarOffersScreen> {
            offerSnippet(SIMILAR_OFFER_ID)
                .waitUntil { listView.contains(this) }
                .invoke { menuButton.click() }
        }
        onScreen<OfferMenuDialogScreen> {
            hideButton
                .waitUntil { isCompletelyDisplayed() }
                .click()
        }
        onScreen<ConfirmationDialogScreen> {
            root.isViewStateMatches("HideOfferTest/confirmationDialog")
            confirmButton.click()
        }
        onScreen<SimilarOffersScreen> {
            offerSnippet(SIMILAR_OFFER_ID)
                .waitUntil { listView.doesNotContain(this) }
        }
    }

    private fun DispatcherRegistry.registerOffer() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("offerId", OFFER_ID)
            },
            response {
                assetBody("offerWithSiteSearchOffer.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSimilarOffers() {
        register(
            request {
                path("1.0/offer/$OFFER_ID/similar")
            },
            response {
                assetBody("hideOfferTest/similar.json")
            }
        )
    }

    private fun DispatcherRegistry.registerHideOffer() {
        register(
            request {
                path("1.0/user/me/personalization/hideOffers")
                queryParam("offerId", SIMILAR_OFFER_ID)
            },
            response {
                setBody("{}")
            }
        )
    }

    companion object {
        private const val OFFER_ID = "0"
        private const val SIMILAR_OFFER_ID = "1"
    }
}
