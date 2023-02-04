package ru.auto.ara.test.listing.specials

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchSpecialsDispatcher
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.searchfeed.checkSearchFeed
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.screenbundles.SearchFeedFragmentBundles
import ru.auto.ara.core.utils.launchFragment
import ru.auto.ara.core.utils.pressBack
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.ui.activity.SearchFeedActivity
import ru.auto.ara.ui.fragment.feed.SearchFeedFragment
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.filter.StateGroup
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class BlockSpecialsTest {

    private val activityRule = lazyActivityScenarioRule<SearchFeedActivity>()

    private val getSearchCountDispatcherHolder = DispatcherHolder()
    private val postSearchOffersDispatcherHolder = DispatcherHolder()
    private val postSearchSpecialsDispatcherHolder = DispatcherHolder()
    private val getOfferDispatcherHolder = DispatcherHolder()
    private val dispatchers: List<DelegateDispatcher> = listOf(
        getSearchCountDispatcherHolder,
        postSearchOffersDispatcherHolder,
        postSearchSpecialsDispatcherHolder,
        getOfferDispatcherHolder
    )

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        DisableAdsRule(),
        SetPreferencesRule()
    )

    @Test
    fun shouldSeeSpecialsLookCorrect() {
        getSearchCountDispatcherHolder.innerDispatcher = CountDispatcher("trucks")
        postSearchOffersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher.getNewCarsTrucksMotoOffers()
        postSearchSpecialsDispatcherHolder.innerDispatcher = PostSearchSpecialsDispatcher.getTrucksOffers()

        openSearchFeed(StateGroup.NEW, VehicleCategory.TRUCKS)

        performSearchFeed {
            waitFirstPage()
            scrollToPotentialPositionOfOddSpecials()
        }

        checkSearchFeed {
            isSpecialsLookCorrect(
                index = 0,
                title = "2 280 000 \u20BD",
                subtitle = "Ford Transit, 2019",
                badge = "Новый"
            )
            isSpecialsLookCorrect(
                index = 1,
                title = "3 187 600 \u20BD",
                subtitle = "Volkswagen Multivan, 2019",
                badge = "Новый"
            )
        }
    }

    @Test
    fun shouldSeeSpecialsAtPositionOnOddPage() {
        getSearchCountDispatcherHolder.innerDispatcher = CountDispatcher("cars")
        postSearchOffersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher.getNewCarsTrucksMotoOffers()
        postSearchSpecialsDispatcherHolder.innerDispatcher = PostSearchSpecialsDispatcher.getLadaPresetOffers()

        openSearchFeed(StateGroup.ALL, VehicleCategory.CARS)

        performSearchFeed {
            waitFirstPage()
            scrollToPotentialPositionOfOddSpecials()
        }

        checkSearchFeed {
            isSpecialsDisplayed()
        }
    }

    @Test
    fun shouldSeeSpecialsAtPositionOnEvenPage() {
        getSearchCountDispatcherHolder.innerDispatcher = CountDispatcher("cars")
        postSearchOffersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher.getLadaPresetOffers()
        postSearchSpecialsDispatcherHolder.innerDispatcher = PostSearchSpecialsDispatcher.getLadaPresetOffers()

        openSearchFeed(StateGroup.ALL, VehicleCategory.CARS)

        performSearchFeed {
            waitFirstPage()
            scrollToEnd()
            waitSecondPageLoadedWithSpecials()
            scrollToPotentialPositionOfEvenSpecials()
        }

        checkSearchFeed {
            isSpecialsDisplayed()
        }
    }

    @Test
    fun shouldNavigateToOfferCardWithCorrectTitleAndSeeSpecialsWhenReturns() {
        getSearchCountDispatcherHolder.innerDispatcher = CountDispatcher("trucks")
        postSearchOffersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher.getNewCarsTrucksMotoOffers()
        postSearchSpecialsDispatcherHolder.innerDispatcher = PostSearchSpecialsDispatcher.getTrucksOffers()
        getOfferDispatcherHolder.innerDispatcher = GetOfferDispatcher.getOffer("trucks", "10563876-29f3b8c5")

        openSearchFeed(StateGroup.NEW, VehicleCategory.TRUCKS)

        performSearchFeed {
            waitFirstPage()
            scrollToPotentialPositionOfOddSpecials()
            clickSpecialOffer()
        }

        checkOfferCard {
            isOfferCardTitle("Ford Transit, 2019")
        }

        pressBack()

        checkSearchFeed {
            isSpecialsDisplayed()
        }
    }

    @Test
    fun shouldSeeSpecialsInNewTrucksListing() {
        getSearchCountDispatcherHolder.innerDispatcher = CountDispatcher("trucks")
        postSearchOffersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher.getNewCarsTrucksMotoOffers()
        postSearchSpecialsDispatcherHolder.innerDispatcher = PostSearchSpecialsDispatcher.getTrucksOffers()

        shouldSeeSpecialsInListing(StateGroup.NEW, VehicleCategory.TRUCKS)
    }

    @Test
    fun shouldSeeSpecialsInNewMotoListing() {
        getSearchCountDispatcherHolder.innerDispatcher = CountDispatcher("moto")
        postSearchOffersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher.getNewCarsTrucksMotoOffers()
        postSearchSpecialsDispatcherHolder.innerDispatcher = PostSearchSpecialsDispatcher.getMotoOffers()

        shouldSeeSpecialsInListing(StateGroup.NEW, VehicleCategory.MOTO)
    }

    @Test
    fun shouldSeeSpecialsInUsedCarsListing() {
        getSearchCountDispatcherHolder.innerDispatcher = CountDispatcher("cars")
        postSearchOffersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher.getUsedCarsTrucksMotoOffers()
        postSearchSpecialsDispatcherHolder.innerDispatcher = PostSearchSpecialsDispatcher.getLadaPresetOffers()

        shouldSeeSpecialsInListing(StateGroup.USED, VehicleCategory.CARS)
    }

    @Test
    fun shouldSeeSpecialsInUsedTrucksListing() {
        getSearchCountDispatcherHolder.innerDispatcher = CountDispatcher("trucks")
        postSearchOffersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher.getUsedCarsTrucksMotoOffers()
        postSearchSpecialsDispatcherHolder.innerDispatcher = PostSearchSpecialsDispatcher.getTrucksOffers()

        shouldSeeSpecialsInListing(StateGroup.USED, VehicleCategory.TRUCKS)
    }

    @Test
    fun shouldSeeSpecialsInUsedMotoListing() {
        getSearchCountDispatcherHolder.innerDispatcher = CountDispatcher("moto")
        postSearchOffersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher.getUsedCarsTrucksMotoOffers()
        postSearchSpecialsDispatcherHolder.innerDispatcher = PostSearchSpecialsDispatcher.getMotoOffers()

        shouldSeeSpecialsInListing(StateGroup.USED, VehicleCategory.MOTO)
    }

    @Test
    fun shouldNotSeeSpecialsInNewCarsListing() {
        getSearchCountDispatcherHolder.innerDispatcher = CountDispatcher("cars")
        postSearchOffersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher.getNewCarsTrucksMotoOffers()
        postSearchSpecialsDispatcherHolder.innerDispatcher = PostSearchSpecialsDispatcher.getLadaPresetOffers()

        openSearchFeed(StateGroup.NEW, VehicleCategory.CARS)

        performSearchFeed {
            waitFirstPage()
            scrollToPotentialPositionOfOddSpecials()
        }

        checkSearchFeed {
            isSpecialsAbsent()
        }
    }

    @Test
    fun shouldSeeDayOffersLookCorrect() {
        getSearchCountDispatcherHolder.innerDispatcher = CountDispatcher("cars")
        postSearchOffersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher.getNewCarsTrucksMotoOffers()
        postSearchSpecialsDispatcherHolder.innerDispatcher = PostSearchSpecialsDispatcher.getLadaPresetOffers()

        openSearchFeed(StateGroup.NEW, VehicleCategory.CARS)

        performSearchFeed {
            waitFirstPage()
            scrollToPotentialPositionOfOddSpecials()
        }

        checkSearchFeed {
            isDayOffersLookCorrect(
                index = 0,
                title = "Mitsubishi Outlander III",
                price = "820 000 \u20BD",
                subtitle = "2.4 CVT (167 л.с.) 4WD • Индивидуальная"
            )
            isDayOffersLookCorrect(
                index = 1,
                title = "Toyota Camry VII (XV50) Рестайлинг",
                price = "1 450 000 \u20BD",
                subtitle = "3.5 AT (249 л.с.) • Люкс"
            )
        }
    }

    @Test
    fun shouldSeeDayOffersAtPositionOnOddPage() {
        getSearchCountDispatcherHolder.innerDispatcher = CountDispatcher("cars")
        postSearchOffersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher.getNewCarsTrucksMotoOffers()
        postSearchSpecialsDispatcherHolder.innerDispatcher = PostSearchSpecialsDispatcher.getLadaPresetOffers()

        openSearchFeed(StateGroup.NEW, VehicleCategory.CARS)

        performSearchFeed {
            waitFirstPage()
            scrollToPotentialPositionOfOddSpecials()
        }

        checkSearchFeed {
            isDayOffersDisplayed()
        }
    }

    @Test
    fun shouldSeeDayOffersAtPositionOnEvenPage() {
        getSearchCountDispatcherHolder.innerDispatcher = CountDispatcher("cars")
        postSearchOffersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher.getLadaPresetOffers()
        postSearchSpecialsDispatcherHolder.innerDispatcher = PostSearchSpecialsDispatcher.getLadaPresetOffers()

        openSearchFeed(StateGroup.NEW, VehicleCategory.CARS)

        performSearchFeed {
            waitFirstPage()
            scrollToEnd()
            waitSecondPageLoadedWithDayOffers()
            scrollToPotentialPositionOfEvenSpecials()
        }

        checkSearchFeed {
            isDayOffersDisplayed()
        }
    }

    @Test
    fun shouldNavigateToOfferCardWithCorrectTitleAndSeeDayOffersWhenReturns() {
        getSearchCountDispatcherHolder.innerDispatcher = CountDispatcher("cars")
        postSearchOffersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher.getNewCarsTrucksMotoOffers()
        postSearchSpecialsDispatcherHolder.innerDispatcher = PostSearchSpecialsDispatcher.getLadaPresetOffers()
        getOfferDispatcherHolder.innerDispatcher = GetOfferDispatcher.getOffer("cars", "1092467068-bdba09a9")

        openSearchFeed(StateGroup.NEW, VehicleCategory.CARS)

        performSearchFeed {
            waitFirstPage()
            scrollToPotentialPositionOfOddSpecials()
            waitSomething(3, TimeUnit.SECONDS)
            clickDayOffer()
        }

        checkOfferCard {
            isOfferCardTitle("Mitsubishi Outlander III, 2012")
        }

        pressBack()

        checkSearchFeed {
            isDayOffersDisplayed()
        }
    }

    private fun openSearchFeed(stateGroup: StateGroup, category: VehicleCategory) {
        activityRule.launchFragment<SearchFeedFragment>(
            SearchFeedFragmentBundles.searchFeedBundle(
                category = category,
                stateGroup = stateGroup
            )
        )
    }

    private fun shouldSeeSpecialsInListing(stateGroup: StateGroup, category: VehicleCategory) {
        openSearchFeed(stateGroup, category)

        performSearchFeed {
            waitFirstPage()
            scrollToPotentialPositionOfOddSpecials()
        }

        checkSearchFeed {
            isSpecialsDisplayed()
        }
    }
}
