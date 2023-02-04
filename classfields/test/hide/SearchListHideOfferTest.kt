package com.yandex.mobile.realty.test.hide

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.MainActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.robot.performOnConfirmationDialog
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.OfferMenuDialogScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SearchListScreen
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
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
 * @author andrikeev on 19/01/2021.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SearchListHideOfferTest {

    private val activityTestRule = MainActivityTestRule(launchActivity = false)

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(listMode = true),
        activityTestRule,
    )

    @Test
    fun shouldHideOfferWhenHideMenuButtonPressedAndConfirmed() {
        configureWebServer {
            registerOffers()
            registerHideOffer()
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .invoke {
                    menuButton.click()
                }
        }
        onScreen<OfferMenuDialogScreen> {
            hideButton.isCompletelyDisplayed()
            hideButton.click()
        }
        performOnConfirmationDialog {
            isTitleEquals(getResourceString(R.string.hide_offer_confirmation_title))
            isMessageEquals(getResourceString(R.string.hide_offer_confirmation))
            confirm()
        }
        onScreen<SearchListScreen> {
            offerSnippet(OFFER_ID).waitUntil { listView.doesNotContain(this) }
        }
    }

    @Test
    fun shouldNotHideOfferWhenHideMenuButtonPressedAndNotConfirmed() {
        configureWebServer {
            registerOffers()
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .invoke {
                    menuButton.click()
                }
        }
        onScreen<OfferMenuDialogScreen> {
            hideButton.isCompletelyDisplayed()
            hideButton.click()
        }
        performOnConfirmationDialog {
            isTitleEquals(getResourceString(R.string.hide_offer_confirmation_title))
            isMessageEquals(getResourceString(R.string.hide_offer_confirmation))
            cancel()
        }
        onScreen<OfferMenuDialogScreen> {
            pressBack()
        }
        onScreen<SearchListScreen> {
            offerSnippet(OFFER_ID).waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldShowErrorWhenHideMenuButtonPressedAndFailed() {
        configureWebServer {
            registerOffers()
        }

        activityTestRule.launchActivity()

        onScreen<SearchListScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .invoke {
                    menuButton.click()
                }
        }
        onScreen<OfferMenuDialogScreen> {
            hideButton.isCompletelyDisplayed()
            hideButton.click()
        }
        performOnConfirmationDialog {
            isTitleEquals(getResourceString(R.string.hide_offer_confirmation_title))
            isMessageEquals(getResourceString(R.string.hide_offer_confirmation))
            confirm()
        }
        onScreen<SearchListScreen> {
            toastView(getResourceString(R.string.hide_offer_failed))
                .waitUntil { isCompletelyDisplayed() }
        }
    }

    private fun DispatcherRegistry.registerOffers() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
                excludeQueryParamKey("excludeRightLongitude")
            },
            response {
                assetBody("hideOfferTest/offerWithSiteSearch.json")
            }
        )
    }

    private fun DispatcherRegistry.registerHideOffer() {
        register(
            request {
                path("1.0/user/me/personalization/hideOffers")
                queryParam("offerId", OFFER_ID)
            },
            response {
                setBody("{}")
            }
        )
    }

    companion object {

        private const val OFFER_ID = "1"
    }
}
