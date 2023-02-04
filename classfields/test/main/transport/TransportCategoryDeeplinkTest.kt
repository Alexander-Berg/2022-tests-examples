package ru.auto.ara.test.main.transport

import androidx.annotation.StringRes
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.device.ParseDeeplinkDispatcher
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.robot.transporttab.performTransport
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.pressBack
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.deeplink.DeeplinkActivity
import java.util.concurrent.TimeUnit

@RunWith(Parameterized::class)
class TransportCategoryDeeplinkTest(
    private val param: TestParam
) {
    private val webServerRule = WebServerRule {
        delegateDispatcher(param.dispatcher)
    }
    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityTestRule
    )

    @Test
    fun shouldUpdateScreenOnCategorySegmentClick() {
        activityTestRule.launchDeepLinkActivity(param.url)
        waitSomething(3, TimeUnit.SECONDS)
        performSearchFeed {
            waitSearchFeed()
            pressBack()
            waitSomething(3, TimeUnit.SECONDS)
        }
        performTransport {}.checkResult {
            checkCategorySegmentSelected(param.category)
        }
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data() = listOf(
            TestParam("https://auto.ru/cars/all/", R.string.category_auto, ParseDeeplinkDispatcher.carsAll(), "auto"),
            TestParam("https://auto.ru/scooters/new/", R.string.category_moto, ParseDeeplinkDispatcher.scootersNew(), "moto"),
            TestParam("https://auto.ru/lcv/all/", R.string.category_comm, ParseDeeplinkDispatcher.lcvAll(), "trucks")
        )

        data class TestParam(
            val url: String,
            @StringRes val category: Int,
            val dispatcher: DelegateDispatcher,
            val description: String
        ) {
            override fun toString() = description
        }
    }
}
