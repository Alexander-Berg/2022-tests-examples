package ru.auto.ara.test.offer.contacts

import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.actions.ViewActions.setAppBarExpandedState
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.matchers.ViewMatchers.withClearText
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

@RunWith(AndroidJUnit4::class)
class PrivateTest {
    private val webServerRule = WebServerRule {
        delegateDispatchers(
            GetOfferDispatcher.getOffer("cars", "1082957054-8d55bf9a")
        )
    }
    var activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityTestRule,
        SetPreferencesRule(),
    )

    @Before
    fun setUp() {
        activityTestRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/1082957054-8d55bf9a")
        performOfferCard {
            interactions.onAppBar().waitUntilIsCompletelyDisplayed().perform(setAppBarExpandedState(false))
            scrollToSellerContacts()
        }
    }

    @Test
    fun shouldSeePrivateSellerContactsBlock() {
        performOfferCard { scrollToSellerContacts() }
        checkOfferCard {
            isPrivateSellerContactsDisplayed(
                "id41057536",
                "Москва На карте",
                "  Пражская    Южная"
            )
        }
    }

    @Test
    fun shouldOpenAlertWithAppsAfterClickAddress() {
        performOfferCard { scrollToChatbot() }
        performOfferCard {
            interactions.onSellerAddress().waitUntilIsCompletelyDisplayed().performClick()
            Intents.init()
        }.checkResult {
            interactions.onAlertTitle().waitUntil(isCompletelyDisplayed(), withClearText(R.string.car_navigation_dialog_title))
        }
        performOfferCard { interactions.onAppInAlert("Maps").waitUntilIsCompletelyDisplayed().performClick() }
        checkCommon { isOpenMapIntentCalled("geo:0,0?q=55.753216,37.622505(Москва)") }
    }
}
