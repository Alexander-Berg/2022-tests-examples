package ru.yandex.autotests.mobile.disk.android.notifications

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
import ru.yandex.autotests.mobile.disk.android.core.driver.AndroidVersion
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic
import ru.yandex.autotests.mobile.disk.android.rules.IgnoreRule.ForbiddenVersions
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.*
import ru.yandex.autotests.mobile.disk.data.AccountConstants
import java.util.concurrent.TimeUnit

@Feature("Notifications")
@UserTags("notifications")
@AuthorizationTest
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
@ForbiddenVersions(AndroidVersion._5_1, AndroidVersion._6_0, AndroidVersion._7_0, AndroidVersion._7_1)
class NotificationsTest {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Inject
    @Named(AccountConstants.TEST_USER)
    lateinit var account: Account

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onSettings: SettingsSteps

    @Inject
    lateinit var onNotificationSettings: NotificationsSettingsSteps

    @Inject
    lateinit var onLogin: LoginSteps

    @Inject
    lateinit var onAdb: AdbSteps

    @Test
    @TmsLink("3571")
    @Category(BusinessLogic::class)
    fun shouldCreateNotificationChannels() {
        onBasePage.openSettings()
        onSettings.openNotificationsSettings()
        onNotificationSettings.shouldChannelsExist()
    }

    @Test
    @TmsLink("3732")
    @Category(BusinessLogic::class)
    fun shouldSaveNotificationChannelSettings() {
        onBasePage.openSettings()
        onSettings.openNotificationsSettings()
        onNotificationSettings.disableChannels()
        onNotificationSettings.wait(1, TimeUnit.SECONDS)
        onNotificationSettings.pressHardBack() //close notifications
        onSettings.openNotificationsSettings()
        onNotificationSettings.shouldChannelsBeDisabled()
    }

    @Test
    @TmsLink("4810")
    @Category(BusinessLogic::class)
    @AuthorizationTest
    fun shouldNotDuplicateCreateNotificationChannels() {
        onBasePage.openSettings()
        onSettings.openNotificationsSettings()
        onNotificationSettings.disableThreeChannels()
        onBasePage.wait(1, TimeUnit.SECONDS)
        onBasePage.pressHardBackNTimes(3)
        onBasePage.switchToAirplaneMode()
        onBasePage.logout()
        onBasePage.switchToWifi()
        onLogin.wait(1, TimeUnit.SECONDS)
        onLogin.shouldSwitchToLoginFormIfAlreadyLogged()
        onLogin.shouldLoginIntoApp(account)
        onLogin.closeWizards()
        onLogin.pressHardBack()
        onAdb.openAppNotificationSettings()
        onBasePage.wait(1, TimeUnit.SECONDS)
        onNotificationSettings.shouldNotificationChannelsBeInTheSingular()
    }
}
