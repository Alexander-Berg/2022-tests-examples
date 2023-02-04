package ru.auto.ara.test.listing.snippet.footer

import android.Manifest
import androidx.test.espresso.intent.Intents
import androidx.test.ext.junit.runners.AndroidJUnit4
import ru.auto.ara.core.rules.GrantPermissionsRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.behaviour.checkInitialApp2AppOutgoingCallIsDisplayingCorrectly
import ru.auto.ara.core.behaviour.disableApp2AppInstantCalling
import ru.auto.ara.core.behaviour.enableApp2AppInstantCalling
import ru.auto.ara.core.di.module.TestMainModuleArguments
import ru.auto.ara.core.dispatchers.device.ParseDeeplinkDispatcher
import ru.auto.ara.core.dispatchers.phones.GetPhonesDispatcher
import ru.auto.ara.core.dispatchers.phones.PhonesResponse
import ru.auto.ara.core.dispatchers.phones.getPhones
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.experimentsOf
import ru.auto.ara.core.robot.checkCommon
import ru.auto.ara.core.robot.performCommon
import ru.auto.ara.core.robot.searchfeed.performListingOffers
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(AndroidJUnit4::class)
class CommercialUsedTest {
    private val DEFAULT_LISTING_DEEPLINK = "https://auto.ru/cars/all"

    private val extendedOfferOfLoyalDealerWithMetroDispatcher = PostSearchOffersDispatcher("extended_commercial_used")
    private val extendedOfferOfNotLoyalDealerDispatcher = PostSearchOffersDispatcher("extended_not_loyal_dealer")
    private val extendedOfferWithDeliveryDispatcher = PostSearchOffersDispatcher("extended_with_delivery")
    private val commonOfferOfLoyalDealerWithMetroDispatcher = PostSearchOffersDispatcher("common_used_loyal_with_metro")
    private val commonOfferOfNotLoyalDealerDispatcher = PostSearchOffersDispatcher("common_used_not_loyal_not_metro")
    private val commonOfferWithDeliveryDispatcher = PostSearchOffersDispatcher("common_with_delivery")

