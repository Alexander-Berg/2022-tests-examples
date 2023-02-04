package ru.auto.ara.test.offer.contacts

import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.actions.ViewActions
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.matchers.TextInputLayoutMatchers
import ru.auto.ara.core.matchers.ViewMatchers.withClearText
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class RequestCallTest {
    private val webServerRule = WebServerRule {
        delegateDispatchers(
            GetOfferDispatcher.getOffer("cars", "1084250931-f8070529"),
        )
        userSetup()
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
        activityTestRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/1084250931-f8070529")
        performOfferCard {
            interactions.onAppBar().waitUntilIsCompletelyDisplayed().perform(ViewActions.setAppBarExpandedState(false))
            scrollToComplain()
        }
    }

    @Test
    fun shouldSeeErrorHint() {
        performOfferCard { interactions.onRequestCallButton().waitUntilIsCompletelyDisplayed().performClick() }.checkResult {
            interactions.onRequestCallInputLayout()
                .waitUntil(isCompletelyDisplayed(), TextInputLayoutMatchers.withError(R.string.request_call_phone_error))
        }
    }

    @Test
    fun shouldSeePlusSevenInNumberAfterSetFocus() {
        performOfferCard { interactions.onRequestCallInputLayout().waitUntilIsCompletelyDisplayed().performClick() }.checkResult {
            interactions.onRequestCallInput().waitUntil(ViewMatchers.isDisplayed(), withClearText("+7"))
        }
    }

    //More tests will come when migrate from node-api to public-api...
}
