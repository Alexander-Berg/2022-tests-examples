package ru.auto.ara.test.offer.manual

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.dispatchers.offer_card.GetRelatedDispatcher
import ru.auto.ara.core.robot.offercard.OfferCardRobot
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.robot.webview.checkWebView
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.MANUAL_PARAMS
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.deeplink.DeeplinkActivity
import java.util.concurrent.TimeUnit

@RunWith(Parameterized::class)
class ManualTest(private val testParameter: TestParameter) {

    private val webServerRule = WebServerRule {
        delegateDispatchers(
            GetOfferDispatcher.getOffer("cars", "1082957054-8d55bf9a"),
            GetRelatedDispatcher.relatedCars()
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

    @Before
    fun setup() {
        activityTestRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/1082957054-8d55bf9a")
        scrollToManual()
    }

    @Test
    fun shouldSeeManualItem() {
        checkOfferCard {
            interactions.onManualBlockTitle().waitUntilIsCompletelyDisplayed()
            testParameter.check(this)
        }
        performOfferCard {
            interactions.onManualTitle(testParameter.title).performClick()
        }
        checkWebView { isWebViewToolBarDisplayed("Учебник") }
    }

    private fun scrollToManual() {
        checkOfferCard { interactions.onMakeCallOnCardButton().waitUntilIsCompletelyDisplayed() }
        performOfferCard {
            collapseAppBar()
            scrollToComplain() //to initiate request for special block
            waitSomething(500, TimeUnit.MILLISECONDS) //wait until specials view is presented
            scrollToSpecials()
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "index={index}")
        fun data(): Collection<Array<out Any?>> = MANUAL.map { arrayOf(it) }

        private val MANUAL = MANUAL_PARAMS.map { manualParam ->
            TestParameter(
                title = manualParam.title,
                check = { isManualItemDisplayed(manualParam.title, manualParam.subtitle, manualParam.image) }
            )
        }.plus(
            TestParameter(
                title = "Больше статей\nпро покупку авто",
                check = { isManualItemWithoutSubtitleDisplayed("Больше статей\nпро покупку авто", "", R.drawable.uchebnik_more) }
            )
        )

        data class TestParameter(
            val title: String,
            val check: OfferCardRobot.OfferCardRobotChecker.() -> Unit
        ) {
            override fun toString() = "check manual"
        }
    }
}
