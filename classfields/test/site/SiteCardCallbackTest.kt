package com.yandex.mobile.realty.test.site

import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.SiteCardActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.InternetRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SimpleInfoScreen
import com.yandex.mobile.realty.core.screen.SiteCardScreen
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.*
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.utils.jsonArrayOf
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author sorokinandrei on 4/26/21.
 */
class SiteCardCallbackTest : BaseTest() {

    private val activityTestRule = SiteCardActivityTestRule(
        siteId = SITE_ID,
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
    fun shouldShowSuccessWhenEnteredPhone() {
        val dispatcher = DispatcherRegistry()
        val expectedAnalyticsRequest = dispatcher.registerCallbackOrderEvent(
            siteId = SITE_ID,
            eventPlace = "NEWBUILDING_CARD_TOP",
            currentScreen = "NEWBUILDING_CARD",
        )
        dispatcher.registerSiteWithOfferStat()
        dispatcher.registerSiteCallback(PHONE_INPUT, responseCode = 200)
        configureWebServer(dispatcher)

        activityTestRule.launchActivity()

        onScreen<SiteCardScreen> {
            appBar.collapse()
            waitUntil { listView.contains(callbackButtonItem) }
            listView.scrollByFloatingButtonHeight()

            callbackButtonItem.view.click()
            listView.isCallbackFormStateMatches(getTestRelatedFilePath("emptyError"))

            callbackInputView.typeText(PARTIAL_INPUT)
            listView.scrollTo(callbackButtonItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            listView.isCallbackFormStateMatches(getTestRelatedFilePath("partialError"))

            callbackInputView.typeText(PARTIAL_INPUT_TAIL)
            listView.scrollTo(callbackButtonItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            waitUntil { listView.contains(callbackSuccessItem) }
            listView.isCallbackFormStateMatches(getTestRelatedFilePath("success"))
            waitUntil { expectedAnalyticsRequest.isOccured() }
        }
    }

    @Test
    fun shouldShowSuccessAlreadyInQueue() {
        configureWebServer {
            registerSiteWithOfferStat()
            registerSiteCallback(PHONE_INPUT, responseCode = 409)
        }

        activityTestRule.launchActivity()

        onScreen<SiteCardScreen> {
            appBar.collapse()
            waitUntil { listView.contains(callbackButtonItem) }
            listView.scrollByFloatingButtonHeight()

            callbackInputView.typeText(PHONE_INPUT)

            listView.scrollTo(callbackButtonItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            waitUntil { listView.contains(callbackSuccessItem) }
            listView.isCallbackFormStateMatches(getTestRelatedFilePath("successAlreadyInQueue"))
        }
    }

    @Test
    fun shouldShowSuccessWhenPhoneFromProfile() {
        val dispatcher = DispatcherRegistry()
        val expectedAnalyticsRequest = dispatcher.registerCallbackOrderEvent(
            siteId = SITE_ID,
            eventPlace = "NEWBUILDING_CARD_TOP",
            currentScreen = "NEWBUILDING_CARD",
        )
        dispatcher.registerSiteWithOfferStat()
        dispatcher.registerSiteCallback(PROFILE_PHONE, responseCode = 200)
        dispatcher.registerUserProfile()
        configureWebServer(dispatcher)

        activityTestRule.launchActivity()

        onScreen<SiteCardScreen> {
            appBar.collapse()
            waitUntil { listView.contains(callbackButtonItem) }
            listView.scrollByFloatingButtonHeight()

            callbackButtonItem.view.click()
            waitUntil { listView.contains(callbackSuccessItem) }
            listView.isCallbackFormStateMatches(getTestRelatedFilePath("successFromProfile"))
            waitUntil { expectedAnalyticsRequest.isOccured() }
        }
    }

    @Test
    fun shouldResetInputAndShowEmptyError() {
        val dispatcher = DispatcherRegistry()
        val expectedAnalyticsRequest = dispatcher.registerCallbackOrderEvent(
            siteId = SITE_ID,
            eventPlace = "NEWBUILDING_CARD_TOP",
            currentScreen = "NEWBUILDING_CARD",
        )
        dispatcher.registerSiteWithOfferStat()
        dispatcher.registerSiteCallback(PHONE_INPUT, responseCode = 200)
        dispatcher.registerUserProfile()
        configureWebServer(dispatcher)

        activityTestRule.launchActivity()

        onScreen<SiteCardScreen> {
            appBar.collapse()
            waitUntil { listView.contains(callbackButtonItem) }
            listView.scrollByFloatingButtonHeight()

            callbackInputView.textMatches(PROFILE_PHONE_FORMATTED)

            callbackInputView.clearText()
            callbackButtonItem.view.click()
            listView.isCallbackFormStateMatches(getTestRelatedFilePath("resetError"))

            callbackInputView.typeText(PHONE_INPUT)
            listView.scrollTo(callbackButtonItem)
                .also { listView.scrollByFloatingButtonHeight() }
                .click()
            waitUntil { listView.contains(callbackSuccessItem) }
            listView.isCallbackFormStateMatches(getTestRelatedFilePath("success"))
            waitUntil { expectedAnalyticsRequest.isOccured() }
        }
    }

    @Test
    fun shouldShowNoInternetToast() {
        configureWebServer {
            registerSiteWithOfferStat()
        }

        activityTestRule.launchActivity()

        onScreen<SiteCardScreen> {
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
            registerSiteWithOfferStat()
            registerSiteCallback(PHONE_INPUT, responseCode = 400)
        }

        activityTestRule.launchActivity()

        onScreen<SiteCardScreen> {
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

    private fun DispatcherRegistry.registerSiteWithOfferStat() {
        register(
            request {
                method("GET")
                path("1.0/siteWithOffersStat.json")
                queryParam("siteId", SITE_ID)
            },
            response {
                assetBody("callbackTest/siteWithOfferStatAllInfoExtended.json")
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
                                        "utmLink": "https://realty.test.vertis.yandex.ru/newbuilding/52595/"
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

    @Test
    fun shouldShowTermsOfUse() {
        configureWebServer {
            registerSiteWithOfferStat()
        }

        activityTestRule.launchActivity()

        onScreen<SiteCardScreen> {
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
        siteId: String,
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
                                    "siteInfo" to jsonObject {
                                        "siteId" to siteId
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
