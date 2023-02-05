package com.yandex.mail.testopithecus

import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.yandex.mail.BaseMailApplication
import com.yandex.mail.BuildConfig
import com.yandex.mail.metrica.TestopithecusTestLogger
import com.yandex.mail.rules.ClearAppDataBeforeEachTestRule
import com.yandex.mail.startupwizard.StartWizardActivity
import com.yandex.mail.testopithecus.feature.AndroidFeaturesRegistry
import com.yandex.mail.testopithecus.steps.isTablet
import com.yandex.mail.xmail.blockingGet
import com.yandex.xplat.common.DefaultJSONSerializer
import com.yandex.xplat.common.Log
import com.yandex.xplat.common.YSArray
import com.yandex.xplat.common.minInt32
import com.yandex.xplat.testopithecus.AllMailTests
import com.yandex.xplat.testopithecus.DefaultImapProvider
import com.yandex.xplat.testopithecus.LoginComponent
import com.yandex.xplat.testopithecus.MailboxBuilder
import com.yandex.xplat.testopithecus.MailboxModel
import com.yandex.xplat.testopithecus.MailboxPreparerProvider
import com.yandex.xplat.testopithecus.PublicBackendConfig
import com.yandex.xplat.testopithecus.common.DefaultSyncNetwork
import com.yandex.xplat.testopithecus.common.DeviceType
import com.yandex.xplat.testopithecus.common.JavaErrorThrower
import com.yandex.xplat.testopithecus.common.JavaLogger
import com.yandex.xplat.testopithecus.common.MBTPlatform
import com.yandex.xplat.testopithecus.common.MBTTest
import com.yandex.xplat.testopithecus.common.OAuthUserAccount
import com.yandex.xplat.testopithecus.common.Registry
import com.yandex.xplat.testopithecus.common.TestopithecusTestRunner
import com.yandex.xplat.testopithecus.common.ThreadSleep
import com.yandex.xplat.testopithecus.common.UserService
import com.yandex.xplat.testopithecus.common.getTrustedCases
import io.qameta.allure.android.rules.LogcatRule
import io.qameta.allure.android.rules.ScreenshotRule
import io.qameta.allure.android.rules.WindowHierarchyRule
import org.junit.After
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class TestopithecusTests(private val test: MBTTest<MailboxBuilder>, private val description: String) {

    @get:Rule
    val permissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )

    @get:Rule
    val chain = RuleChain.emptyRuleChain()
        .around(RetryTestRule(1))
        .around(ScreenshotRule(mode = ScreenshotRule.Mode.END))
        .around(WindowHierarchyRule())
        .around(LogcatRule())
        .around(ClearAppDataBeforeEachTestRule())
