package com.yandex.mobile.realty.test.mortgageprogram

import android.content.Intent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.activity.MortgageApplicationFormActivityTestRule
import com.yandex.mobile.realty.activity.launchActivity
import com.yandex.mobile.realty.core.interaction.NamedIntents.intended
import com.yandex.mobile.realty.core.registerResultOkIntent
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.MortgageApplicationFormScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.viewMatchers.NamedIntentMatcher
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.configureWebServer
import com.yandex.mobile.realty.core.webserver.jsonBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import com.yandex.mobile.realty.core.webserver.success
import com.yandex.mobile.realty.input.createStandardProgram
import com.yandex.mobile.realty.utils.jsonObject
import okhttp3.mockwebserver.MockResponse
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * @author sorokinandrei on 8/2/21.
 */
@LargeTest
class MortgageApplicationFormTest {

    private val activityTestRule = MortgageApplicationFormActivityTestRule(
        program = createStandardProgram(PROGRAM_ID),
        launchActivity = false
    )

    @JvmField
    @Rule
    val ruleChain: RuleChain = baseChainOf(
        activityTestRule
    )

    @Test
    fun shouldShowApplicationFormAndSuccess() {
        configureWebServer {
            registerMortgageDemand(
                "formSubmitSuccess.json",
                firstName = FIRST_NAME_VALID,
                lastName = LAST_NAME_VALID,
                middleName = MIDDLE_NAME_VALID,
                email = EMAIL_VALID,
                phone = PHONE_VALID
            )
            registerMortgageCommit(code = CODE_VALID)
        }
        activityTestRule.launchActivity()
        val prefix = "MortgageApplicationFormTest/shouldShowApplicationFormAndSuccess"
        onScreen<MortgageApplicationFormScreen> {
            waitUntil { submitButton.isCompletelyDisplayed() }
            isContentStateMatches("$prefix/initialState")

            listView.scrollTo(agreementCheckboxItem).click()
            submitButton.isViewStateMatches("$prefix/submitActive")

            listView.scrollTo(lastNameInputItem)
            lastNameInputView
                .waitUntil { isCompletelyDisplayed() }
                .typeText(LAST_NAME_VALID)
            listView.scrollTo(firstNameInputItem)
            firstNameInputView
                .waitUntil { isCompletelyDisplayed() }
                .typeText(FIRST_NAME_VALID)
            listView.scrollTo(middleNameInputItem)
            middleNameInputView
                .waitUntil { isCompletelyDisplayed() }
                .typeText(MIDDLE_NAME_VALID)
            listView.scrollTo(emailInputItem)
            emailInputView
                .waitUntil { isCompletelyDisplayed() }
                .typeText(EMAIL_VALID)
            listView.scrollTo(phoneInputItem)
            phoneInputView
                .waitUntil { isCompletelyDisplayed() }
                .typeText(PHONE_VALID)

            isContentStateMatches("$prefix/formFilled")

            submitButton.click()

            waitUntil { listView.contains(codeInputItem) }
            resendSmsButton.waitUntil { isCompletelyDisplayed() }
            closeKeyboard()
            isContentStateMatches("$prefix/confirmationState")
            listView.scrollTo(codeInputItem)
            codeInputView.typeText(CODE_PARTIAL)
            proceedButton.isViewStateMatches("$prefix/proceedInactive")
            listView.scrollTo(codeInputItem)
            codeInputView.clearText()
            codeInputView.typeText(CODE_VALID, closeKeyboard = false)

            waitUntil { listView.contains(fullscreenSuccessItem) }
            isContentStateMatches("$prefix/successState")
        }
    }

    @Test
    fun shouldAutofillPhoneAndEmail() {
        configureWebServer {
            registerUserProfile()
        }
        activityTestRule.launchActivity()
        val prefix = "MortgageApplicationFormTest/shouldAutofillPhoneAndEmail"
        onScreen<MortgageApplicationFormScreen> {
            waitUntil { submitButton.isCompletelyDisplayed() }
            isContentStateMatches("$prefix/autofillProfileState")
        }
    }

