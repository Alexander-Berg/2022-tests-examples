package ru.auto.ara.test.offer

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
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
class OfferBodyTypeNumberTest {

    private val dispatchers = listOf(
        GetOfferDispatcher.getOffer(CAR, OFFER_BODY_NUMBER),
        GetOfferDispatcher.getOffer(CAR, OFFER_NO_VIN_OR_BODY_NUMBER),
        GetOfferDispatcher.getOffer(CAR, OFFER_VIN)
    )
    var activityTestRule = lazyActivityScenarioRule<OfferDetailsActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        DisableAdsRule(),
        activityTestRule,
        SetPreferencesRule()
    )

    @Test
    fun shouldShowVinNumberIfFieldNotEmpty() {
        val category = CAR
        val offerId = OFFER_BODY_NUMBER
        val bodyNumber = "Z8NFEAJ1"
        activityTestRule.launchOfferDetailsActivity(category = category, offerId = offerId)

        performOfferCard { scrollToAllParameters() }.checkResult { isBodyNumberEquals(bodyNumber) }
    }

    @Test
    fun shouldNotShowBodyNumberIfNoVinPresent() {
        val category = CAR
        val offerId = OFFER_NO_VIN_OR_BODY_NUMBER
        activityTestRule.launchOfferDetailsActivity(category = category, offerId = offerId)

        performOfferCard { scrollToAllParameters() }.checkResult { noBodyNumberFieldIsVisible() }
    }

    @Test
    fun shouldNotShowBodyNumberIfVinIsLongerOrEqualTo17() {
        val category = CAR
        val offerId = OFFER_VIN
        activityTestRule.launchOfferDetailsActivity(category = category, offerId = offerId)

        performOfferCard { scrollToAllParameters() }.checkResult { noBodyNumberFieldIsVisible() }
    }

    companion object {
        private const val OFFER_NO_VIN_OR_BODY_NUMBER = "1077957027-c7abdb2f"
        private const val OFFER_BODY_NUMBER = "1074918427-cdd53a67"
        private const val OFFER_VIN = "1084193117-d3fa3f07"
    }
}
