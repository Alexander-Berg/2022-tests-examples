package ru.auto.ara.test.login

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.auth.postLoginError
import ru.auto.ara.core.dispatchers.auth.postLoginOrRegisterPassword
import ru.auto.ara.core.dispatchers.auth.postLoginOrRegisterSuccess
import ru.auto.ara.core.dispatchers.auth.postLoginOrRegisterWrongEmail
import ru.auto.ara.core.dispatchers.auth.postLoginSuccess
import ru.auto.ara.core.dispatchers.user.getUserSuccess
import ru.auto.ara.core.dispatchers.user.postUserConfirmError
import ru.auto.ara.core.dispatchers.user.postUserConfirmSuccess
import ru.auto.ara.core.interaction.auth.LoginInteractions
import ru.auto.ara.core.interaction.auth.LoginInteractions.onLicenseAgreementText
import ru.auto.ara.core.interaction.auth.LoginInteractions.onLoginBtn
import ru.auto.ara.core.robot.auth.checkLogin
import ru.auto.ara.core.robot.auth.performLogin
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.getRandomCode
import ru.auto.ara.core.utils.getRandomEmail
import ru.auto.ara.core.utils.getRandomString
import ru.auto.ara.core.utils.getResourceString
import ru.auto.ara.core.utils.launchFragment
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.ui.activity.PhoneAuthActivity
import ru.auto.ara.ui.fragment.auth.EmailAuthFragment
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class EmailAuthTest {

    private val activityTestRule = lazyActivityScenarioRule<PhoneAuthActivity>()

    private val webServerRule = WebServerRule()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityTestRule
    )

    @Before
    fun setUp() {
        activityTestRule.launchFragment<EmailAuthFragment>()
    }

    @Test
    fun shouldShowEmailLoginScreen() {
        checkLogin {
            isInputTitle(R.string.auth_email_hint)
            isInput("")
            onLoginBtn().checkIsGone()
        }
    }

    @Test
    fun shouldSeeCustomError() {
        webServerRule.routing { postLoginOrRegisterWrongEmail() }
        waitSomething(1, TimeUnit.SECONDS)
        performLogin {
            input(getRandomEmail())
            clickOnLoginBtn()
        }.checkResult {
            onLoginBtn().checkIsDisplayed()
            isError(R.string.auth_email_error)
        }
    }

    @Test
    fun shouldShowAndHideLoginButtonOnEmailInput() {
        performLogin {
            input(getRandomEmail())
        }.checkResult {
            onLoginBtn().checkIsDisplayed()
            onLicenseAgreementText(R.id.vLicenseAgreement1).checkIsCompletelyDisplayed()
        }
        waitSomething(1, TimeUnit.SECONDS)
        performLogin {
            input("")
        }.checkResult {
            onLoginBtn().checkIsGone()
            onLicenseAgreementText(R.id.vLicenseAgreement1).checkIsGone()
        }
    }

    @Test
    fun shouldShowVerificationCodeScreen() {
        val email = getRandomEmail()
        webServerRule.routing {
            postLoginOrRegisterSuccess().watch {
                checkRequestBodyParameter("email", email)
                checkRequestBodyParameter("options.allow_client_login", true.toString())
            }
        }
        loginWithEmail(email)
        waitSomething(1, TimeUnit.SECONDS)
        checkLogin {
            isInputTitle(R.string.verification_code_email)
            interactions.onProgress().checkIsGone()
            interactions.onResendCodeButton().checkIsDisplayed()
        }
    }

    @Test
    fun shouldLoginWithVerificationCode() {
        val email = getRandomEmail()
        val code = getRandomCode()
        webServerRule.routing {
            postLoginOrRegisterSuccess()
            getUserSuccess()
            postUserConfirmSuccess().watch {
                checkRequestBodyParameter("email", email)
                checkRequestBodyParameter("code", code)
            }
        }
        loginWithEmail(email)
        waitSomething(1, TimeUnit.SECONDS)
        performLogin {
            input(code)
        }.checkResult {
            activityTestRule.isClosedAfterLogin()
        }
    }

    @Test
    fun shouldShowVerificationCodeError() {
        webServerRule.routing {
            postLoginOrRegisterSuccess()
            postUserConfirmError()
        }
        loginWithEmail(getRandomEmail())
        waitSomething(1, TimeUnit.SECONDS)
        performLogin {
            input(getRandomCode())
        }.checkResult {
            isError(R.string.wrong_confirm_code)
        }
    }

    @Test
    fun shouldRequestVerificationCodeSecondTime() {
        webServerRule.routing { postLoginOrRegisterSuccess() }
        loginWithEmail(getRandomEmail())
        waitSomething(1, TimeUnit.SECONDS)
        performLogin {
            requestVerificationCodeAgain()
        }.checkResult {
            interactions.onResendCooldown().checkIsCompletelyDisplayed()
        }
    }

    @Test
    fun shouldOpenLicenseAgreement() {
        performLogin {
            input(getRandomEmail())
            val webViewChecker = clickToLicenseAgreementLink(
                licenseAgreementId = R.id.vLicenseAgreement1,
                textToClick = "Пользовательским соглашением"
            )
            checkResult {
                webViewChecker.checkOpenLicenseAgreementLink(
                    url = "https://yandex.ru/legal/autoru_terms_of_service/",
                    title = getResourceString(R.string.terms_of_service)
                )
            }
        }
    }

    @Test
    fun shouldShowAuthByEmailScreen() {
        webServerRule.routing { postLoginOrRegisterPassword() }
        loginWithEmail(getRandomEmail())
        waitSomething(1, TimeUnit.SECONDS)
        checkLogin {
            isInputTitle(R.string.password_hint)
            LoginInteractions.onRestorePasswordBtn().checkIsCompletelyDisplayed()
            onLoginBtn().checkIsGone()
        }
    }

    @Test
    fun shouldAuthWithEmailAndPassword() {
        val email = getRandomEmail()
        val password = getRandomString()
        webServerRule.routing {
            getUserSuccess()
            postLoginSuccess().watch {
                checkRequestBodyParameter("login", email)
                checkRequestBodyParameter("password", password)
            }
            postLoginOrRegisterPassword()
        }
        loginWithEmail(email)
        waitSomething(1, TimeUnit.SECONDS)
        performLogin {
            input(password)
        }.checkResult {
            isLicenseAgreementGone()
        }
        performLogin {
            onLoginBtn().checkIsCompletelyDisplayed().performClick()
        }.checkResult {
            activityTestRule.isClosedAfterLogin()
        }
    }

    @Test
    fun shouldFailAuthWithEmailAndPassword() {
        webServerRule.routing {
            postLoginError()
            postLoginOrRegisterPassword()
        }
        val email = getRandomEmail()
        loginWithEmail(email)
        waitSomething(1, TimeUnit.SECONDS)
        performLogin {
            input(getRandomString())
            onLoginBtn().checkIsCompletelyDisplayed().performClick()
        }.checkResult {
            isError(R.string.error_password_incorrect)
            isLicenseAgreementGone()
        }
    }

    private fun loginWithEmail(email: String) {
        performLogin {
            input(email)
            clickOnLoginBtn()
        }
    }
}
