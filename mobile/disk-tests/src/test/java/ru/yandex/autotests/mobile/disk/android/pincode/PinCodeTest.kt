package ru.yandex.autotests.mobile.disk.android.pincode

import com.carlosbecker.guice.GuiceModules
import com.carlosbecker.guice.GuiceTestRunner
import com.google.inject.Inject
import com.google.inject.name.Named
import io.qameta.allure.Feature
import io.qameta.allure.TmsLink
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.yandex.autotests.mobile.disk.android.core.accounts.Account
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Acceptance
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.BasePageSteps
import ru.yandex.autotests.mobile.disk.android.steps.LoginSteps
import ru.yandex.autotests.mobile.disk.android.steps.SettingsSteps
import ru.yandex.autotests.mobile.disk.data.AccountConstants
import ru.yandex.autotests.mobile.disk.data.SettingsConstants
import ru.yandex.autotests.mobile.disk.data.ToastMessages
import java.util.concurrent.TimeUnit

@Feature("PIN Code")
@UserTags("pinCode")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
@AuthorizationTest
class PinCodeTest {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    private val anotherAccount = Account("etestuser1", "etestpass1")

    @Inject
    @Named(AccountConstants.TEST_USER)
    lateinit var account: Account

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onSettings: SettingsSteps

    @Inject
    lateinit var onLogin: LoginSteps

    @Test
    @TmsLink("2485")
    @Category(BusinessLogic::class)
    @AuthorizationTest
    fun shouldSeePinAfterBackground() {
        onBasePage.openSettings()
        onSettings.setupPin(SettingsConstants.TEST_PIN)
        onSettings.shouldPinBeEnabledOnSettings()
        onSettings.closeSettings()
        onSettings.closeProfile()
        onBasePage.openFiles()
        onBasePage.sendApplicationToBackground()
        onBasePage.wait(40, TimeUnit.SECONDS)
        onBasePage.returnFromBackGroundWithActivity(SettingsConstants.ENTER_PIN_ACTIVITY)
        onSettings.shouldSeePinWindow()
        onSettings.enterPin(SettingsConstants.TEST_PIN)
        onBasePage.shouldBeOnFiles()
    }

    @Test
    @TmsLink("2024")
    @Category(Regression::class)
    fun shouldNotSeePinCodeIfDisabled() {
        onBasePage.openSettings()
        onSettings.setupPin(SettingsConstants.TEST_PIN)
        onSettings.shouldPinBeEnabledOnSettings()
        onSettings.disablePinIfEnabled()
        onSettings.runInBackground(40, TimeUnit.SECONDS)
        onSettings.shouldNotSeePinWindow()
    }

    @Test
    @TmsLink("2027")
    @Category(Regression::class)
    fun shouldOpenLoginWindowAfterClickOnForgetPin() {
        onBasePage.openSettings()
        onSettings.setupPin(SettingsConstants.TEST_PIN)
        onSettings.shouldPinBeEnabledOnSettings()
        onSettings.sendApplicationToBackground()
        onSettings.wait(40, TimeUnit.SECONDS)
        onSettings.returnFromBackGroundWithActivity(SettingsConstants.ENTER_PIN_ACTIVITY)
        onSettings.shouldSeePinWindow()
        onSettings.enterPin(SettingsConstants.INCORRECT_PIN)
        onSettings.shouldDeleteFogottenPin()
        onLogin.shouldSeeLoginPage()
    }

    @Test
    @TmsLink("2030")
    @Category(Regression::class)
    fun shouldSeePinAfterLoginToAnotherAccount() {
        setupPinAndLogout()
        onLogin.shouldSwitchToLoginFormIfAlreadyLogged()
        onLogin.shouldLoginIntoApp(anotherAccount) //use another account
        onSettings.shouldSeePinWindow()
        onSettings.enterPin(SettingsConstants.TEST_PIN)
        onLogin.closeWizards()
        onBasePage.openSettings()
        onSettings.shouldPinBeEnabledOnSettings()
    }

    @Test
    @TmsLink("2487")
    @Category(Acceptance::class) //Regression
    fun shouldViewPinAfterSetuping() {
        setupPinAndLogout()
        onLogin.shouldSwitchToLoginFormIfAlreadyLogged()
        onLogin.shouldLoginIntoApp(account) //use the same account
        onSettings.shouldSeePinWindow()
    }

    private fun setupPinAndLogout() {
        onBasePage.openSettings()
        onSettings.setupPin(SettingsConstants.TEST_PIN)
        onSettings.shouldPinBeEnabledOnSettings()
        onSettings.closeSettings()
        onSettings.logoutOnProfilePage()
    }

    @Test
    @TmsLink("2021")
    @Category(FullRegress::class)
    fun shouldNotSetupPinWhenSecondEnterNotLikeFirst() {
        onBasePage.openSettings()
        onSettings.openPinWindow()
        onSettings.shouldPinTitleBe(SettingsConstants.SELECT_PIN_TITLE)
        onSettings.enterPin(SettingsConstants.TEST_PIN)
        onSettings.shouldPinTitleBe(SettingsConstants.ENTER_PIN_AGAIN)
        onSettings.enterPin(SettingsConstants.INCORRECT_PIN)
        onSettings.shouldSeeToastWithMessage(ToastMessages.PIN_DOESNT_MATCH_TOAST)
        onSettings.shouldPinTitleBe(SettingsConstants.SELECT_PIN_TITLE)
    }

    @Test
    @TmsLink("2022")
    @Category(FullRegress::class)
    fun shouldNotSavePinAfterFirstAttempt() {
        onBasePage.openSettings()
        onSettings.openPinWindow()
        onSettings.shouldPinTitleBe(SettingsConstants.SELECT_PIN_TITLE)
        onSettings.enterPin(SettingsConstants.TEST_PIN)
        onSettings.shouldPinTitleBe(SettingsConstants.ENTER_PIN_AGAIN)
        onSettings.pressHardBack()
        onSettings.shouldNotSeePinWindow()
        onSettings.shouldPinNotBeEnabledOnSettings()
    }

    @Test
    @TmsLink("2026")
    @Category(Regression::class)
    fun shouldWrongPinMessageBeDisplayed() {
        setupPinAndLogout()
        onLogin.shouldSwitchToLoginFormIfAlreadyLogged()
        onLogin.shouldLoginIntoApp(account) //use the same account
        onSettings.shouldSeePinWindow()
        onSettings.enterPin(SettingsConstants.INCORRECT_PIN)
        onSettings.shouldWrongPinMessageBeDisplayed()
        onSettings.shouldSeeToastWithMessage("Invalid PIN; try again")
    }
}
