package ru.auto.ara.test.offer.stickers

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(Parameterized::class)
class StickersTest(private val testParams: TestParameter) {
    private val webServerRule = WebServerRule {
        delegateDispatchers(
            GetOfferDispatcher.getOffer(testParams.category, testParams.offerId)
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
    fun setUp() {
        activityTestRule.launchDeepLinkActivity(testParams.uri)
        performOfferCard { scrollToStickers() }
    }

    @Test
    fun shouldSeeBadges() {
        checkOfferCard {
            isListOfStickersDisplayed(listOf("Два комплекта резины", "Кожаный салон", "Не участвовала в дтп"))
        }
    }

    companion object {
        @Parameterized.Parameters(name = "index={index} {0}")
        @JvmStatic
        fun data(): Collection<Array<out Any?>> = listOf(
            TestParameter(
                offerId = "1083280948-dc2c56",
                uri = "https://auto.ru/cars/new/sale/1083280948-dc2c56",
                category = "cars"
            ),
            TestParameter(
                offerId = "1894128-d229",
                uri = "https://auto.ru/moto/new/sale/1894128-d229",
                category = "moto"
            ),
            TestParameter(
                offerId = "10448426-ce654669",
                uri = "https://auto.ru/trucks/new/sale/10448426-ce654669",
                category = "trucks"
            )
        ).map { arrayOf(it) }
    }

    data class TestParameter(
        val uri: String,
        val offerId: String,
        val category: String
    ) {
        override fun toString(): String = category
    }
}
