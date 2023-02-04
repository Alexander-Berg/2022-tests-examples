package com.yandex.mobile.realty.test.offer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.OfferCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.*
import com.yandex.mobile.realty.core.screen.OfferCardScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SimpleInfoScreen
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.*
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.utils.jsonArrayOf
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * @author sorokinandrei on 4/28/21.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class OfferCardCallbackTest : BaseTest() {

    private val activityTestRule = OfferCardActivityTestRule(offerId = "0", launchActivity = false)

    private val internetRule = InternetRule()

    @JvmField
    @Rule
    var ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        activityTestRule,
        internetRule,
    )

    @Test
    fun shouldShowSuccessWhenEnteredPhone() {
        val dispatcher = DispatcherRegistry()
        val expectedAnalyticsRequest = dispatcher.registerCallbackOrderEvent(
            offerId = "0",
            eventPlace = "CARD_TOP",
            currentScreen = "OFFER_CARD",
        )
        dispatcher.registerSellNewApartment()
        dispatcher.registerSiteCallback(PHONE_INPUT, responseCode = 200)
        configureWebServer(dispatcher)

        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            appBar.collapse()
            waitUntil { listView.contains(callbackButtonItem) }
            listView.scrollByFloatingButtonHeight()

            listView.isCallbackFormStateMatches(getTestRelatedFilePath("empty"))

            callbackButtonItem.view.click()
            listView.isCallbackFormErrorStateMatches(getTestRelatedFilePath("emptyError"))

            callbackInputView.typeText(PARTIAL_INPUT)
            listView.scrollTo(callbackButtonItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            listView.isCallbackFormErrorStateMatches(getTestRelatedFilePath("partialError"))

            callbackInputView.typeText(PARTIAL_INPUT_TAIL)
            listView.scrollTo(callbackButtonItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            waitUntil { listView.contains(callbackSuccessItem) }
            listView.isCallbackSuccessStateMatches(getTestRelatedFilePath("success"))
            waitUntil { expectedAnalyticsRequest.isOccured() }
        }
    }

    @Test
    fun shouldShowSuccessAlreadyInQueue() {
        configureWebServer {
            registerSellNewApartment()
            registerSiteCallback(PHONE_INPUT, responseCode = 409)
        }

        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            appBar.collapse()
            waitUntil { listView.contains(callbackButtonItem) }
            listView.scrollByFloatingButtonHeight()

            callbackInputView.typeText(PHONE_INPUT)

            listView.scrollTo(callbackButtonItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            waitUntil { listView.contains(callbackSuccessItem) }
            listView.isCallbackSuccessStateMatches(getTestRelatedFilePath("successAlreadyInQueue"))
        }
    }

    @Test
    fun shouldShowSuccessWhenPhoneFromProfile() {
        val dispatcher = DispatcherRegistry()
        val expectedAnalyticsRequest = dispatcher.registerCallbackOrderEvent(
            offerId = "0",
            eventPlace = "CARD_TOP",
            currentScreen = "OFFER_CARD",
        )
        dispatcher.registerSellNewApartment()
        dispatcher.registerSiteCallback(PROFILE_PHONE, responseCode = 200)
        dispatcher.registerUserProfile()
        configureWebServer(dispatcher)

        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            appBar.collapse()
            waitUntil { listView.contains(callbackButtonItem) }
            listView.scrollByFloatingButtonHeight()

            callbackButtonItem.view.click()
            waitUntil { listView.contains(callbackSuccessItem) }
            listView.isCallbackSuccessStateMatches(getTestRelatedFilePath("successFromProfile"))
            waitUntil { expectedAnalyticsRequest.isOccured() }
        }
    }

    @Test
    fun shouldResetInputAndShowEmptyError() {
        val dispatcher = DispatcherRegistry()
        val expectedAnalyticsRequest = dispatcher.registerCallbackOrderEvent(
            offerId = "0",
            eventPlace = "CARD_TOP",
            currentScreen = "OFFER_CARD",
        )
        dispatcher.registerSellNewApartment()
        dispatcher.registerSiteCallback(PHONE_INPUT, responseCode = 200)
        dispatcher.registerUserProfile()
        configureWebServer(dispatcher)

        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            appBar.collapse()
            waitUntil { listView.contains(callbackButtonItem) }
            listView.scrollByFloatingButtonHeight()

            callbackInputView.textMatches(PROFILE_PHONE_FORMATTED)

            callbackInputView.clearText()
            callbackButtonItem.view.click()
            listView.isCallbackFormErrorStateMatches(getTestRelatedFilePath("resetError"))

            callbackInputView.typeText(PHONE_INPUT)
            listView.scrollTo(callbackButtonItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            waitUntil { listView.contains(callbackSuccessItem) }
            listView.isCallbackSuccessStateMatches(getTestRelatedFilePath("success"))
            waitUntil { expectedAnalyticsRequest.isOccured() }
        }
    }

    @Test
    fun shouldShowNoInternetToast() {
        configureWebServer {
            registerSellNewApartment()
        }

        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            appBar.collapse()
            waitUntil { listView.contains(callbackButtonItem) }
            listView.scrollByFloatingButtonHeight()

            callbackInputView.typeText(PHONE_INPUT)

            internetRule.turnOff()
            listView.scrollTo(callbackButtonItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            toastView(getResourceString(R.string.error_network_message))
                .isCompletelyDisplayed()
        }
    }

    @Test
    fun shouldShowErrorToast() {
        configureWebServer {
            registerSellNewApartment()
            registerSiteCallback(PHONE_INPUT, responseCode = 400)
        }

        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            appBar.collapse()
            waitUntil { listView.contains(callbackButtonItem) }
            listView.scrollByFloatingButtonHeight()

            callbackInputView.typeText(PHONE_INPUT)

            listView.scrollTo(callbackButtonItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            toastView(getResourceString(R.string.error_request_callback))
                .isCompletelyDisplayed()
        }
    }

    @Test
    fun shouldShowTermsOfUse() {
        configureWebServer {
            registerSellNewApartment()
        }

        activityTestRule.launchActivity()

        onScreen<OfferCardScreen> {
            appBar.collapse()

            callbackDisclaimerItem.waitUntil { listView.contains(this) }
                .also { listView.scrollByFloatingButtonHeight() }
                .tapOnLinkText(R.string.nb_detail_callback_disclaimer_action)
        }

        onScreen<SimpleInfoScreen> {
            dialogView.waitUntil { isCompletelyDisplayed() }
                .isViewStateMatches(getTestRelatedFilePath("infoView"))
        }
    }

    private fun DispatcherRegistry.registerSellNewApartment() {
        register(
            request {
                path("1.0/cardWithViews.json")
            },
            response {
                assetBody("callbackTest/sellNewApartmentExtended.json")
            }
        )
    }

    private fun DispatcherRegistry.registerSiteCallback(phone: String, responseCode: Int) {
        register(
            request {
                method("POST")
                path("2.0/newbuilding/callback")
                body(
                    @Suppress("MaxLineLength")
                    """{
                                "siteId": "$SITE_ID",
                                "payload": {
                                    "sourceNumber": "$phone",
                                    "trafficInfo": {
                                        "utmLink": "https://realty.yandex.ru/offer/201344023575625023/"
                                    }
                                }
                            }
                    """.trimIndent()
                )
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

    private fun DispatcherRegistry.registerCallbackOrderEvent(
        offerId: String,
        eventPlace: String,
        currentScreen: String,
    ): ExpectedRequest {
        return register(
            request {
                method("POST")
                path("1.0/event/log")
                partialBody(
                    jsonObject {
                        "events" to jsonArrayOf(
                            jsonObject {
                                "eventType" to "CALLBACK_ORDER"
                                "requestContext" to jsonObject {
                                    "eventPlace" to eventPlace
                                    "mobileReferer" to jsonObject {
                                        "currentScreen" to currentScreen
                                    }
                                }
                                "objectInfo" to jsonObject {
                                    "offerInfo" to jsonObject {
                                        "offerId" to offerId
                                    }
                                }
                            }
                        )
                    }
                )
            },
            response {
                setBody("""{"response": {}}""")
            }
        )
    }

    companion object {

        private const val SITE_ID = "0"
        private const val PARTIAL_INPUT = "+7999222"
        private const val PARTIAL_INPUT_TAIL = "3344"
        private const val PHONE_INPUT = "+79992223344"
        private const val PROFILE_PHONE = "+79112223344"
        private const val PROFILE_PHONE_FORMATTED = "+7 (911) 222-33-44"
    }
}
