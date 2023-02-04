package com.yandex.mobile.realty.test.yandexrent.inventory

import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.InventorySmsConfirmationActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.interaction.NamedIntents.intended
import com.yandex.mobile.realty.core.matchesMarketIntent
import com.yandex.mobile.realty.core.registerMarketIntent
import com.yandex.mobile.realty.core.rule.AuthorizationRule
import com.yandex.mobile.realty.core.rule.InternetRule
import com.yandex.mobile.realty.core.rule.SetupDefaultAppStateRule
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.ConfirmationDialogScreen
import com.yandex.mobile.realty.core.screen.RentSmsConfirmationScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.*
import com.yandex.mobile.realty.test.BaseTest
import com.yandex.mobile.realty.test.yandexrent.OWNER_REQUEST_ID
import com.yandex.mobile.realty.utils.jsonArrayOf
import com.yandex.mobile.realty.utils.jsonObject
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author misha-kozlov on 22.04.2022
 */
@LargeTest
class InventorySigningErrorsTest : BaseTest() {

    private val internetRule = InternetRule()
    private val authorizationRule = AuthorizationRule()
    private val activityTestRule = InventorySmsConfirmationActivityTestRule(
        ownerRequestId = OWNER_REQUEST_ID,
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        SetupDefaultAppStateRule(),
        authorizationRule,
        activityTestRule,
        internetRule,
    )

