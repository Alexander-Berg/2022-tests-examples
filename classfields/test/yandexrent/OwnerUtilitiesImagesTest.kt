package com.yandex.mobile.realty.test.yandexrent

import com.google.gson.JsonObject
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.RentFlatActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.InternetRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.GalleryScreen
import com.yandex.mobile.realty.core.screen.OwnerUtilitiesImagesScreen
import com.yandex.mobile.realty.core.screen.RentFlatScreen
import com.yandex.mobile.realty.core.screen.RentUtilitiesDeclineScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.*
import com.yandex.mobile.realty.test.services.FLAT_ID
import com.yandex.mobile.realty.test.services.registerOwnerRentFlat
import com.yandex.mobile.realty.utils.jsonArrayOf
import com.yandex.mobile.realty.utils.jsonObject
import com.yandex.mobile.realty.utils.toJsonArray
import org.junit.Rule
import org.junit.rules.RuleChain

/**
 * @author misha-kozlov on 06.02.2022
 */
open class OwnerUtilitiesImagesTest : RentUtilitiesTest() {

    private val authorizationRule = AuthorizationRule()
    private val internetRule = InternetRule()
    private val activityTestRule = RentFlatActivityTestRule(
        flatId = FLAT_ID,
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        internetRule,
        activityTestRule
    )

    protected fun shouldShowImages(
        notificationKey: String,
        notificationTitle: String,
        registerPeriod: DispatcherRegistry.() -> Unit
    ) {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    notificationKey to jsonObject {
                        "periodId" to PERIOD_ID
                        "period" to PERIOD
                    }
                }
            )
            registerPeriod()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            notificationItem(notificationTitle)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("notification"))
                .invoke { actionButton.click() }
        }
        onScreen<OwnerUtilitiesImagesScreen> {
            waitUntil { toolbarTitleView.isTextEquals(TITLE) }

            root.isViewStateMatches(getTestRelatedFilePath("images"))

            listView.scrollTo(imagesItem(IMAGE_URL)).click()
        }
        onScreen<GalleryScreen> {
            waitUntil { photoView.isCompletelyDisplayed() }
        }
    }

    protected fun shouldDeclineImages(
        notificationKey: String,
        notificationTitle: String,
        registerPeriod: DispatcherRegistry.() -> Unit,
        declineRequest: RequestMatcher
    ) {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    notificationKey to jsonObject {
                        "periodId" to PERIOD_ID
                        "period" to PERIOD
                    }
                }
            )
            registerPeriod()
            register(declineRequest, error())
            register(declineRequest, success())
            registerOwnerRentFlat()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            notificationItem(notificationTitle)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }
        onScreen<OwnerUtilitiesImagesScreen> {
            waitUntil { toolbarTitleView.isTextEquals(TITLE) }

            root.isViewStateMatches(getTestRelatedFilePath("screen"))

            listView.scrollTo(declineButton).click()
        }
        onScreen<RentUtilitiesDeclineScreen> {
            waitUntil { messageView.isCompletelyDisplayed() }
            isViewStateMatches(getTestRelatedFilePath("empty"))

            messageView.typeText(DECLINE_REASON)
            isViewStateMatches(getTestRelatedFilePath("filled"))

            declineButton.click()
            toastView(getResourceString(R.string.error_try_again)).isCompletelyDisplayed()

            declineButton.click()

            waitUntil { successView.isCompletelyDisplayed() }
            isViewStateMatches(getTestRelatedFilePath("success"))
            successButton.click()
        }
        onScreen<RentFlatScreen> {
            waitUntil {
                listView.doesNotContain(notificationItem(notificationTitle))
            }
        }
    }

    protected fun shouldShowNetworkErrorToast(
        notificationKey: String,
        notificationTitle: String,
        registerPeriod: DispatcherRegistry.() -> Unit
    ) {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    notificationKey to jsonObject {
                        "periodId" to PERIOD_ID
                        "period" to PERIOD
                    }
                }
            )
            registerPeriod()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            notificationItem(notificationTitle)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }
        onScreen<OwnerUtilitiesImagesScreen> {
            waitUntil { toolbarTitleView.isTextEquals(TITLE) }
            listView.scrollTo(declineButton).click()
        }
        onScreen<RentUtilitiesDeclineScreen> {
            messageView
                .waitUntil { isCompletelyDisplayed() }
                .typeText(DECLINE_REASON)

            internetRule.turnOff()

            declineButton.click()
            toastView(getResourceString(R.string.error_network_message)).isCompletelyDisplayed()
        }
    }

    protected fun shouldShowConflictErrorToast(
        notificationKey: String,
        notificationTitle: String,
        registerPeriod: DispatcherRegistry.() -> Unit,
        declineRequest: RequestMatcher
    ) {
        configureWebServer {
            registerOwnerRentFlat(
                notification = jsonObject {
                    notificationKey to jsonObject {
                        "periodId" to PERIOD_ID
                        "period" to PERIOD
                    }
                }
            )
            registerPeriod()
            register(declineRequest, conflictError())
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentFlatScreen> {
            notificationItem(notificationTitle)
                .waitUntil { listView.contains(this) }
                .invoke { actionButton.click() }
        }
        onScreen<OwnerUtilitiesImagesScreen> {
            waitUntil { toolbarTitleView.isTextEquals(TITLE) }
            listView.scrollTo(declineButton).click()
        }
        onScreen<RentUtilitiesDeclineScreen> {
            messageView
                .waitUntil { isCompletelyDisplayed() }
                .typeText(DECLINE_REASON)

            declineButton.click()
            toastView(ERROR_MESSAGE).isCompletelyDisplayed()
        }
    }

    protected fun getImages(count: Int): JsonObject {
        return jsonObject {
            "photos" to List(count) { index ->
                jsonObject {
                    "namespace" to NAMESPACE
                    "groupId" to GROUP_ID
                    "name" to "$index"
                    "imageUrls" to jsonArrayOf(
                        jsonObject {
                            "alias" to "1024x1024"
                            "url" to IMAGE_URL
                        },
                        jsonObject {
                            "alias" to "orig"
                            "url" to IMAGE_URL
                        }
                    )
                }
            }.toJsonArray()
        }
    }

    companion object {

        const val DECLINE_REASON = "some text"
    }
}
