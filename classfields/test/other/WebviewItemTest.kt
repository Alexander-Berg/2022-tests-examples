package ru.auto.ara.test.other

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.garage.postEmptyGarageListing
import ru.auto.ara.core.dispatchers.journal.GetJournalDispatcher
import ru.auto.ara.core.dispatchers.user.userSetupDealer
import ru.auto.ara.core.robot.othertab.performGarage
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.BlockWebViewLoadUrlRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.GARAGE_TAB_WEBVIEW_ITEMS
import ru.auto.ara.core.utils.activityScenarioRule
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.web.checkResult
import ru.auto.ara.web.watchWebView
import java.util.concurrent.TimeUnit

/*
 * If webview items will have difference for anon and auth user
 * better split this test like is already done with native items
 */

@RunWith(Parameterized::class)
class WebviewItemTest(private val param: TestParameter) {
    private val dispatchers: List<DelegateDispatcher> = listOf(
        GetJournalDispatcher("common")
    )

    private val activityRule = activityScenarioRule<MainActivity>()
    private val webServerRule = WebServerRule {
        delegateDispatchers(dispatchers)
        postEmptyGarageListing()
        userSetupDealer()
    }

    @JvmField
    @Rule
    val ruleChain = baseRuleChain(
        webServerRule,
        activityRule,
        SetupAuthRule(),
        BlockWebViewLoadUrlRule(),
        SetPreferencesRule()
    )

    @Before
    fun openOther() {
        performMain {
            openLowTab(R.string.other)
        }
    }

    @Test
    fun shouldOpenWebviewItem() {
        watchWebView {
            performGarage { clickSettingsItem(param.tabNameRes) }
            //for journal it takes some time to match webview intent (mb cause we get journal url from network)
            if (param.tabNameRes == R.string.journal) waitSomething(3, TimeUnit.SECONDS)
        }.checkResult {
            checkTitleMatches(param.tabNameRes)
            checkUrlMatches(param.uri)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "index={index}")
        fun data(): Collection<Array<out Any?>> = GARAGE_TAB_ITEM.map { arrayOf(it) }

        private val GARAGE_TAB_ITEM = GARAGE_TAB_WEBVIEW_ITEMS.map { otherTabItem ->
            TestParameter(
                tabNameRes = otherTabItem.itemNameRes,
                uri = otherTabItem.uri
            )
        }

        data class TestParameter(
            val tabNameRes: Int,
            val uri: String,
        )
    }
}