    @Test
    fun shouldShowDefaultFullscreenError() {
        configureWebServer {
            registerInventorySmsCodeRequestError()
            registerInventorySmsCodeRequest()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentSmsConfirmationScreen> {
            fullscreenErrorView
                .waitUntil { isCompletelyDisplayed() }
                .isViewStateMatches(getTestRelatedFilePath("error"))
                .invoke { retryButton.click() }

            smsCodeItem.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldShowNoInternetFullscreenError() {
        configureWebServer {
            registerInventorySmsCodeRequest()
        }

        internetRule.turnOff()
        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentSmsConfirmationScreen> {
            fullscreenErrorView
                .waitUntil { isCompletelyDisplayed() }
                .isViewStateMatches(getTestRelatedFilePath("error"))

            internetRule.turnOn()
            fullscreenErrorView.invoke {
                retryButton.click()
            }

            smsCodeItem.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldShowConflictFullscreenError() {
        configureWebServer {
            registerInventorySmsCodeRequestConflictError()
            registerInventorySmsCodeRequest()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentSmsConfirmationScreen> {
            fullscreenErrorView
                .waitUntil { isCompletelyDisplayed() }
                .isViewStateMatches(getTestRelatedFilePath("error"))
                .invoke { retryButton.click() }

            smsCodeItem.waitUntil { listView.contains(this) }
        }
    }

    @Test
    fun shouldShowTooManyRequestsError() {
        configureWebServer {
            registerInventorySmsCodeRequestTooManyRequestsError()
            registerInventorySmsCodeRequest()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentSmsConfirmationScreen> {
            tooManyRequestsItem
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("error"))
        }
    }

    @Test
    fun shouldShowSubmitValidationError() {
        val validationErrorText = "Неправильный код"

        configureWebServer {
            registerInventorySmsCodeRequest()
            registerInventorySmsCodeSubmitValidationError(validationErrorText)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentSmsConfirmationScreen> {
            smsCodeItem
                .waitUntil { listView.contains(this) }
                .invoke { inputView.typeText(SMS_CODE, closeKeyboard = false) }

            validationErrorItem(validationErrorText)
                .waitUntil { listView.contains(this) }
                .isViewStateMatches(getTestRelatedFilePath("error"))
        }
    }

    @Test
    fun shouldShowSubmitConflictError() {
        val conflictErrorText = "Вы не можете подписать эту опись"

        configureWebServer {
            registerInventorySmsCodeRequest()
            registerInventorySmsCodeSubmitConflictError(conflictErrorText)
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentSmsConfirmationScreen> {
            smsCodeItem
                .waitUntil { listView.contains(this) }
                .invoke { inputView.typeText(SMS_CODE, closeKeyboard = false) }

            waitUntil {
                toastView(conflictErrorText).isCompletelyDisplayed()
            }
        }
    }

    @Test
    fun shouldShowSubmitDefaultError() {
        configureWebServer {
            registerInventorySmsCodeRequest()
            registerInventorySmsCodeSubmitError()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentSmsConfirmationScreen> {
            smsCodeItem
                .waitUntil { listView.contains(this) }
                .invoke { inputView.typeText(SMS_CODE, closeKeyboard = false) }

            waitUntil {
                toastView(getResourceString(R.string.error_try_again)).isCompletelyDisplayed()
            }
        }
    }

    @Test
    fun shouldShowSubmitNoInternetError() {
        configureWebServer {
            registerInventorySmsCodeRequest()
            registerInventorySmsCodeSubmitError()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()

        onScreen<RentSmsConfirmationScreen> {
            smsCodeItem.waitUntil { listView.contains(this) }

            internetRule.turnOff()

            smsCodeItem.view.invoke { inputView.typeText(SMS_CODE, closeKeyboard = false) }

            waitUntil {
                toastView(getResourceString(R.string.error_network_message)).isCompletelyDisplayed()
            }
        }
    }

    @Test
    fun shouldShowSubmitConsistencyError() {
        configureWebServer {
            registerInventorySmsCodeRequest()
            registerInventorySmsCodeSubmitConsistencyError()
        }

        authorizationRule.setUserAuthorized()
        activityTestRule.launchActivity()
        registerMarketIntent()

        onScreen<RentSmsConfirmationScreen> {
            smsCodeItem
                .waitUntil { listView.contains(this) }
                .invoke { inputView.replaceText(SMS_CODE) }
        }
        onScreen<ConfirmationDialogScreen> {
            waitUntil { titleView.isCompletelyDisplayed() }

            root.isViewStateMatches("dialog/needAppUpdateDialog")

            confirmButton.click()

            intended(matchesMarketIntent())
        }
    }

    private fun DispatcherRegistry.registerInventorySmsCodeRequestError() {
        register(
            request {
                path("2.0/rent/user/me/inventory/$OWNER_REQUEST_ID/confirmation-code/request")
            },
            error()
        )
    }

    private fun DispatcherRegistry.registerInventorySmsCodeSubmitError() {
        register(
            request {
                path("2.0/rent/user/me/inventory/$OWNER_REQUEST_ID/confirmation-code/submit")
            },
            error()
        )
    }

    private fun DispatcherRegistry.registerInventorySmsCodeRequestConflictError() {
        register(
            request {
                path("2.0/rent/user/me/inventory/$OWNER_REQUEST_ID/confirmation-code/request")
            },
            response {
                setResponseCode(409)
                jsonBody {
                    "error" to jsonObject {
                        "code" to "CONFLICT"
                        "message" to "Вы не можете подписать эту опись"
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerInventorySmsCodeRequestTooManyRequestsError() {
        register(
            request {
                path("2.0/rent/user/me/inventory/$OWNER_REQUEST_ID/confirmation-code/request")
            },
            response {
                setResponseCode(429)
                jsonBody {
                    "error" to jsonObject {
                        "code" to "TOO_MANY_REQUESTS"
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerInventorySmsCodeSubmitValidationError(text: String) {
        register(
            request {
                path("2.0/rent/user/me/inventory/$OWNER_REQUEST_ID/confirmation-code/submit")
            },
            response {
                setResponseCode(400)
                jsonBody {
                    "error" to jsonObject {
                        "code" to "VALIDATION_ERROR"
                        "data" to jsonObject {
                            "validationErrors" to jsonArrayOf(
                                jsonObject {
                                    "parameter" to "/confirmSmsInfo/code"
                                    "localizedDescription" to text
                                    "code" to "INVALID_CODE"
                                }
                            )
                        }
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerInventorySmsCodeSubmitConflictError(text: String) {
        register(
            request {
                path("2.0/rent/user/me/inventory/$OWNER_REQUEST_ID/confirmation-code/submit")
            },
            response {
                setResponseCode(409)
                jsonBody {
                    "error" to jsonObject {
                        "code" to "CONFLICT"
                        "message" to text
                    }
                }
            }
        )
    }

    private fun DispatcherRegistry.registerInventorySmsCodeSubmitConsistencyError() {
        register(
            request {
                path("2.0/rent/user/me/inventory/$OWNER_REQUEST_ID/confirmation-code/submit")
            },
            response {
                setResponseCode(400)
                jsonBody {
                    "error" to jsonObject {
                        "code" to "VALIDATION_ERROR"
                        "data" to jsonObject {
                            "validationErrors" to jsonArrayOf(
                                jsonObject {
                                    "parameter" to "/confirmSmsInfo/unsupportedField"
                                    "localizedDescription" to "some text"
                                    "code" to "SOME_CODE"
                                }
                            )
                        }
                    }
                }
            }
        )
    }

    private companion object {

        const val SMS_CODE = "00000"
    }
}
