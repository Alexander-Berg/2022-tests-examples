package ru.auto.ara.test.other.notifications

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.ui.activity.SurfaceActivity
import ru.auto.ara.core.robot.othertab.performNotifications
import ru.auto.ara.core.robot.favorites.performSavedFilters
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.ui.fragment.notifications.NotificationsFragment
import ru.auto.data.util.ARG_FRAGMENT_CLASS

@RunWith(AndroidJUnit4::class)
class NotificationsTest {

    var activityTestRule = lazyActivityScenarioRule<SurfaceActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule(),
        activityTestRule
    )

    @Before
    fun setUp() {
        val startIntent = Intent().apply {
            putExtra(ARG_FRAGMENT_CLASS, NotificationsFragment::class.java)
        }
        activityTestRule.launchActivity(startIntent)
    }


    @Test
    @Ignore("not work if start via intent, need Splash Activity")
    fun shouldOpenSavedSearch() {
        performNotifications {
            interactions.onSavedFilters().checkIsCompletelyDisplayed().performClick()
        }
        performSavedFilters {}.checkResult { isEmpty() }
    }
}
