package ru.yandex.market.test.rule

import android.app.Activity
import androidx.test.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import kotlin.test.assertTrue

private const val ACTIVITY_CLOSE_TIMEOUT_MS = 2000L

class ActivityFinishedRule<T : Activity>(activityClass: Class<T>) :
    ActivityTestRule<T>(activityClass, false, false) {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)

    fun assertActivityFinished() {
        with(device) {
            MatcherAssert.assertThat(launcherPackageName, CoreMatchers.notNullValue())
            wait(Until.hasObject(By.pkg(launcherPackageName).depth(0)), ACTIVITY_CLOSE_TIMEOUT_MS)
        }
        assertTrue(activity == null || activity.isDestroyed)
    }
}