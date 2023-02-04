package ru.auto.ara.test.deeplink

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.device.postHello
import ru.auto.ara.core.robot.CommonRobot.CommonChecker.isOpenDeeplinkIntentCalled
import ru.auto.ara.core.robot.performCommon
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.di.TestAppStoreChecker
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.core.utils.withIntents
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.core_ui.util.AppStoreType

@RunWith(AndroidJUnit4::class)
class StoreDeeplinkTest {

    private val activityRule = lazyActivityScenarioRule<DeeplinkActivity>()

    private val webServerRule = WebServerRule()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityRule
    )

    @Before
    fun setUp() {
        // brake all network interactions due to it makes tests more stable :3
        webServerRule.routing {
            postHello()
        }
    }

    @After
    fun finalize() {
        performCommon {
            pressSystemKeyBack()
        }
    }

    @Test
    fun shouldOpenGooglePlayStoreWhenAvailable() {
        TestAppStoreChecker.testStoreResult = AppStoreType.GOOGLE_PLAY_MARKET
        withIntents {
            activityRule.launchDeepLinkActivity(APP_STORE_APPLINK)
            // add additional timing due to delay at espresso intent recognition system
            waitSomething(2)
            isOpenDeeplinkIntentCalled("market://details?id=ru.auto.ara")
        }
    }

    @Test
    fun shouldOpenHuaweiAppGalleryWhenAvailable() {
        TestAppStoreChecker.testStoreResult = AppStoreType.HUAWEI_APP_GALLERY
        withIntents {
            activityRule.launchDeepLinkActivity(APP_STORE_APPLINK)
            // add additional timing due to delay at espresso intent recognition system
            waitSomething(2)
            isOpenDeeplinkIntentCalled("appmarket://details?id=ru.auto.ara")
        }
    }

    @Test
    fun shouldOpenBrowserWhenNoneStoresAvailable() {
        TestAppStoreChecker.testStoreResult = AppStoreType.NONE
        withIntents {
            activityRule.launchDeepLinkActivity(APP_STORE_APPLINK)
            // add additional timing due to delay at espresso intent recognition system
            waitSomething(2)
            isOpenDeeplinkIntentCalled("https://play.google.com/store/apps/details?id=ru.auto.ara")
        }
    }
}

private const val APP_STORE_APPLINK = "autoru://app/store"
