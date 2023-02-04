package ru.auto.ara.test.offer.contacts

import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.actions.ViewActions.setAppBarExpandedState
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.dispatchers.phones.GetPhonesDispatcher
import ru.auto.ara.core.dispatchers.salon.getCustomizableSalonById
import ru.auto.ara.core.dispatchers.salon.getSalonPhones
import ru.auto.ara.core.robot.checkCommon
import ru.auto.ara.core.robot.dealer.checkDealerContacts
import ru.auto.ara.core.robot.dealer.checkDealerFeed
import ru.auto.ara.core.robot.dealer.performDealerContacts
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.getAutoRuYears
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class DealerNewTest {
    private val webServerRule = WebServerRule {
        getCustomizableSalonById(
            dealerId = 20134444,
            dealerCode = "baltavtotreyd_m_moskva_bmw"
        )
        delegateDispatchers(
            GetOfferDispatcher.getOffer("cars", "1084250931-f8070529"),
            GetPhonesDispatcher.onePhone("1084250931-f8070529")
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
        activityTestRule.launchDeepLinkActivity("https://auto.ru/cars/used/sale/1084250931-f8070529")
        performOfferCard {
            interactions.onAppBar().waitUntilIsCompletelyDisplayed().perform(setAppBarExpandedState(false))
            scrollToRequestCall()
        }
    }

    @Test
    fun shouldSeeDealerBlock() {
        checkOfferCard {
            isDealerContactsDisplayed(
                name = "БалтАвтоТрейд-М BMW Москва",
                officialLabel = "Официальный дилер",
                ageLabel = "На Авто.ру ${getAutoRuYears()} лет",
                isOfficialDealer = true
            )
        }
    }

    @Test
    fun shouldSeeDealerContactsBottomsheet() {
        performOfferCard { interactions.onDealerName().waitUntilIsCompletelyDisplayed().performClick() }
        checkDealerContacts { checkDealerContactsDialogIsDisplayed() }
    }

    @Test
    fun shouldMakeCallFromBottomsheet() {
        webServerRule.routing { getSalonPhones(dealerCode = SALON_CODE) }

        performOfferCard { interactions.onDealerName().waitUntilIsCompletelyDisplayed().performClick() }

        Intents.init()
        performDealerContacts { clickOnCallButton() }

        checkCommon { isActionDialIntentCalled("+7 923 454-32-10") }
    }

    @Test
    fun shouldOpenDealersFeedFromBottomsheetAfterClickDealerName() {
        performOfferCard {
            interactions.onDealerName().waitUntilIsCompletelyDisplayed().performClick()
        }
        performDealerContacts { clickOnTitle() }
        checkDealerFeed { checkDealerFeedIsOpen() }
    }

    @Test
    fun shouldOpenDealersFeedFromBottomsheetAfterClickCarsInSale() {
        performOfferCard { interactions.onDealerName().waitUntilIsCompletelyDisplayed().performClick() }
        performDealerContacts { clickOnOffersCount() }
        checkDealerFeed { checkDealerFeedIsOpen() }
    }

    companion object {
        private const val SALON_CODE = "baltavtotreyd_m_moskva_bmw"
    }
}
