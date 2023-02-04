package com.yandex.mobile.realty.test.hide

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.MortgageProgramListActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.ConfirmationDialogScreen
import com.yandex.mobile.realty.core.screen.MortgageProgramListScreen
import com.yandex.mobile.realty.core.screen.OfferMenuDialogScreen
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

/**
 * @author sorokinandrei on 3/26/21.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class MortgageSearchListHideOfferTest {

    private val activityTestRule = MortgageProgramListActivityTestRule(
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
    )

    @Test
    fun shouldHideOfferWhenHideMenuButtonPressedAndConfirmed() {
        configureWebServer {
            registerOffersWithSiteSearch()
            registerHideOffer()
            registerMortgageProgramSearch("mortgageProgramSearchDefault.json", rgid = null)
            registerMortgageProgramSearch(
                "mortgageProgramSearchDefault.json",
                rgid = RGID
            )
        }

        activityTestRule.launchActivity()

        onScreen<MortgageProgramListScreen> {
            offerSnippet(OFFER_ID)
                .waitUntil { listView.contains(this) }
                .menuButton
                .click()
            onScreen<OfferMenuDialogScreen> {
                hideButton.waitUntil { isCompletelyDisplayed() }
                    .click()
            }
            onScreen<ConfirmationDialogScreen> {
                root.isViewStateMatches("HideOfferTest/confirmationDialog")
                confirmButton.click()
            }
            waitUntil { listView.doesNotContain(offerSnippet(OFFER_ID)) }
        }
    }

    private fun DispatcherRegistry.registerOffersWithSiteSearch() {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("page", "0")
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

    private fun DispatcherRegistry.registerMortgageProgramSearch(
        responseFileName: String,
        page: Int = 0,
        rgid: String?,
    ) {
        register(
            request {
                path("2.0/mortgage/program/search")
                rgid?.let { queryParam("rgid", it) }
                queryParam("page", page.toString())
            },
            response {
                assetBody(responseFileName)
            }
        )
    }

    companion object {
        private const val OFFER_ID = "1"
        private const val RGID = "587795"
    }
}
