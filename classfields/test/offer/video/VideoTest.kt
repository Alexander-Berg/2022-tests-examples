package ru.auto.ara.test.offer.video

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.dispatchers.video.GetVideoDispatcher
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.robot.webview.checkWebView
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.deeplink.DeeplinkActivity
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class VideoTest {
    private val CAR_USED_URI = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a"
    private val videoWatcher = RequestWatcher()

    private val webServerRule = WebServerRule {
        delegateDispatchers(
            GetOfferDispatcher.getOffer("cars", "1082957054-8d55bf9a"),
            GetVideoDispatcher(expectedResponse = "common_videos", requestWatcher = videoWatcher)
        )
    }


    var activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityTestRule,
        SetPreferencesRule()
    )

    @Test
    fun shouldSeeBlockOfVideos() {
        activityTestRule.launchDeepLinkActivity(CAR_USED_URI)
        waitSomething(5, TimeUnit.SECONDS)
        performOfferCard {
            scrollToCatalog()
        }.checkResult {
            isVideoItemDisplayed("AUDI Q8 (2018) - обзор и тест-драйв от Елены Добровольской", "4:39")
        }
        performOfferCard { interactions.onGallerySubtitle("AUDI Q8 (2018) - обзор и тест-драйв от Елены Добровольской").performClick() }
        checkWebView { isWebViewToolBarDisplayed("AUDI Q8 (2018) - обзор и тест-драйв от Елены Добровольской") }
        videoWatcher.checkQueryParameters(
            listOf(
                "mark" to "BMW", "model" to "4 серия", "page" to "1", "page_size" to "12", "super_gen" to "F32/F33/F36"
            )
        )
    }

}
