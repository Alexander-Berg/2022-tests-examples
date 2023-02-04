package ru.auto.ara.test.dealer.offersfeed

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.user.userSetupDealer
import ru.auto.ara.core.robot.dealeroffers.checkDealerOffers
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule

@RunWith(AndroidJUnit4::class)
class EmptyScreenTest {

    var activityTestRule = lazyActivityScenarioRule<MainActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule { userSetupDealer() },
        activityTestRule,
        SetupAuthRule()
    )

    @Before
    fun setup() {
        activityTestRule.launchActivity()
        performMain {
            openLowTab(R.string.offers)
        }
    }

    @Test
    fun should() {
        checkDealerOffers {
            isEmptyDealer()
        }
    }
}