    private val webServerRule = WebServerRule {
        delegateDispatchers(
            CountDispatcher(category = "cars"),
            GetPhonesDispatcher.onePhone(offerId = "1089911274-2aa4a58e"),
            ParseDeeplinkDispatcher.carsAll()
        )
    }
    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    private val experiments = experimentsOf()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityTestRule,
        GrantPermissionsRule.grant(Manifest.permission.RECORD_AUDIO, Manifest.permission.SYSTEM_ALERT_WINDOW),
        arguments = TestMainModuleArguments(
            experiments
        )
    )

    @Before
    fun setUp() {
        experiments.disableApp2AppInstantCalling()
    }

    @Test
    fun shouldSeeLoyalDealerContactsWithMetroOnExtendedOffer() {
        webServerRule.routing { delegateDispatcher(extendedOfferOfLoyalDealerWithMetroDispatcher) }
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers { scrollToFilterPromo() }
            .checkResult {
                isSellerContactsDisplayed("Тойота Центр Внуково\nОфициальный дилер")
                isCallButtonDisplayed()
                isLoyalDealerIconDisplayed()
                isFooterWithAddressWithMetroDisplayed("Саларьево", "#CC0000")
                isUncheckedFavIconDisplayed()
            }
    }

    @Test
    fun shouldSeeNotLoyalDealerContactsWithMetroOnExtendedOffer() {
        webServerRule.routing { delegateDispatcher(extendedOfferOfNotLoyalDealerDispatcher) }
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers { scrollToFilterPromo() }
            .checkResult {
                isSellerContactsDisplayed("Грант Авто\nДилер")
                isCallButtonDisplayed()
                isNotLoyalDealerIconDisplayed()
                isFooterWithAddressDisplayed("Чебоксары")
                isUncheckedFavIconDisplayed()
            }
    }

    @Test
    fun shouldSeeDeliveryInfoOnExtendedOffer() {
        webServerRule.routing { delegateDispatcher(extendedOfferWithDeliveryDispatcher) }
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers { scrollToFilterPromo() }
            .checkResult {
                isSellerContactsDisplayed("Ауди Центр Юг\nДилер")
                isCallButtonDisplayed()
                isFooterWithAddressDisplayed("Доставка из Москвы")
                isDeliveryInfoIconDisplayed()
                isUncheckedFavIconDisplayed()
            }
    }

    @Test
    fun shouldSeeLoyalDealerContactsWithMetroOnCommonOffer() {
        webServerRule.routing { delegateDispatcher(commonOfferOfLoyalDealerWithMetroDispatcher) }
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers { scrollToFilterPromo() }
            .checkResult {
                isSellerContactsNotDisplayed()
                isCallButtonNotDisplayed()
                isLoyalDealerIconDisplayed()
                isFooterWithAddressWithMetroDisplayed("Лубянка", "#CC0000")
                isUncheckedFavIconDisplayed()
            }
    }

    @Test
    fun shouldSeeNotLoyalDealerContactsWithMetroOnCommonOffer() {
        webServerRule.routing { delegateDispatcher(commonOfferOfNotLoyalDealerDispatcher) }
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers { scrollToFilterPromo() }
            .checkResult {
                isSellerContactsNotDisplayed()
                isCallButtonNotDisplayed()
                isNotLoyalDealerIconDisplayed()
                isFooterWithAddressDisplayed("Москва")
                isUncheckedFavIconDisplayed()
            }
    }

    @Test
    fun shouldSeeDeliveryInfoOnCommonOffer() {
        webServerRule.routing { delegateDispatcher(commonOfferWithDeliveryDispatcher) }
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers { scrollToFilterPromo() }
            .checkResult {
                isSellerContactsNotDisplayed()
                isCallButtonNotDisplayed()
                isFooterWithAddressDisplayed("Доставка из Москвы")
                isDeliveryInfoIconDisplayed()
                isUncheckedFavIconDisplayed()
            }
    }

    @Test
    fun shouldMakeCallFromSnippet() {
        webServerRule.routing { delegateDispatcher(extendedOfferWithDeliveryDispatcher) }
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        Intents.init()
        performListingOffers {
            scrollToFilterPromo()
            interactions.onCallOrChatButton().waitUntilIsCompletelyDisplayed().performClick()
        }
        checkCommon { isActionDialIntentCalled("+7 985 440-66-27") }
    }

    @Test
    fun shouldCellCallNotApp2AppFromSnippet() {
        webServerRule.routing {
            userSetup()
            delegateDispatcher(extendedOfferWithDeliveryDispatcher)
            getPhones(PhonesResponse.ONE_WITH_APP2APP)
        }
        performCommon { login() }
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        Intents.init()
        performListingOffers {
            scrollToFilterPromo()
            interactions.onCallOrChatButton().waitUntilIsCompletelyDisplayed().performClick()
        }
        checkCommon { isActionDialIntentCalled("+7 916 039-40-24") }
    }

    @Test
    fun shouldCallByApp2AppInstantlyFromSnippet() {
        experiments.enableApp2AppInstantCalling()
        webServerRule.routing {
            userSetup()
            delegateDispatcher(extendedOfferWithDeliveryDispatcher)
            getPhones(PhonesResponse.ONE_WITH_APP2APP)
        }
        performCommon { login() }
        activityTestRule.launchDeepLinkActivity(DEFAULT_LISTING_DEEPLINK)
        performSearchFeed { waitFirstPageLoaded(1) }
        performListingOffers {
            scrollToFilterPromo()
            interactions.onCallOrChatButton().waitUntilIsCompletelyDisplayed().performClick()
        }
        checkInitialApp2AppOutgoingCallIsDisplayingCorrectly()
    }
}
