package ru.yandex.autotests.mobile.disk.android.theme

import com.carlosbecker.guice.GuiceModules
import com.carlosbecker.guice.GuiceTestRunner
import io.qameta.allure.Feature
import io.qameta.allure.TmsLink
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.BasePageSteps
import ru.yandex.autotests.mobile.disk.android.steps.SettingsSteps
import ru.yandex.autotests.mobile.disk.data.SettingsConstants
import javax.inject.Inject

@Feature("Dark theme in the app")
@UserTags("darkTheme")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class DarkThemeTests {
    companion object {
        @ClassRule
        @JvmField
        var classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Inject
    lateinit var onBasePage: BasePageSteps

    @Inject
    lateinit var onSettings: SettingsSteps
    @Test
    @TmsLink("7856")
    @Category(Regression::class)
    fun shouldChangeTheme() {
        onBasePage.openSettings()
        onSettings.switchToLightTheme()
        onSettings.shouldThemeValueMatch(SettingsConstants.THEME_LIGHT_VALUE)
        onSettings.shouldThemeValueBackgroundContainColor(SettingsConstants.LIGHT_WINDOW_BACKGROUND)
        onSettings.switchToDarkTheme()
        onSettings.shouldThemeValueMatch(SettingsConstants.THEME_DARK_VALUE)
        onSettings.shouldThemeValueBackgroundContainColor(SettingsConstants.DARK_WINDOW_BACKGROUND)
        onSettings.switchToSystemTheme()
        onSettings.shouldThemeValueMatch(SettingsConstants.THEME_SYSTEM_VALUE)
        onSettings.shouldThemeValueBackgroundContainColor(SettingsConstants.LIGHT_WINDOW_BACKGROUND)
    }
}
