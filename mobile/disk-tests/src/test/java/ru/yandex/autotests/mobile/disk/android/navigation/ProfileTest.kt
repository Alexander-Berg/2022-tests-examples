package ru.yandex.autotests.mobile.disk.android.navigation

import com.carlosbecker.guice.GuiceModules
import com.carlosbecker.guice.GuiceTestRunner
import com.google.inject.Inject
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
import ru.yandex.autotests.mobile.disk.android.steps.*

@Feature("Profile")
@UserTags("profile")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class ProfileTest {
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
    lateinit var onProfile: ProfileSteps

    @Inject
    lateinit var onTrash: TrashSteps

    @Inject
    lateinit var onInvites: InvitesListSteps

    @Inject
    lateinit var onSettings: SettingsSteps

    @Inject
    lateinit var onFeedback: FeedbackFormSteps

    @Test
    @TmsLink("5543")
    @Category(Regression::class)
    fun shouldOpenProfileScreen() {
        onBasePage.openProfile()
        onBasePage.shouldBeOnProfile()
    }

    @Test
    @TmsLink("5548")
    @Category(Regression::class)
    fun shouldOpenTrashScreen() {
        onBasePage.openProfile()
        onBasePage.shouldBeOnProfile()
        onProfile.openTrash()
        onTrash.shouldBeOnTrashScreen()
        onBasePage.pressHardBack()
        onBasePage.shouldNotBeOnProfile()
        onBasePage.shouldBeOnFeed()
    }

    @Test
    @TmsLink("5549")
    @Category(Regression::class)
    fun shouldOpenInvites() {
        onBasePage.openProfile()
        onBasePage.shouldBeOnProfile()
        onProfile.openInvites()
        onInvites.shouldBeOnInvitesScreen()
        shouldBeOnProfileAfterBack()
    }

    @Test
    @TmsLink("5550")
    @Category(Regression::class)
    fun shouldOpenSettings() {
        onBasePage.openProfile()
        onBasePage.shouldBeOnProfile()
        onProfile.openSettings()
        onSettings.shouldBeOnSettings()
        shouldBeOnProfileAfterBack()
    }

    @Test
    @TmsLink("5551")
    @Category(Regression::class)
    fun shouldOpenFeedback() {
        onBasePage.openProfile()
        onBasePage.shouldBeOnProfile()
        onProfile.openFeedback()
        onFeedback.shouldSeeFeedbackForm()
        shouldBeOnProfileAfterBack()
    }

    @Test
    @TmsLink("5553")
    @Category(Regression::class)
    fun shouldCloseProfileScreenByBack() {
        onBasePage.openProfile()
        onBasePage.shouldBeOnProfile()
        onProfile.pressHardBack()
        onBasePage.shouldNotBeOnProfile()
    }

    private fun shouldBeOnProfileAfterBack() {
        onBasePage.pressHardBack()
        onBasePage.shouldBeOnProfile()
    }
}
