package ru.auto.ara.test.login

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.auth.R
import ru.auto.ara.core.dispatchers.auth.loginOrRegisterYandexSuccess
import ru.auto.ara.core.dispatchers.auth.postLoginOrRegisterPasswordChangeRequired
import ru.auto.ara.core.dispatchers.auth.postLoginOrRegisterPhoneBanned
import ru.auto.ara.core.dispatchers.auth.postLoginOrRegisterSuccess
import ru.auto.ara.core.dispatchers.auth.postLoginOrRegisterWrongEmail
import ru.auto.ara.core.dispatchers.user.postUserConfirmError
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.auth.checkAccountMerge
import ru.auto.ara.core.robot.auth.checkAccountMergeCode
import ru.auto.ara.core.robot.auth.performAccountMerge
import ru.auto.ara.core.robot.auth.performAccountMergeCode
import ru.auto.ara.core.robot.burger.checkBurger
import ru.auto.ara.core.robot.checkCommon
import ru.auto.ara.core.robot.support_report.checkSupport
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.robot.webview.checkWebView
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.getResourceString
import ru.auto.ara.core.utils.pressBack
import ru.auto.ara.core.utils.withIntents
import ru.auto.ara.router.command.auth.OpenAccountMergeCommand
import ru.auto.data.util.URL_CHANGE_PASSWORD
import ru.auto.feature.auth.account_merge.accounts.IAccountMergeProvider
import ru.auto.feature.auth.account_merge.model.UserIdentity
import ru.auto.feature.auth.account_merge.model.YandexUid

/**
 * Tests for account merge screen that can be shown after user tries to login with Yandex.Passport but multiple accounts
 * can be associated with it.
 */
@RunWith(AndroidJUnit4::class)
class AccountMergeTest {

    private val activityRule = lazyActivityScenarioRule<MainActivity>()
    private val webServerRule = WebServerRule()

    private val emailIdentity = UserIdentity.EmailIdentity("ya**************ex.ru", "1")
    private val phoneIdentity = UserIdentity.PhoneIdentity("7111******11", "2")
    private val matchedAccounts = listOf(emailIdentity, phoneIdentity)
    private val uid = YandexUid(12345678L, false)

    private val openAccountMergeCommand = OpenAccountMergeCommand(IAccountMergeProvider.Args(
        matched = matchedAccounts,
        uid = uid,
    ))

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityRule,
    )

    @Before
    fun setUp() {
        activityRule.launchActivity()
    }

    @Test
    fun shouldCloseScreenOnBackIconClick() {
        openAccountMergeScreen()

        performAccountMerge {
            waitScreenLoaded()
            clickBackIcon()
        }

        checkAccountMerge { waitScreenClosed() }
    }

    @Test
    fun shouldSuccessfullyLoginWithPhone() {
        openAccountMergeScreen()

        performAccountMerge { waitScreenLoaded() }
        webServerRule.routing {
            loginOrRegisterYandexSuccess().watch { checkRequestsCount(1) }
            postLoginOrRegisterSuccess()
            userSetup()
        }
        checkAccountMerge { compareFullScreenshots("account_merge_screen.png") }
        performAccountMerge { clickItem("+${phoneIdentity.phone}") }

        performAccountMergeCode { hideCursor() }
        checkAccountMergeCode { compareFullScreenshots("code_input_phone.png") }

        performAccountMergeCode { inputCode("1111") }

        checkBurger { isMenuDisplayed() }
    }

    @Test
    fun shouldSuccessfullyLoginWithEmail() {
        openAccountMergeScreen()

        performAccountMerge { waitScreenLoaded() }
        webServerRule.routing {
            loginOrRegisterYandexSuccess().watch { checkRequestsCount(1) }
            postLoginOrRegisterSuccess()
            userSetup()
        }
        checkAccountMerge { compareFullScreenshots("account_merge_screen.png") }
        performAccountMerge { clickItem(emailIdentity.email) }

        performAccountMergeCode { hideCursor() }
        checkAccountMergeCode { compareFullScreenshots("code_input_email.png") }
        performAccountMergeCode { inputCode("1111") }

        checkBurger { isMenuDisplayed() }
    }

    @Test
    fun shouldShowInputErrorWhenIncorrectEmail() {
        openAccountMergeScreen()

        webServerRule.routing { postLoginOrRegisterWrongEmail() }
        performAccountMerge {
            waitScreenLoaded()
            clickItemOnce(emailIdentity.email)
        }
        checkAccountMerge { isSnackDisplayed(getResourceString(R.string.account_merge_wrong_email)) }
    }

    @Test
    fun shouldShowSnackErrorWhenCodeIsIncorrect() {
        openAccountMergeScreen()

        webServerRule.routing {
            postLoginOrRegisterSuccess()
            postUserConfirmError()
        }

        performAccountMerge {
            waitScreenLoaded()
            clickItem("+${phoneIdentity.phone}")
        }

        performAccountMergeCode { hideCursor() }
        checkAccountMergeCode { compareFullScreenshots("code_input_phone.png") }

        performAccountMergeCode { inputCode("1111") }
        checkAccountMerge { isSnackDisplayed(getResourceString(R.string.account_merge_wrong_confirm_code)) }
    }

    @Test
    fun shouldOpenWebviewOnUserAgreementClick() {
        openAccountMergeScreen()

        performAccountMerge { clickOnLicenceAgreementSpan("пользовательского соглашения") }
        checkWebView { isWebViewToolBarDisplayed(text = "Пользовательское соглашение сайта «AUTO.RU»") }
        pressBack()

        performAccountMerge { clickOnLicenceAgreementSpan("пользовательским соглашением") }
        checkWebView { isWebViewToolBarDisplayed(text = "Пользовательское соглашение сайта «AUTO.RU»") }
    }

    @Test
    fun shouldReturnOnAccountMergeScreenFromCodeScreenOnBack() {
        openAccountMergeScreen()

        webServerRule.routing {
            loginOrRegisterYandexSuccess()
            userSetup()
        }
        performAccountMerge {
            waitScreenLoaded()
            clickItem("+${phoneIdentity.phone}")
        }
        performAccountMergeCode { clickBackIcon() }
        checkAccountMerge { compareFullScreenshots("account_merge_screen.png") }
    }

    @Test
    fun shouldOpenSupportChatOnPhoneBannedSnackAction() {
        openAccountMergeScreen()

        webServerRule.routing { postLoginOrRegisterPhoneBanned() }
        performAccountMerge {
            waitScreenLoaded()
            clickItemOnce("+${phoneIdentity.phone}")
            clickSnackAction()
        }

        checkSupport { isSupportReport() }
    }

    @Test
    fun shouldOpenPasswordChangeWebpageOnPasswordNeededSnackAction() {
        withIntents {
            openAccountMergeScreen()

            webServerRule.routing { postLoginOrRegisterPasswordChangeRequired() }
            performAccountMerge {
                waitScreenLoaded()
                clickItemOnce("+${phoneIdentity.phone}")
            }

            checkCommon { isBrowserIntentOpened(URL_CHANGE_PASSWORD) }
        }
    }

    private fun openAccountMergeScreen() {
        performMain { openBurgerMenu() }
        activityRule.requireScenario().onActivity { mainActivity ->
            openAccountMergeCommand.perform(mainActivity.router, mainActivity)
        }
    }
}
