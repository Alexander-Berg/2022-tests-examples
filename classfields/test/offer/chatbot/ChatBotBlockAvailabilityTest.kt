package ru.auto.ara.test.offer.chatbot

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.robot.offercard.checkScreenshotOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchOfferDetailsActivity
import ru.auto.ara.ui.activity.OfferDetailsActivity
import ru.auto.data.model.data.offer.CAR

@RunWith(AndroidJUnit4::class)
class ChatBotBlockAvailabilityTest {
    private val dispatchers = listOf(
        GetOfferDispatcher.getOffer(CAR, OFFER_VAVAILABLE_FOR_CHECKUP)
    )
    var activityTestRule = lazyActivityScenarioRule<OfferDetailsActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        DisableAdsRule(),
        activityTestRule,
        SetPreferencesRule(),
    )

    @Test
    fun checkBotShowing() {
        val category = CAR
        val offerId = OFFER_VAVAILABLE_FOR_CHECKUP
        activityTestRule.launchOfferDetailsActivity(category, offerId)
        performOfferCard { scrollToChatbot() }
        checkScreenshotOfferCard { isChatBotBlockScreenshotTheSame() }
    }

    companion object {
        private const val OFFER_VAVAILABLE_FOR_CHECKUP = "7716531-aas881"
    }

}
