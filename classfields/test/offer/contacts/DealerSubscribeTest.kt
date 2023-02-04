package ru.auto.ara.test.offer.contacts

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.dispatchers.phones.GetPhonesDispatcher
import ru.auto.ara.core.dispatchers.salon.getCustomizableSalonById
import ru.auto.ara.core.dispatchers.search_offers.postSavedSearch
import ru.auto.ara.core.dispatchers.search_offers.postSearchOffers
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.checkScreenshotOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.data.model.VehicleCategory

@RunWith(AndroidJUnit4::class)
class DealerSubscribeTest {

    private val webServerRule = WebServerRule {
        getCustomizableSalonById(
            dealerId = 20134444,
            dealerCode = "baltavtotreyd_m_moskva_bmw"
        )
        delegateDispatchers(
            GetOfferDispatcher.getOffer("cars", "1084250931-f8070529"),
            GetPhonesDispatcher.onePhone("1084250931-f8070529")
        )
        postSearchOffers()
        postSavedSearch(VehicleCategory.CARS, "saved_search/group_saved_search.json")
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
            collapseAppBar()
            scrollToRequestCall()
        }
    }

    @Test
    fun shouldSeeDealerSubscribe() {
        checkScreenshotOfferCard {
            isSubscribeDealerScreenshotTheSame()
        }
        performOfferCard {
            clickOnDealerDisplayed()
        }
        checkScreenshotOfferCard {
            isSubscribedDealerScreenshotTheSame()
        }
    }

    @Test
    fun shouldSeeDealersOffersCount() {
        checkOfferCard {
            checkDealerOfferCount(324)
        }
        performOfferCard {
            clickDealerOfferCount(324)
        }
        checkScreenshotOfferCard {
            checkDealerCardOpened()
        }
    }
}
