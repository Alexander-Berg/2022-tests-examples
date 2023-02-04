package ru.auto.ara.test.deeplink

import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class OfferCardReturnToListingFromDeeplinkTest {
    private val dispatchers: List<DelegateDispatcher> = listOf(
        GetOfferDispatcher.getOffer("cars", CAR_OFFER_ID),
        GetOfferDispatcher.getOffer("moto", MOTO_OFFER_ID)
    )
    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        DisableAdsRule(),
        activityTestRule,
        SetPreferencesRule()
    )

    @Test
    fun shouldOpenSearchWithSameMMGWhenGoingBackFromAutoDeeplink() {
        activityTestRule.launchDeepLinkActivity("http://auto.ru/cars/used/sale/$CAR_OFFER_ID/")
        performOfferCard().checkResult { isOfferCard() }

        pressBack()

        performSearchFeed().checkResult {
            isMarkFilterWithText("BMW")
            isModelFilterWithText("X3")
            isGenerationFilterWithText("2010 - 2014 II (F25)")
        }
    }

    @Test
    fun shouldNotOpenSearchWhenGoingBackFromMotoCommDeeplink() {
        activityTestRule.launchDeepLinkActivity("http://auto.ru/moto/used/sale/$MOTO_OFFER_ID/")
        performOfferCard().checkResult { isOfferCard() }

        pressBack()

        performMain().checkResult {
            isMainTabSelected(R.string.transport)
            isLowTabSelected(R.string.search)
        }
    }

    companion object {
        private const val CAR_OFFER_ID = "1086435582-a036286a"
        private const val MOTO_OFFER_ID = "2651406-adf0e32d"
    }
}
