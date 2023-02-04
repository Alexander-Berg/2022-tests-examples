package ru.auto.ara.test.other

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.di.module.TestMainModuleArguments
import ru.auto.ara.core.dispatchers.garage.postEmptyGarageListing
import ru.auto.ara.core.dispatchers.offer_card.TinkoffDispatcher
import ru.auto.ara.core.dispatchers.reviews.GetReviewsListingDispatcher
import ru.auto.ara.core.dispatchers.user.userSetupDealer
import ru.auto.ara.core.experimentsOf
import ru.auto.ara.core.robot.othertab.performGarage
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.GrantPermissionsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.AUTH_GARAGE_TAB_NATIVE_ITEMS
import ru.auto.ara.core.utils.activityScenarioRule
import ru.auto.experiments.Experiments
import ru.auto.experiments.loanBrokerEnabled

@RunWith(Parameterized::class)
class AuthNativeItemTest(private val param: TestParameter) {

    private val activityRule = activityScenarioRule<MainActivity>()
    private val composeRule = createEmptyComposeRule()

    private val webServerRule = WebServerRule {
        delegateDispatchers(
            TinkoffDispatcher("empty"),
            GetReviewsListingDispatcher("cars")
        )
        postEmptyGarageListing()
        userSetupDealer()
    }

    @JvmField
    @Rule
    val ruleChain = baseRuleChain(
        webServerRule,
        GrantPermissionsRule(),
        activityRule,
        composeRule,
        SetupAuthRule(),
        SetPreferencesRule(),
        arguments = TestMainModuleArguments(
            testExperiments = experimentsOf {
                Experiments::loanBrokerEnabled then false
            }
        )
    )

    @Before
    fun openOther() {
        performMain {
            openLowTab(R.string.other)
        }
    }

    @Test
    fun shouldOpenSettingsItem() {
        performGarage {
            clickSettingsItem(param.tabNameRes)
        }.checkResult {
            param.check(composeRule)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "index={index}")
        fun data(): Collection<Array<out Any?>> = GARAGE_TAB_ITEM.map { arrayOf(it) }

        private val GARAGE_TAB_ITEM = AUTH_GARAGE_TAB_NATIVE_ITEMS.map { settingsItem ->
            TestParameter(
                tabNameRes = settingsItem.itemNameRes,
                check = settingsItem.check
            )
        }

        data class TestParameter(
            val tabNameRes: Int,
            val check: (composeTestRule: ComposeTestRule) -> Unit,
        )
    }
}
