package ru.auto.ara.test.listing.snippet.footer

import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.device.ParseDeeplinkDispatcher
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.dispatchers.phones.GetPhonesDispatcher
import ru.auto.ara.core.dispatchers.salon.getCustomizableSalonById
import ru.auto.ara.core.dispatchers.salon.getSalonPhones
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.robot.checkCommon
import ru.auto.ara.core.robot.dealer.checkDealerContacts
import ru.auto.ara.core.robot.dealer.checkDealerFeed
import ru.auto.ara.core.robot.dealer.performDealerContacts
import ru.auto.ara.core.robot.searchfeed.checkListingOffers
import ru.auto.ara.core.robot.searchfeed.performListingOffers
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class CommercialNewTest {
    private val DEFAULT_LISTING_DEEPLINK = "https://auto.ru/cars/all"

    private val webServerRule = WebServerRule {
        getCustomizableSalonById(
            dealerId = 20134444,
            dealerCode = SALON_CODE
        )
        delegateDispatchers(
            CountDispatcher(category = "cars"),
            PostSearchOffersDispatcher(fileName = "extended_dealer_new"),
            GetPhonesDispatcher.onePhone(offerId = "1084250931-f8070529"),
            ParseDeeplinkDispatcher.carsAll(),
            GetOfferDispatcher.getOffer(category = "cars", offerId = "1084250931-f8070529")
        )
    }
    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule()
    )

    @Before
    fun setupDispatchers() {
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers { scrollToFilterPromo() }
    }

    @Test
    fun shouldSeeNotHiddenDealerContacts() {
        checkListingOffers {
            isSellerContactsDisplayed("БалтАвтоТрейд-М BMW Москва\nКонтакты")
            isCallButtonDisplayed()
        }
    }

    @Test
    fun shouldMakeCallFromSnippet() {
        Intents.init()
        performListingOffers { interactions.onCallOrChatButton().waitUntilIsCompletelyDisplayed().performClick() }
        checkCommon { isActionDialIntentCalled("+7 985 440-66-27") }
    }

    @Test
    fun shouldSeeDealerContactsBottomsheet() {
        performListingOffers { interactions.onSeller().waitUntilIsCompletelyDisplayed().performClick() }
        checkDealerContacts { checkDealerContactsDialogIsDisplayed() }
    }

    @Test
    fun shouldMakeCallFromBottomsheet() {
        webServerRule.routing { getSalonPhones(dealerCode = SALON_CODE) }
        performListingOffers { interactions.onSeller().waitUntilIsCompletelyDisplayed().performClick() }

        Intents.init()
        performDealerContacts { clickOnCallButton() }

        checkCommon { isActionDialIntentCalled("+7 923 454-32-10") }
    }

    @Test
    fun shouldOpenDealersFeedFromBottomsheetAfterClickDealerName() {
        performListingOffers { interactions.onSeller().waitUntilIsCompletelyDisplayed().performClick() }
        performDealerContacts { clickOnTitle() }
        checkDealerFeed { checkDealerFeedIsOpen() }
    }

    @Test
    fun shouldOpenDealersFeedFromBottomsheetAfterClickCarsInSale() {
        performListingOffers { interactions.onSeller().waitUntilIsCompletelyDisplayed().performClick() }
        performDealerContacts { clickOnOffersCount() }
        checkDealerFeed { checkDealerFeedIsOpen() }
    }

    companion object {
        private const val SALON_CODE = "baltavtotreyd_m_moskva_bmw"
    }
}
