package ru.yandex.autotests.mobile.disk.android.onboardings

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
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.RequireOnboarding
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.BasePageSteps
import ru.yandex.autotests.mobile.disk.android.steps.CommonsSteps
import ru.yandex.autotests.mobile.disk.android.steps.LoginSteps
import ru.yandex.autotests.mobile.disk.android.steps.SettingsSteps
import ru.yandex.autotests.mobile.disk.data.AccountConstants

@Feature("Onboardings")
@UserTags("onboarding")
@AuthorizationTest
@RequireOnboarding
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class OnboardingTest {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Inject
    lateinit var commonsSteps: CommonsSteps

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onSettings: SettingsSteps

    @Inject
    lateinit var onLogin: LoginSteps

    @Inject
    @Named(AccountConstants.TEST_USER)
    lateinit var account: Account

    @Test
    @TmsLink("6359")
    @Category(BusinessLogic::class)
    fun shouldEnableAutouploadViaUnlimOnboarding() {
        onBasePage.openSettings()
        onSettings.openAutouploadSettings()
        onSettings.disablePhotoAutoupload()
        onSettings.disableAutouploadOverMobileNetwork()
        onSettings.pressHardBackNTimes(2)
        onSettings.logoutOnProfilePage()
        commonsSteps.shouldReinstallApp()
        onLogin.shouldLoginIntoApp(account)
        onLogin.shouldDisplayPhotonUnlimWizard()
    }
}
