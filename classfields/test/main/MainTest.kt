package ru.auto.ara.test.main

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule

@RunWith(AndroidJUnit4::class)
class MainTest {

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule(),
        ActivityScenarioRule(MainActivity::class.java)
    )

    @Test
    fun shouldLaunchApp() {
        performMain {}.checkResult {
            isMainTabSelected(R.string.transport)
            isLowTabSelected(R.string.search)
        }
    }
}
