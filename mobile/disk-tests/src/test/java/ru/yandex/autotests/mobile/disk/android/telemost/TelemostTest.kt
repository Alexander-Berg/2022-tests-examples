package ru.yandex.autotests.mobile.disk.android.telemost

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
import ru.yandex.autotests.mobile.disk.android.DiskTest
import ru.yandex.autotests.mobile.disk.android.StepsLocator
import ru.yandex.autotests.mobile.disk.android.core.factory.RulesFactory.createClassTestRules
import ru.yandex.autotests.mobile.disk.android.core.module.AndroidModule
import ru.yandex.autotests.mobile.disk.android.infrastructure.categories.priority.Acceptance
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.AuthorizationTest
import ru.yandex.autotests.mobile.disk.android.rules.annotations.test.UserTags

@Feature("Telemost")
@UserTags("diskUI")
@AuthorizationTest
@RunWith(GuiceTestRunner::class)
@GuiceModules(AndroidModule::class)
class TelemostTest : DiskTest {

    companion object {
        @ClassRule
        @JvmField
        val classRuleChain = createClassTestRules()
    }

    @Rule
    @Inject
    lateinit var ruleChain: RuleChain

    @Inject
    override lateinit var locator: StepsLocator

    @Test
    @TmsLink("7689")
    @Category(Acceptance::class) //BusinessLogic
    fun `should normally start conference`() {
        onNavigationPage {
            openTelemost {
                startConference()
                shouldSeeCopyLinkNotification()
                exitConference()
            }
        }
    }
}
