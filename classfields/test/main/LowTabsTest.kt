package ru.auto.ara.test.main

import androidx.annotation.StringRes
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.TestGeoRepository
import ru.auto.ara.core.dispatchers.garage.postEmptyGarageListing
import ru.auto.ara.core.robot.chat.performMessages
import ru.auto.ara.core.robot.garage.checkGarageLanding
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.robot.useroffers.performOffers
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.di.GeoSuggestRule
import ru.auto.ara.core.rules.mock.WebServerRule

@RunWith(Parameterized::class)
class LowTabsTest(private val param: TestParam) {

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule { postEmptyGarageListing() },
        GeoSuggestRule(TestGeoRepository.GeoArgs.moscow300()),
        ActivityScenarioRule(MainActivity::class.java),
    )

    @Test
    fun shouldClickLowTab() {
        performMain {
            openLowTab(param.text)
        }.checkResult {
            isLowTabSelected(param.text)
            param.check()
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "index={index}")
        fun data() = listOf(
            TestParam(R.string.search) { performMain {}.checkResult { isMainTabSelected(R.string.transport) } },
            //favorites check was moved to screenshot package screenshotTests.favorites
            TestParam(R.string.add_offer) { performOffers {}.checkResult { isEmptyUnauthorized() } },
            TestParam(R.string.messages) { performMessages {}.checkResult { isAuthError() } },
            TestParam(R.string.garage) { checkGarageLanding { isGarageLandingDisplayed() } }
        )


        data class TestParam(@StringRes val text: Int, val check: () -> Unit)
    }
}