    @Test
    fun shouldShowValidationErrors() {
        configureWebServer {
            registerMortgageDemandError(
                errorResponse = validationErrorResponse("PHONE_BAD_NUM_FORMAT"),
                firstName = FIRST_NAME_VALID,
                lastName = LAST_NAME_VALID,
                middleName = null,
                email = EMAIL_VALID,
                phone = PHONE_INVALID_SERVER
            )
            registerMortgageDemand(
                "formSubmitSuccess.json",
                firstName = FIRST_NAME_VALID,
                lastName = LAST_NAME_VALID,
                middleName = null,
                email = EMAIL_VALID,
                phone = PHONE_VALID
            )
            registerMortgageCommitError(
                errorResponse = validationErrorResponse("PHONE_BAD_CONFIRMATION_CODE"),
                code = CODE_INVALID
            )
            registerMortgageCommit(code = CODE_VALID)
        }
        activityTestRule.launchActivity()
        val prefix = "MortgageApplicationFormTest/shouldShowValidationErrors"
        onScreen<MortgageApplicationFormScreen> {
            waitUntil { submitButton.isCompletelyDisplayed() }

            listView.scrollTo(agreementCheckboxItem).click()
            submitButton.click()
            fieldError("Укажите фамилию").waitUntil { listView.contains(this) }
            isContentStateMatches("$prefix/emptyState")

            listView.scrollTo(lastNameInputItem)
            lastNameInputView
                .waitUntil { isCompletelyDisplayed() }
                .typeText(LAST_NAME_VALID)
            listView.scrollTo(firstNameInputItem)
            firstNameInputView
                .waitUntil { isCompletelyDisplayed() }
                .typeText(FIRST_NAME_VALID)
            listView.scrollTo(emailInputItem)
            emailInputView
                .waitUntil { isCompletelyDisplayed() }
                .typeText(EMAIL_INVALID)
            listView.scrollTo(phoneInputItem)
            phoneInputView
                .waitUntil { isCompletelyDisplayed() }
                .typeText(PHONE_INVALID)
            submitButton.click()
            fieldError("Проверьте эл. почту").waitUntil { listView.contains(this) }
            isContentStateMatches("$prefix/invalidPhoneAndEmail")

            listView.scrollTo(emailInputItem)
            emailInputView.waitUntil { isCompletelyDisplayed() }
            emailInputView.clearText()
            emailInputView.typeText(EMAIL_VALID)
            listView.scrollTo(phoneInputItem)
            phoneInputView.waitUntil { isCompletelyDisplayed() }
            phoneInputView.clearText()
            phoneInputView.typeText(PHONE_INVALID_SERVER)
            submitButton.click()
            fieldError("Проверьте номер телефона").waitUntil { listView.contains(this) }
            isContentStateMatches("$prefix/phoneInvalidServer")

            listView.scrollTo(phoneInputItem)
            phoneInputView.waitUntil { isCompletelyDisplayed() }
            phoneInputView.clearText()
            phoneInputView.typeText(PHONE_VALID)
            submitButton.click()

            waitUntil { listView.contains(codeInputItem) }
            codeInputView.clearText()
            codeInputView.typeText(CODE_INVALID, closeKeyboard = false)

            fieldError("Неверный код подтверждения").waitUntil {
                listView.contains(this)
            }
            resendSmsButton.waitUntil { isCompletelyDisplayed() }
            closeKeyboard()
            isContentStateMatches("$prefix/codeInvalidServer")

            listView.scrollTo(codeInputItem)
            codeInputView.clearText()
            codeInputView.typeText(CODE_VALID, closeKeyboard = false)
            waitUntil { listView.contains(fullscreenSuccessItem) }
        }
    }

