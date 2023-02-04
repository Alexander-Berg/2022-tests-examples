package ru.auto.ara.test.evaluate

import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.draft.createNewDraft
import ru.auto.ara.core.dispatchers.draft.putDraft
import ru.auto.ara.core.robot.othertab.performEvaluate
import ru.auto.ara.core.rules.GrantPermissionsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchEvaluateFragment
import ru.auto.ara.ui.activity.SimpleSecondLevelActivity


@RunWith(AndroidJUnit4::class)
class EvaluateTest {

    private val activityTestRule = lazyActivityScenarioRule<SimpleSecondLevelActivity>()

    private val webServerRule = WebServerRule {
        createNewDraft(fileName = "1859688558076192892-f26b83df")
        putDraft("1859688558076192892-f26b83df")
    }

    @JvmField
    @Rule
    val ruleChain = baseRuleChain(
        webServerRule,
        GrantPermissionsRule(),
        activityTestRule,
    )

    @Before
    fun before() {
        activityTestRule.launchEvaluateFragment()
        performEvaluate().waitEvaluate()
    }

    @Test
    fun shouldClearAll() {
        val enabledFields = listOf(R.string.wiz_location_label, R.string.marks)
        val notEnabledFields =
            listOf(R.string.step_model_title, R.string.step_year_title, R.string.step_generation_title, R.string.body)
        performEvaluate { clearAll() }.checkResult {
            enabledFields.forEach { f ->
                interactions.onContainer(f).waitUntil(isEnabled(), isDisplayed())
                interactions.onFieldValue(f).waitUntilWithText("")
            }
            notEnabledFields.forEach { f ->
                interactions.onContainer(f).waitUntil(not(isEnabled()), isDisplayed())
                interactions.onFieldValue(f).waitUntilWithText("")
            }
        }
    }
}
