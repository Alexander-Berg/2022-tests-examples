package ru.auto.ara.test.login

import android.content.Intent
import androidx.test.espresso.action.ScrollToAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.auth.postLoginOrRegisterSuccess
import ru.auto.ara.core.dispatchers.garage.postEmptyGarageListing
import ru.auto.ara.core.dispatchers.user.postUserConfirmError
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.auth.checkLogin
import ru.auto.ara.core.robot.auth.performLogin
import ru.auto.ara.core.robot.burger.performBurger
import ru.auto.ara.core.robot.othertab.profile.checkProfile
import ru.auto.ara.core.robot.othertab.profile.performProfile
import ru.auto.ara.core.robot.performDevice
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.getResourceString
import ru.auto.ara.ui.activity.PhoneAuthActivity
import ru.auto.ara.ui.fragment.auth.PhoneAuthFragment
import ru.auto.data.util.ARG_FRAGMENT_CLASS

@RunWith(AndroidJUnit4::class)
class PhoneAuthTest {

    private val TEST_PHONE = "70000000000"
    private val CODE = "0000"

    @JvmField
    @Rule
    val activityTestRule = ActivityTestRule(
        PhoneAuthActivity::class.java,
        false,
        false
    )

    private val webServerRule = WebServerRule {
        postEmptyGarageListing()
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        SetPreferencesRule(),
        ActivityTestRule(MainActivity::class.java),
    )

    @Before
    fun setUp() {
        val startIntent = Intent().apply {
            putExtra(ARG_FRAGMENT_CLASS, PhoneAuthFragment::class.java)
        }
        activityTestRule.launchActivity(startIntent)
    }

    @After
    fun afterTest() {
        performDevice { setInternet(true) }
    }

    @Test
    fun shouldSeePhoneAuthWithSocialBlock() {
        webServerRule.routing { userSetup() }
        performLogin { waitLogin() }.checkResult {
            isCloseButtonDisplayed()
            isLogoDisplayed()
            isInputTitle(R.string.auth_phone_hint)
            isInput("+7")
            isSocialYandexButtonDisplayed()
            isGosuslugiButtonDisplayed()
            isSocialEmailButtonDisplayed()
            isSocialItemDisplayed("Вконтакте")
            isSocialItemDisplayed("Google")
            isSocialItemDisplayed("Одноклассники")
            isSocialItemDisplayed("Mail.ru")
            isSocialMosRuShortButtonDisplayed()
            isLicenseAgreementDisplayed(R.id.vLicenseAgreement)
            isKeyboardDisplayed()
        }
    }

    @Test
    //TODO remove ignore and add TestLocationAutoDetectInteractor when
    //https://github.com/YandexClassifieds/mobile-autoru-client-android/pull/3119 merge in dev
    @Ignore
    fun shouldSeeMosRuCollapseWhenLocationIsNotMoscow() {
        webServerRule.routing { userSetup() }
        performLogin { waitLogin() }.checkResult {
            isSocialMosRuShortButtonDisplayed()
        }
    }

    @Test
    fun shouldOpenLicenseAgreement() {
        webServerRule.routing { userSetup() }
        performLogin {
            waitLogin()
            val webViewChecker = clickToLicenseAgreementLink(
                licenseAgreementId = R.id.vLicenseAgreement,
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
    fun shouldTypePhone() {
        webServerRule.routing { userSetup() }
        performLogin {
            interactions.onInput().performReplaceText("7921315750")
            waitLogin()
        }.checkResult {
            isInput("+7 (921) 315-75-0")
            interactions.onLoginBtn().check(matches(not(isDisplayed())))
            isKeyboardDisplayed()
        }
    }

    @Test
    fun shouldOpenEmail() {
        webServerRule.routing { userSetup() }
        performLogin {
            interactions.onEmailBtn()
                .perform(ViewActions.actionWithAssertions(ScrollToAction()))
                .waitUntilIsCompletelyDisplayed()
            openEmailAuth()
        }.checkResult {
            isEmailAuth()
            isKeyboardDisplayed()
        }
    }

    @Test
    fun shouldFillPhoneOnRelogin() {
        webServerRule.routing {
            userSetup()
            postLoginOrRegisterSuccess()
        }
        performLogin {
            interactions.onInput().performReplaceText(TEST_PHONE)
            waitCode()
            interactions.onCodeInput().performReplaceText(CODE)
        }
        performMain { openBurgerMenu() }
        performBurger { scrollAndClickOnUserItem() }
        checkProfile { isProfileHeaderDisplayed() }
        performProfile { scrollAndClickLogout() }
        val startIntent = Intent().apply {
            putExtra(ARG_FRAGMENT_CLASS, PhoneAuthFragment::class.java)
        }
        activityTestRule.launchActivity(startIntent)
        checkLogin { isInput("+7 (000) 000-00-00") }
    }

    @Test
    fun shouldResendCode() {
        webServerRule.routing {
            userSetup()
            postLoginOrRegisterSuccess()
        }
        performLogin {
            interactions.onInput().performReplaceText(TEST_PHONE)
            waitCode()
            interactions.onResendCodeButton().performClick()
        }
        checkLogin {
            interactions.onResendCooldown().checkIsCompletelyDisplayed()
        }
    }

    @Test
    fun shouldSeeIncorrectCodeError() {
        webServerRule.routing {
            postLoginOrRegisterSuccess()
            postUserConfirmError()
        }
        performLogin {
            interactions.onInput().performReplaceText(TEST_PHONE)
            waitCode()
            interactions.onCodeInput().performReplaceText(CODE)
        }
        checkLogin {
            isError(R.string.wrong_confirm_code)
        }
    }

    @Test
    fun shouldSeeConnectionErrorOnLogin() {
        performDevice { setInternet(false) }
        performLogin { interactions.onInput().performReplaceText(TEST_PHONE) }
        checkLogin { isInternetErrorShown() }
    }

    @Test
    fun shouldSeeConnectionErrorOnConfirm() {
        webServerRule.routing {
            postLoginOrRegisterSuccess()
        }
        performLogin {
            interactions.onInput().performReplaceText(TEST_PHONE)
            waitCode()
        }
        performDevice { setInternet(false) }
        performLogin { interactions.onCodeInput().performReplaceText(CODE) }
        checkLogin { isInternetErrorShown() }
    }

}