    @Test
    fun shouldShowToastErrors() {
        configureWebServer {
            registerMortgageDemandError(
                errorResponse = response { setResponseCode(500) },
                firstName = FIRST_NAME_VALID,
                lastName = LAST_NAME_VALID,
                middleName = MIDDLE_NAME_VALID,
                email = EMAIL_VALID,
                phone = PHONE_VALID
            )
            registerMortgageDemand(
                "formSubmitSuccess.json",
                firstName = FIRST_NAME_VALID,
                lastName = LAST_NAME_VALID,
                middleName = MIDDLE_NAME_VALID,
                email = EMAIL_VALID,
                phone = PHONE_VALID
            )
            registerMortgageDemandError(
                errorResponse = response { setResponseCode(500) },
                firstName = FIRST_NAME_VALID,
                lastName = LAST_NAME_VALID,
                middleName = MIDDLE_NAME_VALID,
                email = EMAIL_VALID,
                phone = PHONE_VALID
            )
            registerMortgageCommitError(
                errorResponse = response { setResponseCode(500) },
                code = CODE_VALID
            )
            registerMortgageCommit(code = CODE_VALID)
        }
        activityTestRule.launchActivity()
        onScreen<MortgageApplicationFormScreen> {
            waitUntil { submitButton.isCompletelyDisplayed() }

            listView.scrollTo(agreementCheckboxItem).click()

            listView.scrollTo(lastNameInputItem)
            lastNameInputView
                .waitUntil { isCompletelyDisplayed() }
                .typeText(LAST_NAME_VALID)
            listView.scrollTo(firstNameInputItem)
            firstNameInputView
                .waitUntil { isCompletelyDisplayed() }
                .typeText(FIRST_NAME_VALID)
            listView.scrollTo(middleNameInputItem)
            middleNameInputView
                .waitUntil { isCompletelyDisplayed() }
                .typeText(MIDDLE_NAME_VALID)
            listView.scrollTo(emailInputItem)
            emailInputView
                .waitUntil { isCompletelyDisplayed() }
                .typeText(EMAIL_VALID)
            listView.scrollTo(phoneInputItem)
            phoneInputView
                .waitUntil { isCompletelyDisplayed() }
                .typeText(PHONE_VALID)
            submitButton.click()

            waitUntil {
                toastView(getResourceString(R.string.mortgage_application_form_submit_error))
                    .isCompletelyDisplayed()
            }

            submitButton.click()

            resendSmsButton.waitUntil { isCompletelyDisplayed() }
                .click()

            waitUntil {
                toastView(getResourceString(R.string.mortgage_application_form_submit_error))
                    .isCompletelyDisplayed()
            }

            waitUntil { listView.contains(codeInputItem) }
            codeInputView.clearText()
            codeInputView.typeText(CODE_VALID, closeKeyboard = false)

            waitUntil {
                toastView(getResourceString(R.string.mortgage_application_form_confirm_error))
                    .isCompletelyDisplayed()
            }

            proceedButton.click()

            waitUntil { listView.contains(fullscreenSuccessItem) }
        }
    }

    @Test
    fun shouldResendSms() {
        configureWebServer {
            registerMortgageDemand(
                "formSubmitSuccess.json",
                firstName = FIRST_NAME_VALID,
                lastName = LAST_NAME_VALID,
                middleName = MIDDLE_NAME_VALID,
                email = EMAIL_VALID,
                phone = PHONE_VALID
            )
            registerMortgageDemand(
                "formSubmitSuccess.json",
                firstName = FIRST_NAME_VALID,
                lastName = LAST_NAME_VALID,
                middleName = MIDDLE_NAME_VALID,
                email = EMAIL_VALID,
                phone = PHONE_VALID
            )
            registerMortgageCommit(code = CODE_VALID)
        }
        activityTestRule.launchActivity()
        val prefix = "MortgageApplicationFormTest/shouldResendSms"
        onScreen<MortgageApplicationFormScreen> {
            waitUntil { submitButton.isCompletelyDisplayed() }

            listView.scrollTo(agreementCheckboxItem).click()

            listView.scrollTo(lastNameInputItem)
            lastNameInputView
                .waitUntil { isCompletelyDisplayed() }
                .typeText(LAST_NAME_VALID)
            listView.scrollTo(firstNameInputItem)
            firstNameInputView
                .waitUntil { isCompletelyDisplayed() }
                .typeText(FIRST_NAME_VALID)
            listView.scrollTo(middleNameInputItem)
            middleNameInputView
                .waitUntil { isCompletelyDisplayed() }
                .typeText(MIDDLE_NAME_VALID)
            listView.scrollTo(emailInputItem)
            emailInputView
                .waitUntil { isCompletelyDisplayed() }
                .typeText(EMAIL_VALID)
            listView.scrollTo(phoneInputItem)
            phoneInputView
                .waitUntil { isCompletelyDisplayed() }
                .typeText(PHONE_VALID)
            submitButton.click()
            resendSmsTimerView.waitUntil {
                isTextEquals("Отправить код повторно через 0:01")
            }
            resendSmsButton.waitUntil { isCompletelyDisplayed() }
                .isViewStateMatches("$prefix/resendButton")
                .click()

            listView.scrollTo(codeInputItem)
            codeInputView.clearText()
            codeInputView.typeText(CODE_VALID, closeKeyboard = false)

            waitUntil { listView.contains(fullscreenSuccessItem) }
        }
    }