//        .around(ClearAppDataBeforeEachTestRule(mActivityTestRule))
//        .around(mActivityTestRule)

    val mActivityTestRule = ControlledActivityTestRule(StartWizardActivity::class.java, false, false)

    val imap = DefaultImapProvider()
    val network = DefaultSyncNetwork(DefaultJSONSerializer(), JavaLogger)
    val runner = TestopithecusTestRunner(
        MBTPlatform.Android,
        test,
        testRegistry,
        MailboxPreparerProvider(MBTPlatform.Android, DefaultJSONSerializer(), network, JavaLogger, ThreadSleep, imap),
        PublicBackendConfig.mailApplicationCredentials,
        network,
        DefaultJSONSerializer(),
        JavaLogger,
        assertionsEnabled = false
    )
    var accounts: YSArray<OAuthUserAccount> = mutableListOf()
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    init {
        UserService.userServiceOauthToken = BuildConfig.USER_SERVICE_OAUTH_TOKEN
        runner.statToken = BuildConfig.STAT_OAUTH_TOKEN
        runner.reporter = DefaultReportIntegration()
        val enabled = runner.isEnabled(MailboxModel.allSupportedFeatures, AndroidFeaturesRegistry.supportedFeatures)
        Assume.assumeTrue("Игнорирую тест. См. причину в предыдущей строчке лога.", enabled)
        Assume.assumeFalse("Ignore this test, because there is bug in application", testRegistry.isIgnored(test))
    }

    @Before
    fun setUp() {
        val application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as BaseMailApplication
        Assume.assumeFalse("Этот тест уже пройден", isTestAlreadyPassed(test.description))
        Log.info("Tест ${test.description} не был пройден ранее")

        val lockedAccounts = runner.lockAndPrepareAccountData().blockingGet()
        Assume.assumeFalse("No free account!", lockedAccounts.isEmpty())
        accounts = lockedAccounts

        application.getApplicationComponent().stories().resetStories()
        TestopithecusTestLogger.clear()
        mActivityTestRule.relaunchActivity()
    }

    @Test(timeout = 210000)
    fun testScenario() {
        val pal = AndroidFeaturesRegistry(device)
        try {
            runner.runTest(accounts, LoginComponent(), pal)
        } catch (e: RuntimeException) {
            Thread.sleep(2000)
            runner.failed()
            e.printStackTrace()
            throw e
        }
    }

    @After
    fun tearDown() {
        if (runner.isPassed() || isTestAlreadyFailed(test.description)) {
            Log.info("Отсылаем результаты в стату теста ${test.description}. isPasses = ${runner.isPassed()}, isTestAlreadyFailed = ${isTestAlreadyFailed(test.description)}")
            runner.sendTestsResults(test.description)
        }
        val isFailedFirst = !runner.isPassed() && !isTestAlreadyFailed(test.description) && !isTestAlreadyPassed(test.description)
        if (isFailedFirst) {
            Log.info("Тест ${test.description} упал в первый раз")
            Assume.assumeFalse("Тест упал в первый раз", isFailedFirst)
            runner.clearTestResults()
        }
        runner.finish()
    }

    companion object {
        private val allMailTests = AllMailTests.get
        val deviceTag = if (isTablet()) DeviceType.Tab else DeviceType.Phone
        val testRegistry = allMailTests
//            .only("true" == InstrumentationRegistry.getArguments().getString("required"))
            .onlyWithTag(deviceTag, MBTPlatform.Android)
            .onlyTestsWithCaseIds(
                "true" == InstrumentationRegistry.getArguments().getString("required"),
                getTrustedCases(
                    MBTPlatform.Android,
                    BuildConfig.TESTPALM_OAUTH_TOKEN,
                    DefaultSyncNetwork(DefaultJSONSerializer(), JavaLogger),
                    DefaultJSONSerializer()
                ),
                MBTPlatform.Android
            )
            .bucket(
                InstrumentationRegistry.getArguments().getString("bucketIndex", "0").toInt(),
                InstrumentationRegistry.getArguments().getString("bucketsTotal", "1").toInt()
            )
            .screenOnly(InstrumentationRegistry.getArguments().getString("screenOnly", "false").toBoolean())
            .retries(1)
//            .debug(ShortSwipeToArchiveThreadTest())
//            .debug(AddLabelsFromMessageView())

        val testsToRun =
            testRegistry.getTestsPossibleToRun(MBTPlatform.Android, MailboxModel.allSupportedFeatures, AndroidFeaturesRegistry.supportedFeatures)

        @Parameterized.Parameters(name = "{index}: {1}")
        @JvmStatic
        fun data(): Collection<Array<Any>> {
            setupRegistry()
            val tests = mutableListOf<Array<Any>>()
            for (test in testsToRun) {
                // имена тестов слишком большие, аллюр ломается
                tests.add(arrayOf(test, test.description.subSequence(0, minInt32(test.description.length, 75)).toString().replace(",", " ")))
            }
            Log.info("Будем пытаться запустить ${tests.size} тестов")
            return tests
        }

        private fun setupRegistry() {
            val registry = Registry.get()
            Log.registerDefaultLogger(JavaLogger)
            registry.errorThrower = JavaErrorThrower
        }
    }
}
