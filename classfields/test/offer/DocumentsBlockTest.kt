package com.yandex.mobile.realty.test.offer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.OfferCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.OfferCardScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
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
 * @author pvl-zolotov on 17.03.2022
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class DocumentsBlockTest : BaseTest() {

    private val activityTestRule = OfferCardActivityTestRule(offerId = "0", launchActivity = false)

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule
    )

    @Test
    fun shouldShowDocumentsSellSecondaryApartmentAndClickOnShowDocumentsButton() {
        configureWebServer {
            registerSellSecondaryApartmentOffer()
        }

        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            appBar.collapse()
            waitUntil { listView.contains(documentsItem) }
            listView.scrollByFloatingButtonHeight()
            isDocumentsItemMatches(getTestRelatedFilePath("documents"))
            documentsButtonItem.click()
        }
        onScreen<WebViewScreen> {
            webView.waitUntil {
                isPageUrlEquals("https://m.realty.yandex.ru/dokumenty/?only-content=true")
            }
            shareButton.isCompletelyDisplayed()
        }
    }

    private fun DispatcherRegistry.registerSellSecondaryApartmentOffer() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("documentsOfferTest/sellSecondaryApartmentOfferDetails.json")
            }
        )
    }
}
