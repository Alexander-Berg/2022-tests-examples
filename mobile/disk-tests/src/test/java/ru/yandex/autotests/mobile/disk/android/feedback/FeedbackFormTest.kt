package ru.yandex.autotests.mobile.disk.android.feedback

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
import org.openqa.selenium.ScreenOrientation
import ru.yandex.autotests.mobile.disk.android.core.accounts.Account
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.BusinessLogic
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.FullRegress
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Regression
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags
import ru.yandex.autotests.mobile.disk.android.steps.BasePageSteps
import ru.yandex.autotests.mobile.disk.android.steps.FeedbackFormSteps
import ru.yandex.autotests.mobile.disk.data.AccountConstants
import ru.yandex.autotests.mobile.disk.data.CommonErrorVariant
import ru.yandex.autotests.mobile.disk.data.CommonSuggestionVariant
import java.util.concurrent.TimeUnit

@Feature("Feedback form")
@UserTags("feedbackForm")
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class FeedbackFormTest {
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
    lateinit var onFeedbackForm: FeedbackFormSteps

    @Inject
    @Named(AccountConstants.TEST_USER)
    lateinit var account: Account

    @Test
    @TmsLink("5219")
    @Category(FullRegress::class)
    fun shouldSeeFeedbackFormBaseScreen() {
        onBasePage.openFeedbackForm()
        onFeedbackForm.shouldSeeFeedbackForm()
    }

    @Test
    @TmsLink("5223")
    @Category(FullRegress::class)
    fun shouldSeeCommonSuggestionVariants() {
        onBasePage.openFeedbackForm()
        onFeedbackForm.openMakeSuggestionForm()
        onFeedbackForm.shouldSeeCommonSuggestionList()
        onBasePage.rotate(ScreenOrientation.LANDSCAPE)
        onFeedbackForm.shouldSeeCommonSuggestionList()
    }

    @Test
    @TmsLink("5224")
    @Category(Regression::class)
    fun shouldSendSuggestion() {
        onBasePage.openFeedbackForm()
        onFeedbackForm.openMakeSuggestionForm()
        onFeedbackForm.applyMakeSuggestionItem(CommonSuggestionVariant.FOLDER_SIZE_INFO)
        onFeedbackForm.shouldBeOnThanksForFeedbackPage()
        onFeedbackForm.closeThanksForFeedbackPage()
    }

    @Test
    @TmsLink("5177")
    @Category(BusinessLogic::class)
    @AuthorizationTest
    fun shouldReportError() {
        onBasePage.openFeedbackForm()
        onFeedbackForm.openReportErrorForm()
        onFeedbackForm.shouldSeeUserEmail(account.login)
        onFeedbackForm.shouldClickSubject()
        onFeedbackForm.applyReportErrorItem(CommonErrorVariant.FILE_UPLOADS_OR_DOWNLOADS)
        onFeedbackForm.shouldReportErrorItemBeSelected(CommonErrorVariant.FILE_UPLOADS_OR_DOWNLOADS)
        onFeedbackForm.shouldInputMessage("Тестовая отправка ФОСа. Не нужно реагировать.")
        onFeedbackForm.shouldSendReport()
        onFeedbackForm.wait(10, TimeUnit.SECONDS)
        onFeedbackForm.shouldBeOnThanksForFeedbackErrorPage()
    }
}
