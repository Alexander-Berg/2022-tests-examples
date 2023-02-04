package ru.auto.ara.test.offer.communicate

import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.ExperimentsScope
import ru.auto.ara.core.di.module.TestMainModuleArguments
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.dispatchers.phones.GetPhonesDispatcher
import ru.auto.ara.core.experimentsOf
import ru.auto.ara.core.matchers.ViewMatchers.withClearText
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.experiments.Experiments
import ru.auto.experiments.letsCallDealer


/**
 *  remove with [Experiments.letsCallDealer]
 */
@RunWith(AndroidJUnit4::class)
abstract class BaseLetsCallDealerExpTest(exp: ExperimentsScope.() -> Unit) {

    protected val dispatcherGetPhonesHolder = DispatcherHolder()
    protected val dispatcherGetOfferHolder = DispatcherHolder()
    private val dispatchers: List<DelegateDispatcher> = listOf(
        dispatcherGetOfferHolder,
        dispatcherGetPhonesHolder
    )
    var activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        DisableAdsRule(),
        activityTestRule,
        SetPreferencesRule(),
        arguments = TestMainModuleArguments(
            testExperiments = experimentsOf(exp)
        )
    )

    @Before
    fun setUp() {
        dispatcherGetOfferHolder.innerDispatcher = GetOfferDispatcher.getOffer("cars", "1096920532-7b934805")
        dispatcherGetPhonesHolder.innerDispatcher = GetPhonesDispatcher.onePhone("1096920532-7b934805")
        activityTestRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/1096920532-7b934805")
    }

}

@RunWith(AndroidJUnit4::class)
class LetsCallDealerTrueTest : BaseLetsCallDealerExpTest({ Experiments::letsCallDealer then true }) {
    @Test
    fun shouldSeeCallSellerOnlineCallToAction() {
        checkOfferCard {
            interactions.onMakeCallButtonTitle().waitUntil(
                isCompletelyDisplayed(),
                withClearText(R.string.makeCall)
            )
            interactions.onMakeCallButtonSubtitle().waitUntil(
                isCompletelyDisplayed(),
                withClearText(R.string.seller_online_call_them)
            )
        }
    }
}

@RunWith(AndroidJUnit4::class)
class LetsCallDealerFalseTest : BaseLetsCallDealerExpTest({ Experiments::letsCallDealer then false }) {
    @Test
    fun shouldSeeTimeCallToAction() {
        checkOfferCard {
            interactions.onMakeCallButtonTitle().waitUntil(isCompletelyDisplayed(), withClearText(R.string.makeCall))
            interactions.onMakeCallButtonSubtitle().waitUntil(isCompletelyDisplayed(), withClearText("с 8:00 до 22:00"))
        }
    }
}