    @Test
    fun shouldOpenDisclaimerLinks() {
        activityTestRule.launchActivity()
        val yandexPolicyUrl = getResourceString(R.string.privacy_policy_url)
        val bankPolicyUrl = "https://bank.ru/policy"
        registerResultOkIntent(matchesOpenLinkIntent(yandexPolicyUrl), null)
        registerResultOkIntent(matchesOpenLinkIntent(bankPolicyUrl), null)
        onScreen<MortgageApplicationFormScreen> {
            disclaimerItem.waitUntil { listView.contains(this) }
                .tapOnLinkText(YANDEX_POLICY_TEXT)
            intended(matchesOpenLinkIntent(yandexPolicyUrl))

            disclaimerItem.waitUntil { listView.contains(this) }
                .tapOnLinkText(BANK_POLICY_TEXT)
            intended(matchesOpenLinkIntent(bankPolicyUrl))
        }
    }

    private fun DispatcherRegistry.registerUserProfile() {
        register(
            request {
                method("GET")
                path("1.0/user")
            },
            response {
                assetBody("MortgageApplicationFormTest/userProfile.json")
            }
        )
    }

    private fun DispatcherRegistry.registerMortgageDemand(
        responseFileName: String,
        firstName: String,
        lastName: String,
        middleName: String?,
        email: String,
        phone: String
    ) {
        registerMortgageDemand(
            firstName,
            lastName,
            middleName,
            email,
            phone,
            response { assetBody("MortgageApplicationFormTest/$responseFileName") }
        )
    }

    private fun DispatcherRegistry.registerMortgageDemandError(
        errorResponse: MockResponse,
        firstName: String,
        lastName: String,
        middleName: String?,
        email: String,
        phone: String
    ) {
        registerMortgageDemand(
            firstName,
            lastName,
            middleName,
            email,
            phone,
            errorResponse
        )
    }

    private fun validationErrorResponse(code: String): MockResponse {
        return response {
            setResponseCode(400)
            jsonBody {
                "error" to jsonObject {
                    "code" to code
                    "message" to "error message"
                }
            }
        }
    }

    private fun DispatcherRegistry.registerMortgageDemand(
        firstName: String,
        lastName: String,
        middleName: String?,
        email: String,
        phone: String,
        response: MockResponse
    ) {
        register(
            request {
                path("2.0/mortgage/mortgage-demand")
                jsonBody {
                    "rgid" to RGID
                    "bankId" to BANK_ID
                    "firstName" to firstName
                    "lastName" to lastName
                    middleName?.let { "middleName" to it }
                    "email" to email
                    "phone" to phone
                    "dataProcessingAgreement" to true
                }
            },
            response
        )
    }

    private fun DispatcherRegistry.registerMortgageCommit(code: String) {
        registerMortgageCommit(code, success())
    }

    private fun DispatcherRegistry.registerMortgageCommitError(
        errorResponse: MockResponse,
        code: String
    ) {
        registerMortgageCommit(code, errorResponse)
    }

    private fun DispatcherRegistry.registerMortgageCommit(
        code: String,
        response: MockResponse
    ) {
        register(
            request {
                path("2.0/mortgage/mortgage-demand/commit")
                jsonBody {
                    "id" to REQUEST_ID
                    "code" to code
                }
            },
            response
        )
    }

    private fun matchesOpenLinkIntent(url: String): Matcher<Intent> {
        return NamedIntentMatcher(
            "Открытие ссылки \"$url\"",
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(url)
            )
        )
    }

    companion object {

        private const val RGID = 587_795
        private const val BANK_ID = "10"
        private const val PROGRAM_ID = "1"
        private const val REQUEST_ID = "100"
        private const val FIRST_NAME_VALID = "Test first name"
        private const val LAST_NAME_VALID = "Test last name"
        private const val MIDDLE_NAME_VALID = "Test middle name"
        private const val EMAIL_VALID = "test@yandex.ru"
        private const val EMAIL_INVALID = "test"
        private const val PHONE_VALID = "+71111111111"
        private const val PHONE_INVALID = "+7111111"
        private const val PHONE_INVALID_SERVER = "+72222222222"

        private const val CODE_VALID = "000000"
        private const val CODE_PARTIAL = "000"
        private const val CODE_INVALID = "111111"

        private const val YANDEX_POLICY_TEXT = "Политики конфиденциальности"
        private const val BANK_POLICY_TEXT = "Политики АО \"Банк\""
    }
}
