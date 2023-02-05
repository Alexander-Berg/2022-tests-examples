package ru.yandex.market.test

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import org.junit.Rule
import org.junit.rules.RuleChain
import ru.yandex.market.test.rule.DisableAnimationsRule
import ru.yandex.market.test.rule.LaunchRule
import ru.yandex.market.test.rule.waitForAppLaunchAndReady

open class MarketTestCase : TestCase() {
    protected val instrumentation by lazy { InstrumentationRegistry.getInstrumentation() }
    protected val context = instrumentation.context
    private val device by lazy { UiDevice.getInstance(instrumentation) }

    @get:Rule
    open var rules: RuleChain = RuleChain
        .outerRule(DisableAnimationsRule())
        .around(getLaunchRule())

    private fun getLaunchRule(): LaunchRule = LaunchRule()

    fun sendExternalDeeplink(testDeeplink: String) {
        val context = instrumentation.targetContext
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(testDeeplink))
        intent.flags = FLAG_ACTIVITY_NEW_TASK

        instrumentation.context.startActivity(intent)
        device.waitForWindowUpdate(null, WAIT_TIMEOUT)
        device.waitForAppLaunchAndReady(context, WAIT_TIMEOUT)
    }

    companion object {
        private const val WAIT_TIMEOUT = 10000L
    }
}