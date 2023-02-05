package ru.yandex.market.test

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.CallSuper
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.PerformException
import androidx.test.espresso.intent.Intents
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import io.github.kakaocup.kakao.Kakao
import io.github.kakaocup.kakao.screen.Screen
import io.qameta.allure.espresso.LogcatClearRule
import io.qameta.allure.espresso.LogcatDumpRule
import io.qameta.allure.espresso.WindowHierarchyRule
import io.qameta.allure.espresso.deviceScreenshot
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.yandex.market.activity.main.MainActivity
import ru.yandex.market.mocks.State
import ru.yandex.market.mocks.StateController
import ru.yandex.market.mocks.StateStore
import ru.yandex.market.mocks.local.store.LocalStateStore
import ru.yandex.market.mocks.model.RegionDeliveryMock
import ru.yandex.market.mocks.model.SkuMock
import ru.yandex.market.mocks.state.AppIconUpdateState
import ru.yandex.market.mocks.state.CartState
import ru.yandex.market.mocks.state.CatalogState
import ru.yandex.market.mocks.state.ChooseRegionState
import ru.yandex.market.mocks.state.CmsState
import ru.yandex.market.mocks.state.ComparisonListState
import ru.yandex.market.mocks.state.ComparisonState
import ru.yandex.market.mocks.state.CostLimitState
import ru.yandex.market.mocks.state.ExperimentState
import ru.yandex.market.mocks.state.ForceUpdateState
import ru.yandex.market.mocks.state.HintState
import ru.yandex.market.mocks.state.MailSubscriptionState
import ru.yandex.market.mocks.state.OnboardingState
import ru.yandex.market.mocks.state.OrdersState
import ru.yandex.market.mocks.state.RegionDeliveryState
import ru.yandex.market.mocks.state.SecretSaleState
import ru.yandex.market.mocks.state.SkuMockState
import ru.yandex.market.mocks.state.UserCoinsState
import ru.yandex.market.mocks.state.WishlistState
import ru.yandex.market.test.deeplink.TestDeeplink
import ru.yandex.market.test.rule.InitialStateTestRule
import ru.yandex.market.test.rule.LaunchRule
import ru.yandex.market.test.rule.waitForAppLaunchAndReady
import ru.yandex.market.test.runner.TestCaseJUnit4ClassRunner
import ru.yandex.market.test.util.AnalyticsChecker
import ru.yandex.market.test.util.AnalyticsHelper
import ru.yandex.market.test.util.closeSystemUiBrokenDialogIfExists
import ru.yandex.market.test.util.hasSystemWindow
import ru.yandex.market.test.util.noChecksRequired
import java.util.concurrent.TimeUnit

@RunWith(TestCaseJUnit4ClassRunner::class)
@Ignore("Base test case")
open class TestCase(val description: String, vararg states: State) {

    protected val instrumentation by lazy { InstrumentationRegistry.getInstrumentation() }
    private val device by lazy { UiDevice.getInstance(instrumentation) }

    protected val stateController by lazy { StateController(getStateStore(), instrumentation) }

    private val steps = mutableListOf<Step>()

    val states = mergeStatesWithDefault(states.toList())

    protected open val analyticsChecker: AnalyticsChecker = noChecksRequired

    protected var retryEnabled = RETRY_ENABLED

    init {
        scenario()
    }

    fun parseDeeplink(): Uri {
        return getDeeplink()?.getURI() ?: throw IllegalStateException("Trying to parse null deep link")
    }

    private fun mergeStatesWithDefault(states: List<State>): List<State> {
        return states + DEFAULT_STATES.filter { defaultState -> states.none { it.javaClass == defaultState.javaClass } }
    }

