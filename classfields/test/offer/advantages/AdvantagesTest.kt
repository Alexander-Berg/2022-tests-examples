package ru.auto.ara.test.offer.advantages

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.OFFER_ADVANTAGES
import ru.auto.ara.core.testdata.OfferAdvantageTestParams
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(Parameterized::class)
class AdvantagesTest(private val params: OfferAdvantageTestParams) {

    private val activityRule = lazyActivityScenarioRule<DeeplinkActivity>()
    private val webServerRule = WebServerRule()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityRule,
        SetPreferencesRule()
    )

    @Test
    fun shouldShowAdvantages() {
        openOfferCard(params.offerIdWithAllAdvantages)
        performOfferCard {
            scrollToAdvantages()
            scrollToAdvantage(params.position)
        }.checkResult {
            isAdvantageDisplayed(
                position = params.position,
                imageResId = params.imageResId,
                title = params.title,
                subtitle = params.subtitle
            )
        }
    }

    @Test
    fun shouldShowSingleAdvantage() {
        openOfferCard(params.offerIdWithSingleAdvantage)
        performOfferCard {
            scrollToAdvantageSingle()
        }.checkResult {
            isAdvantageSingleDisplayed(
                imageResId = params.imageResId,
                title = params.title,
                subtitle = params.subtitle
            )
        }
    }

    private fun openOfferCard(offerId: String) {
        webServerRule.routing { getOffer(offerId) }
        activityRule.launchDeepLinkActivity(OFFER_CARD_PATH + offerId)
    }

    companion object {
        private const val OFFER_CARD_PATH = "https://auto.ru/cars/used/sale/"

        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data() = OFFER_ADVANTAGES.map { arrayOf(it) }
    }
}
