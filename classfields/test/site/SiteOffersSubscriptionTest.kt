package com.yandex.mobile.realty.test.site

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.SiteCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.InternetRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SiteCardScreen
import com.yandex.mobile.realty.core.screen.WebViewScreen
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Created by Alena Malchikhina on 07.08.2020
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class SiteOffersSubscriptionTest {

    private var activityTestRule = SiteCardActivityTestRule(
        siteId = "1",
        launchActivity = false
    )

    private val internetRule = InternetRule()

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        internetRule
    )

    @Test
    fun shouldShowStates() {
        configureWebServer {
            registerSiteWithOffersStat()
            registerOfferStat()
            registerOfferStat()
            registerOfferSubscription(PHONE_INPUT, responseCode = 200)
        }

        activityTestRule.launchActivity()

        onScreen<SiteCardScreen> {
            appBar.collapse()
            waitUntil { listView.contains(offerSubscriptionButtonItem) }
            listView.scrollByFloatingButtonHeight()
            listView.isOfferSubscriptionFormStateMatches(
                "SiteOffersSubscriptionTest/OfferSubscriptionForm"
            )

            offerSubscriptionButtonItem.view.click()
            listView.isOfferSubscriptionFormStateMatches(
                "SiteOffersSubscriptionTest/OfferSubscriptionFormEmptyError"
            )

            offerSubscriptionInputView.typeText(PARTIAL_INPUT)
            offerSubscriptionButtonItem.view.click()
            listView.isOfferSubscriptionFormStateMatches(
                "SiteOffersSubscriptionTest/OfferSubscriptionFormPartialError"
            )

            offerSubscriptionInputView.replaceText(PHONE_INPUT)
            listView.scrollTo(offerSubscriptionButtonItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            waitUntil { listView.contains(offerSubscriptionSuccessItem) }
            listView.isOfferSubscriptionFormStateMatches(
                "SiteOffersSubscriptionTest/OfferSubscriptionFormSuccess"
            )
        }
    }

    @Test
    fun shouldShowSuccessWhenPhoneFromProfile() {
        configureWebServer {
            registerSiteWithOffersStat()
            registerOfferStat()
            registerOfferStat()
            registerOfferSubscription(PROFILE_PHONE, responseCode = 200)
            registerUserProfile()
        }

        activityTestRule.launchActivity()

        onScreen<SiteCardScreen> {
            appBar.collapse()
            waitUntil { listView.contains(offerSubscriptionButtonItem) }
            listView.scrollByFloatingButtonHeight()

            offerSubscriptionButtonItem.view.click()
            waitUntil { listView.contains(offerSubscriptionSuccessItem) }
            listView.isOfferSubscriptionFormStateMatches(
                "SiteOffersSubscriptionTest/OfferSubscriptionFormSuccessFromProfile"
            )
        }
    }

    @Test
    fun shouldShowNoInternetToast() {
        configureWebServer {
            registerSiteWithOffersStat()
            registerOfferStat()
            registerOfferStat()
        }

        activityTestRule.launchActivity()

        onScreen<SiteCardScreen> {
            appBar.collapse()
            waitUntil { listView.contains(offerSubscriptionButtonItem) }
            listView.scrollByFloatingButtonHeight()

            offerSubscriptionInputView.typeText(PHONE_INPUT)

            internetRule.turnOff()
            listView.scrollTo(offerSubscriptionButtonItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            toastView(getResourceString(R.string.error_network_message))
                .isCompletelyDisplayed()
        }
    }

    @Test
    fun shouldShowErrorToast() {
        configureWebServer {
            registerSiteWithOffersStat()
            registerOfferStat()
            registerOfferStat()
            registerOfferSubscription(PHONE_INPUT, responseCode = 400)
        }

        activityTestRule.launchActivity()

        onScreen<SiteCardScreen> {
            appBar.collapse()
            waitUntil { listView.contains(offerSubscriptionButtonItem) }
            listView.scrollByFloatingButtonHeight()

            offerSubscriptionInputView.typeText(PHONE_INPUT)

            listView.scrollTo(offerSubscriptionButtonItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            toastView(getResourceString(R.string.error_concierge_request))
                .isCompletelyDisplayed()
        }
    }

    @Test
    fun shouldShowTermsOfUse() {
        configureWebServer {
            registerSiteWithOffersStat()
            registerOfferStat()
            registerOfferStat()
        }

        activityTestRule.launchActivity()

        onScreen<SiteCardScreen> {
            appBar.collapse()
            waitUntil { listView.contains(offerSubscriptionButtonItem) }

            offerSubscriptionDisclaimerItem.waitUntil { listView.contains(this) }
                .also { listView.scrollByFloatingButtonHeight() }
                .tapOnLinkText(R.string.nb_detail_callback_disclaimer_action)
        }

        onScreen<WebViewScreen> {
            waitUntil { webView.isPageUrlEquals(TERMS_OF_USE_URL) }
        }
    }

    private fun DispatcherRegistry.registerSiteWithOffersStat() {
        register(
            request {
                path("1.0/siteWithOffersStat.json")
                queryParam("siteId", SITE_ID)
            },
            response {
                assetBody("siteWithOfferStat.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOfferStat() {
        register(
            request {
                path("2.0/site/$SITE_ID/offerStat")
            },
            response {
                assetBody("siteCardTest/offerStatNoOffers.json")
            }
        )
    }

    private fun DispatcherRegistry.registerOfferSubscription(phone: String, responseCode: Int) {
        register(
            request {
                method("POST")
                path("2.0/concierge/ticket")
                body("{\"phone\":\"$phone\",\"rgid\":0,\"sitePayload\":{\"siteId\":\"1\"}}")
            },
            response {
                setResponseCode(responseCode)
            }
        )
    }

    private fun DispatcherRegistry.registerUserProfile() {
        register(
            request {
                method("GET")
                path("1.0/user")
            },
            response {
                assetBody("callbackTest/userProfile.json")
            }
        )
    }

    companion object {
        private const val SITE_ID = "1"
        private const val PARTIAL_INPUT = "+7999222"
        private const val PHONE_INPUT = "+79992223344"
        private const val PROFILE_PHONE = "+79112223344"
        private const val TERMS_OF_USE_URL =
            "https://yandex.ru/legal/realty_termsofuse/?only-content=true"
    }
}