    fun sendExternalDeeplink(testDeeplink: TestDeeplink) {
        val context = instrumentation.targetContext
        val intent = Intent(Intent.ACTION_VIEW, testDeeplink.getURI())

        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis(), pendingIntent)
        device.waitForWindowUpdate(null, WAIT_TIMEOUT)
        device.waitForAppLaunchAndReady(context, WAIT_TIMEOUT)
    }

    protected fun waitForWindowUpdate() {
        device.wait(Until.hasObject(By.pkg(instrumentation.targetContext.packageName).depth(0)), WAIT_TIMEOUT)
    }

    @Deprecated("Don't use in production code, used only for auto-tests to debug network or UI")
    fun debugWait() {
        device.wait(
            Until.hasObject(By.pkg(instrumentation.targetContext.packageName).depth(1000)),
            DEBUG_WAIT_TIMEOUT
        )
    }

    fun waitGoneBy(selector: BySelector, timeout: Long = 10000L) {
        val condition = Until.gone(selector)
        device.wait(condition, timeout)
    }

    open fun getStateStore(): StateStore {
        return LocalStateStore()
    }

    protected fun addStep(name: String, ignoreFail: Boolean, test: () -> Unit) {
        steps.add(Step.create(name, ignoreFail, test))
    }

    open fun getDeeplink(): TestDeeplink? = null

    private fun getLaunchRule(): LaunchRule {
        return if (getDeeplink() != null)
            LaunchRule(parseDeeplink())
        else
            LaunchRule()
    }

    fun restartApplication() {
        val context = instrumentation.targetContext
        instrumentation.startActivitySync(
            MainActivity.getIntent(context)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        device.waitForAppLaunchAndReady(context)
    }

    fun exitApplication() {
        var isSuccessful: Boolean
        do {
            isSuccessful = device.pressBack()
        } while (isSuccessful)
    }

    @Rule
    open fun getRules(): RuleChain {
        return RuleChain.outerRule(InitialStateTestRule(stateController, this.states))
            .around(getLaunchRule())
            .around(LogcatDumpRule())
            .around(LogcatClearRule())
            .around(WindowHierarchyRule())
    }

    private fun setupKakao() {
        Kakao {
            intercept {
                onViewInteraction {
                    onCheck(true) { interaction, assertion ->
                        retry(uiDevice = device, retryEnabled = retryEnabled) {
                            interaction.check(assertion)
                        }
                    }

                    onPerform(true) { interaction, action ->
                        retry(uiDevice = device, retryEnabled = retryEnabled) {
                            interaction.perform(action)
                        }
                    }
                }
            }
        }
    }

    fun getIntents(): List<Intent> = Intents.getIntents()

    fun startIntentRecording() {
        Intents.init()
    }

    fun stopIntentRecording() {
        Intents.release()
    }

    @Before
    @CallSuper
    open fun setUp() {
        setupKakao()
        IdlingPolicies.setMasterPolicyTimeout(IDLE_TIMEOUT, TimeUnit.MILLISECONDS)
        stateController.onSetup()
    }

    @Test
    fun test() {
        var firstError: Throwable? = null
        steps.forEach { step ->
            step.test()
            step.error?.let { error ->
                if (firstError == null) {
                    firstError = error
                }
            }
        }
        firstError?.let { throw it }
    }

    @After
    @CallSuper
    open fun tearDown() {
        stateController.onTearDown()
        ensureImportantAnalyticsCheckedInTest()
    }

    fun waitForIdle() {
        device.waitForIdle()
    }

    sealed class Step {

        companion object {
            fun create(name: String, ignoreFail: Boolean, test: () -> Unit): Step {
                return if (ignoreFail) {
                    IgnoreFail(name, test)
                } else {
                    CanFail(name, test)
                }
            }
        }

        abstract val name: String
        protected abstract val task: () -> Unit
        abstract fun test()
        var error: Throwable? = null
            protected set

        class IgnoreFail(override val name: String, override val task: () -> Unit) : Step() {
            override fun test() {
                try {
                    task()
                } catch (e: Throwable) {
                    deviceScreenshot(name)
                    error = e
                }
            }
        }

        class CanFail(override val name: String, override val task: () -> Unit) : Step() {
            override fun test() {
                try {
                    task()
                } catch (e: Throwable) {
                    deviceScreenshot(name)
                    throw e
                }
            }
        }
    }

    companion object {
        private const val REQUEST_CODE = 101
        private const val WAIT_TIMEOUT = 5000L
        private const val DEBUG_WAIT_TIMEOUT = 60 * 60 * 1000L
        private const val RETRY_TIMEOUT = 10_000L
        private const val RETRY_DELAY = 1000L
        private const val IDLE_TIMEOUT = 10_000L

        private val REGION_ID = RegionDeliveryMock.MOSCOW.regionId

        private val DEFAULT_STATES = listOf(
            AppIconUpdateState(),
            OnboardingState.withRegion(regionId = REGION_ID),
            HintState.default(),
            CartState.empty(),
            OrdersState.empty(),
            SecretSaleState.withoutSecretSale(),
            MailSubscriptionState.withoutSubscriptions(),
            CostLimitState(CostLimitState.RegionLimit.list()),
            RegionDeliveryState.available().copy(regionId = REGION_ID),
            WishlistState.empty(),
            CatalogState.root(),
            ComparisonState.empty(),
            ComparisonListState.EMPTY,
            UserCoinsState.empty(),
            ExperimentState.withoutExperiments(),
            CmsState.morda(emptyList()),
            ForceUpdateState.dontNeedToUpdate(),
            SkuMockState(SkuMock.list()),
            ChooseRegionState(RegionDeliveryState.list())
        )

        private const val RETRY_ENABLED = true

        fun retry(
            retryEnabled: Boolean = RETRY_ENABLED,
            retryTimeout: Long = RETRY_TIMEOUT,
            retryDelay: Long = RETRY_DELAY,
            uiDevice: UiDevice,
            action: () -> Unit
        ) {
            if (retryEnabled) {
                val start = System.currentTimeMillis()
                val end = start + retryTimeout

                while (true) {
                    try {
                        action()
                        break
                    } catch (throwable: Throwable) {
                        if (throwable is PerformException) {
                            if (uiDevice.hasSystemWindow()) {
                                return
                            }
                        }
                        if (uiDevice.closeSystemUiBrokenDialogIfExists()) {
                            retry(retryEnabled, retryTimeout, retryDelay, uiDevice, action)
                            return
                        }
                        if (System.currentTimeMillis() >= end) throw throwable
                        Thread.sleep(retryDelay)
                    }
                }
            } else {
                action()
            }
        }
    }

    // Dsl

    protected operator fun String.invoke(ignoreFail: Boolean = false, test: () -> Unit) {
        addStep(
            name = this,
            ignoreFail = ignoreFail,
            test = test
        )
    }

    protected inline operator fun <reified T : Screen<T>> String.invoke(
        ignoreFail: Boolean = false,
        crossinline test: T.() -> Unit
    ) {
        addStep(name = this, ignoreFail = ignoreFail) {
            val screen = T::class.java.newInstance()
            screen.test()
        }
    }

    protected inline fun <reified T : Screen<T>> screen(test: ScreenObjectContext<T>.() -> Unit) {
        ScreenObjectContext(T::class.java.newInstance()).test()
    }

    inline fun <reified T : Screen<T>> action(test: ScreenObjectContext<T>.() -> Unit?) {
        ScreenObjectContext(T::class.java.newInstance()).test()
    }

    protected inline fun <reified T : Screen<T>> expectations(test: ScreenObjectContext<T>.() -> Unit) {
        ScreenObjectContext(T::class.java.newInstance()).test()
    }

    protected inline fun action(test: () -> Unit?) {
        test.invoke()
    }

    protected inline fun expectations(test: () -> Unit?) {
        test.invoke()
    }

    @DslMarker
    annotation class ScreenObjectDslMarker

    @ScreenObjectDslMarker
    inner class ScreenObjectContext<T : Screen<T>>(private val screen: T) {
        operator fun String.invoke(ignoreFail: Boolean = false, test: T.() -> Unit) {
            addStep(name = this, ignoreFail = ignoreFail) {
                screen.test()
            }
        }
    }

    protected open fun scenario() {
        stepPrecondition()
        stepOne()
        stepTwo()
        stepThree()
        stepFour()
        stepFive()
        stepSix()
        stepSeven()
        stepEight()
        stepNine()
        stepTen()
        stepEleven()
        stepTwelve()
        stepThirteen()
        stepFourteen()
        stepFifteen()
        stepSixteen()
    }

    protected open fun stepPrecondition() {
        // no-op (should be abstract)
    }

    protected open fun stepOne() {
        // no-op (should be abstract)
    }

    protected open fun stepTwo() {
        // no-op (should be abstract)
    }

    protected open fun stepThree() {
        // no-op (should be abstract)
    }

    protected open fun stepFour() {
        // no-op (should be abstract)
    }

    protected open fun stepFive() {
        // no-op (should be abstract)
    }

    protected open fun stepSix() {
        // no-op (should be abstract)
    }

    protected open fun stepSeven() {
        // no-op (should be abstract)
    }

    protected open fun stepEight() {
        // no-op (should be abstract)
    }

    protected open fun stepNine() {
        // no-op (should be abstract)
    }

    protected open fun stepTen() {
        // no-op (should be abstract)
    }

    protected open fun stepEleven() {
        // no-op (should be abstract)
    }

    protected open fun stepTwelve() {
        // no-op (should be abstract)
    }

    protected open fun stepThirteen() {
        // no-op (should be abstract)
    }

    protected open fun stepFourteen() {
        // no-op (should be abstract)
    }

    protected open fun stepFifteen() {
        // no-op (should be abstract)
    }

    protected open fun stepSixteen() {
        // no-op (should be abstract)
    }

    private fun ensureImportantAnalyticsCheckedInTest() {
        analyticsChecker.invoke()
        AnalyticsHelper.assertImportantAnalyticsChecked()
    }
}
