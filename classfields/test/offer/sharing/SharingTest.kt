package ru.auto.ara.test.offer.sharing

import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.robot.checkCommon
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
import ru.auto.ara.ui.activity.OfferDetailsActivity

@RunWith(AndroidJUnit4::class)
class SharingTest {
    private val dispatchers: List<DelegateDispatcher> = listOf(
        GetOfferDispatcher.getOffer("cars", CAR_OFFER_ID),
        GetOfferDispatcher.getOffer("moto", MOTO_OFFER_ID)
    )

    var activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        DisableAdsRule(),
        activityTestRule,
        SetPreferencesRule(),
        IntentsTestRule(OfferDetailsActivity::class.java)
    )

    @Test
    fun shouldShareMotoFromToolbar() {
        activityTestRule.launchDeepLinkActivity(MOTO_OFFER)
        checkOfferCard {
            compareToolBarWithShareButtonScreenshots()
        }
        performOfferCard {
            interactions.onShareButton().waitUntilIsCompletelyDisplayed().performClick()
        }
        checkCommon {
            isSendPlainTextIntentCalled(MOTO_OFFER, "Поделиться с помощью:")
        }
    }

    @Test
    fun shouldShareCarFromOffer() {
        activityTestRule.launchDeepLinkActivity(CAR_OFFER)
        checkOfferCard {
            compareToolBarWithShareButtonScreenshots()
        }
        performOfferCard {
            interactions.onShareButton().performClick()
        }
        checkCommon {
            isSendPlainTextIntentCalled(CAR_OFFER, "Поделиться с помощью:")
        }
    }

    companion object {
        private var CAR_OFFER_ID = "1082957054-8d55bf9a"
        private var CAR_OFFER = "https://auto.ru/cars/used/sale/$CAR_OFFER_ID"
        private var MOTO_OFFER_ID = "1894128-d229"
        private var MOTO_OFFER = "https://auto.ru/motovezdehody/used/sale/$MOTO_OFFER_ID"
    }
}
