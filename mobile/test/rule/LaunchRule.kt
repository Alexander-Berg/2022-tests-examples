package ru.yandex.market.test.rule

import android.content.Context
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import ru.yandex.market.feature.main.MainActivity

private const val DEFAULT_LAUNCH_TIMEOUT_MS = 5000L

class LaunchRule : TestRule {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private val appUnderTestContext = instrumentation.targetContext.applicationContext
    private val packageManager = appUnderTestContext.packageManager

    private val launchIntent = /*packageManager
        .getLaunchIntentForPackage(appUnderTestContext.packageName)
        ?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)*/
        Intent(appUnderTestContext, MainActivity::class.java)

    override fun apply(base: Statement, description: Description): Statement = LaunchStatement(base)

    private inner class LaunchStatement(private val base: Statement) : Statement() {

        override fun evaluate() {
            device.pressHome()
            device.waitForLauncher()

            instrumentation.startActivitySync(
                launchIntent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK).addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            )
            device.waitForAppLaunchAndReady(appUnderTestContext)
            base.evaluate()
        }
    }
}

fun UiDevice.waitForLauncher(timeout: Long = DEFAULT_LAUNCH_TIMEOUT_MS) {
    MatcherAssert.assertThat(launcherPackageName, CoreMatchers.notNullValue())
    wait(Until.hasObject(By.pkg(launcherPackageName).depth(0)), timeout)
}

fun UiDevice.waitForAppLaunchAndReady(
    appUnderTestContext: Context,
    timeout: Long = DEFAULT_LAUNCH_TIMEOUT_MS
) {
    wait(Until.hasObject(By.pkg(appUnderTestContext.packageName).depth(0)), timeout)
}