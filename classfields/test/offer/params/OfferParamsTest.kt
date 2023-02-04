package ru.auto.ara.test.offer.params

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.robot.offercard.checkScreenshotOfferCard
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class OfferParamsTest {

    private val dispatchers: List<DelegateDispatcher> = listOf(
        GetOfferDispatcher.getOffer("cars", OFFER_ID)
    )

    private val activityRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        activityRule,
        SetPreferencesRule()
    )

    @Test
    fun shouldSeeAdditionalParamsWithGeoAndCounters() {
        activityRule.launchDeepLinkActivity(OFFER_DEEPLINK + OFFER_ID)
        checkScreenshotOfferCard {
            isAdditionalParamsSame("offer_card/additional/public_offer.png")
        }
    }

    companion object {
        private const val OFFER_DEEPLINK = "https://auto.ru/cars/used/sale/"
        private const val OFFER_ID = "1089249352-3dc864f3"
    }
}
