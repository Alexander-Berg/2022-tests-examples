package ru.auto.ara.test.listing

import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.actions.scroll.scrollToPosition
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.newcars.PostPremiumNewCarsDispatcher
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.dispatchers.phones.GetPhonesDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.dispatchers.search_offers.postSearchOffers
import ru.auto.ara.core.robot.checkCommon
import ru.auto.ara.core.robot.dealer.checkDealerContacts
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.ratecall.checkRateCall
import ru.auto.ara.core.robot.searchfeed.checkDealsOfTheDay
import ru.auto.ara.core.robot.searchfeed.performDealsOfTheDay
import ru.auto.ara.core.robot.searchfeed.performListingOffers
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DecreaseRateCallTimeRule
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.screenbundles.SearchFeedFragmentBundles.searchFeedBundle
import ru.auto.ara.core.utils.getResourceString
import ru.auto.ara.core.utils.intendingNotInternal
import ru.auto.ara.core.utils.launchFragment
import ru.auto.ara.core.utils.pressBack
import ru.auto.ara.core.utils.respondWithOk
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.core.utils.withIntents
import ru.auto.ara.ui.activity.SearchFeedActivity
import ru.auto.ara.ui.fragment.feed.SearchFeedFragment
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.filter.StateGroup
import ru.auto.data.util.RUB_UNICODE
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class NewCarsOfferOfTheDayTest {
    private val requestWatcher = RequestWatcher()
    private val webServerRule = WebServerRule {
        delegateDispatchers(
            PostPremiumNewCarsDispatcher(requestWatcher),
            PostSearchOffersDispatcher.getNewCarsTrucksMotoOffers(),
            GetOfferDispatcher.getOffer(category = "cars", offerId = "10563876-29f3b8c5"),
            GetPhonesDispatcher.onePhone(offerId = "10563876-29f3b8c5", category = "cars")
        )
    }
    private val activityRule = lazyActivityScenarioRule<SearchFeedActivity>()
    @JvmField
    @Rule
    val rules = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityRule,
        SetPreferencesRule(),
        DecreaseRateCallTimeRule()
    )

    @Test
    fun checkFirstSnippetStaticComposition() {
        openSearchFeed()
        checkDealsOfTheDay {
            isSectionTitleDisplayed(R.string.deals_of_the_day)
            isSnippetTitleDisplayed(0, FIRST_CAR_TITLE)
            isPriceDisplayed(0, FIRST_CAR_PRICE)
            isBadgeDisplayed(0, R.string.new_auto)
            isPhotoDisplayed(0)
            areCharacteristicsDisplayed(0, FIRST_CAR_CHARACTERISTICS)
            isDealerDisplayed(0, DEALER_ITEM_TITLE)
            areDealerContactsDisplayed(0, getResourceString(R.string.contacts))
            isCallButtonDisplayed(0)
        }
    }

    @Test
    fun shouldOpenContactsByDealerClick() {
        openSearchFeed()
        waitSomething(2, TimeUnit.SECONDS)
        performDealsOfTheDay {
            interaction.onSnippetDealerContacts(0).waitUntilIsCompletelyDisplayed().performClick()
        }
        checkDealerContacts { checkDealerContactsDialogIsDisplayed() }
    }

    @Test
    fun shouldCallByCallButtonClick() {
        openSearchFeed()
        webServerRule.routing {
            delegateDispatchers(PostSearchOffersDispatcher(fileName = "empty_feed_cars"))
        }
        waitSomething(2, TimeUnit.SECONDS)
        withIntents {
            intendingNotInternal().respondWithOk(delay = 3)
            performDealsOfTheDay {
                interaction.onSnippetCallButton(0).waitUntilIsCompletelyDisplayed().performClick()
            }

            checkCommon { isActionDialIntentCalled(DEALER_PHONE_NUMBER) }
            checkRateCall { isRateCallDialogWithoutCheckupButtonDisplayed(false) }
        }
    }

    @Test
    fun shouldDisableSnippetBySnippetClick() {
        openSearchFeed()
        performDealsOfTheDay { interaction.onSnippet(0).waitUntilIsCompletelyDisplayed().performClick() }
        checkOfferCard { isOfferCardTitle("Krone SD, 2010") }
        pressBack()
        checkDealsOfTheDay {
            snippetIsViewed(0)
        }
    }

    @Test
    fun checkSentDataToServerAndLoadNewPageOnScroll() {
        openSinglePageSearchFeed()
        requestWatcher.checkQueryParameter(PAGE_REQUEST_PARAM, "1")
        requestWatcher.checkQueryParameter(PAGE_SIZE_REQUEST_PARAM, "$PAGE_SIZE")
        performDealsOfTheDay {
            interaction.onDealsRecycler().scrollToPosition(PAGE_SIZE - 1)
            interaction.onDealsRecycler().perform(
                GeneralSwipeAction(
                    Swipe.FAST,
                    GeneralLocation.CENTER,
                    GeneralLocation.CENTER_LEFT,
                    Press.FINGER
                )
            )
        }
        performDealsOfTheDay { interaction.onDealsRecycler().waitUntilAdapterCountMatches(PAGE_SIZE + 1) { true } }
        checkDealsOfTheDay { hasSnippetWithTitleDisplayed(FIRST_CAR_TITLE) }
        requestWatcher.checkQueryParameter(PAGE_REQUEST_PARAM, "2")
        requestWatcher.checkQueryParameter(PAGE_SIZE_REQUEST_PARAM, "$PAGE_SIZE")
    }

    private fun openSearchFeed() {
        activityRule.launchFragment<SearchFeedFragment>(searchFeedBundle(VehicleCategory.CARS, StateGroup.NEW))
        performSearchFeed { waitFirstPage() }
        performListingOffers { scrollToDealsOfTheDay(TOTAL_OFFERS_TITLE) }
    }

    private fun openSinglePageSearchFeed() {
        webServerRule.routing { postSearchOffers { copy(pagination = pagination?.copy(total_page_count = 1)) } }
        activityRule.launchFragment<SearchFeedFragment>(searchFeedBundle(VehicleCategory.CARS, StateGroup.NEW))
        performSearchFeed { waitSearchFeed() }
        performListingOffers { scrollToDealsOfTheDay(TOTAL_OFFERS_TITLE) }
    }

    companion object {
        private const val TOTAL_OFFERS_TITLE = " предложен"
        private const val FIRST_CAR_TITLE = "LADA (ВАЗ) XRAY I"
        private const val FIRST_CAR_PRICE = "от 534 900 $RUB_UNICODE"
        private const val FIRST_CAR_CHARACTERISTICS = "1.6 MT (106 л.с.)\nStandard • 26 опций"
        private const val DEALER_ITEM_TITLE = "Лада-Центр Юго-Запад"
        private const val DEALER_PHONE_NUMBER = "+7 985 440-66-27"

        private const val PAGE_SIZE = 10
        private const val PAGE_REQUEST_PARAM = "page"
        private const val PAGE_SIZE_REQUEST_PARAM = "page_size"
    }
}
