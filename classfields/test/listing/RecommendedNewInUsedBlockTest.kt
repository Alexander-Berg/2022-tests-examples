package ru.auto.ara.test.listing

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.catalog.getCatalogSubtree
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersGroupDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchRecommendNewInStockDispatcher
import ru.auto.ara.core.robot.offercard.checkGroupOfferCard
import ru.auto.ara.core.robot.searchfeed.checkSearchFeed
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.screenbundles.SearchFeedFragmentBundles
import ru.auto.ara.core.utils.checkCatalogFilter
import ru.auto.ara.core.utils.launchFragment
import ru.auto.ara.core.utils.pressBack
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.ui.activity.SearchFeedActivity
import ru.auto.ara.ui.fragment.feed.SearchFeedFragment
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.filter.CatalogFilter
import ru.auto.data.model.filter.GroupBy
import ru.auto.data.model.filter.StateGroup
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class RecommendedNewInUsedBlockTest {

    private val postSearchOffersWatcher = RequestWatcher()
    private val postGroupConfigurationWatcher = RequestWatcher()
    private val postGroupComplectationNameWatcher = RequestWatcher()

    private val postGroupConfigurationDispatcher = PostSearchOffersGroupDispatcher(
        "group/audi_Q5_configuration.json",
        setOf(GroupBy.CONFIGURATION),
        postGroupConfigurationWatcher
    )

    private val postGroupComplectationNameDispatcher = PostSearchOffersGroupDispatcher(
        "group/audi_Q5_complectation_name.json",
        setOf(GroupBy.COMPLECTATION_NAME),
        postGroupComplectationNameWatcher
    )
    private val activityRule = lazyActivityScenarioRule<SearchFeedActivity>()

    private val webServerRule = WebServerRule {
        delegateDispatchers(
            CountDispatcher(category = "cars"),
            PostSearchOffersDispatcher.getLadaPresetOffers(postSearchOffersWatcher)
        )
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        SetPreferencesRule()
    )

    @Test
    fun shouldSeeRecommendLookCorrect() {
        openCarsSearchFeed(StateGroup.ALL)
        performScrollToRecommendedNewCars(PostSearchRecommendNewInStockDispatcher.getTenOffers())
        checkSearchFeed {
            isRecommendedNewInUsedLookCorrect(
                index = 0,
                title = "Mitsubishi Pajero Sport",
                price = "от 1 576 000 \u20BD",
                techParams = "2.4d MT (181 л.с.) 4WD • Invite",
                availability = "132 предложения"
            )
        }
    }

    @Test
    fun shouldSeeRecommendedInAllCarsListing() {
        openCarsSearchFeed(StateGroup.ALL)
        performScrollToRecommendedNewCars(PostSearchRecommendNewInStockDispatcher.getTenOffers())
        checkSearchFeed {
            isRecommendedNewInUsedDisplayed()
        }
    }

    @Test
    fun shouldSeeRecommendedInUsedCarsListing() {
        openCarsSearchFeed(StateGroup.USED)
        performScrollToRecommendedNewCars(PostSearchRecommendNewInStockDispatcher.getTenOffers())
        checkSearchFeed {
            isRecommendedNewInUsedDisplayed()
        }
    }

    @Test
    fun shouldNotSeeRecommendedInNewCarsListing() {
        openCarsSearchFeed(StateGroup.NEW)
        performScrollToRecommendedNewCars(PostSearchRecommendNewInStockDispatcher.getTenOffers())
        checkSearchFeed {
            isRecommendedNewInUsedAbsent()
        }
    }

    @Test
    fun shouldOpenRecommendedGroupCard() {
        openCarsSearchFeed(StateGroup.ALL)
        webServerRule.routing {
            delegateDispatchers(
                postGroupConfigurationDispatcher,
                postGroupComplectationNameDispatcher
            )
            getCatalogSubtree("audi_catalog")
        }
        performScrollToRecommendedNewCars(PostSearchRecommendNewInStockDispatcher.getNineOffers())
        performSearchFeed {
            waitSomething(2, TimeUnit.SECONDS)
            clickRecommendedNewInStockSnippetWithIndex(0)
        }
        checkGroupOfferCard {
            isOfferCardTitle("Audi Q5 II (FY)")
            postGroupComplectationNameWatcher.checkCatalogFilter(complectationNameCatalogFilter)
            postGroupComplectationNameWatcher.checkRequestBodyParameters(*firstSnippetBodyParameters)
            postGroupComplectationNameWatcher.checkRequestBodyExactlyArrayParameter(
                "cars_params.gear_type", setOf("ALL_WHEEL_DRIVE")
            )
            postGroupComplectationNameWatcher.checkRequestBodyExactlyArrayParameter(
                "cars_params.transmission", setOf("ROBOT")
            )
            postGroupConfigurationWatcher.checkCatalogFilter(configurationCatalogFilter)
            postGroupConfigurationWatcher.checkRequestBodyParameters(*firstSnippetBodyParameters)
            postGroupConfigurationWatcher.checkNotRequestBodyParameters(
                listOf(
                    "cars_params.gear_type",
                    "cars_params.transmission"
                )
            )
        }
    }

    @Test
    fun shouldReturnOnListingFromGroupCard() {
        openCarsSearchFeed(StateGroup.ALL)
        webServerRule.routing {
            delegateDispatchers(
                postGroupConfigurationDispatcher,
                postGroupComplectationNameDispatcher
            )
            getCatalogSubtree("audi_catalog")
        }
        performScrollToRecommendedNewCars(PostSearchRecommendNewInStockDispatcher.getNineOffers())
        performSearchFeed {
            waitSomething(2, TimeUnit.SECONDS)
            clickRecommendedNewInStockSnippetWithIndex(0)
        }
        checkGroupOfferCard {
            isOfferCardTitle("Audi Q5 II (FY)")
        }
        pressBack()
        checkSearchFeed {
            isRecommendedNewInUsedDisplayed()
        }

    }

    @Test
    fun shouldOpenFeedAfterTapShowAll() {
        openCarsSearchFeed(StateGroup.ALL)
        performScrollToRecommendedNewCars(PostSearchRecommendNewInStockDispatcher.getTenOffers())
        checkSearchFeed {
            isRecommendedNewInUsedDisplayed()
            isRecommendedBlockHasCorrectnItemsCount(11)
        }
        performSearchFeed {
            waitSomething(2, TimeUnit.SECONDS)
            scrollRecommendBlockToShowAllButton()
            clickShowAllButton()
        }.checkResult {
            isStateSelectorChecked(StateGroup.NEW)
        }
        postSearchOffersWatcher.checkCatalogFilter()
        postSearchOffersWatcher.checkRequestBodyParameters(*showAllButtonBodyParameters)
    }

    @Test
    fun shouldNotSeeShowAllButtonWhenLessThenTenOffers() {
        openCarsSearchFeed(StateGroup.ALL)
        performScrollToRecommendedNewCars(PostSearchRecommendNewInStockDispatcher.getNineOffers())
        checkSearchFeed {
            isRecommendedNewInUsedDisplayed()
            isRecommendedBlockHasCorrectnItemsCount(9)
        }
        performSearchFeed {
            scrollRecommendedNewInStockBlockToEnd()
        }.checkResult {
            isShowAllButtonGone()
        }
    }

    private fun performScrollToRecommendedNewCars(dispatcherOfNewInStock: PostSearchRecommendNewInStockDispatcher) {
        webServerRule.routing { delegateDispatcher(dispatcherOfNewInStock) }
        performSearchFeed {
            waitFirstPage()
            scrollToEnd()
            waitSecondPageLoaded()
            scrollToPotentialRecommendedNewCars()
        }
    }

    private fun openCarsSearchFeed(stateGroup: StateGroup) {
        activityRule.launchFragment<SearchFeedFragment>(
            SearchFeedFragmentBundles.searchFeedBundle(
                category = VehicleCategory.CARS,
                stateGroup = stateGroup
            )
        )
    }

    companion object {
        private val configurationCatalogFilter = CatalogFilter(
            mark = "AUDI",
            model = "Q5",
            configuration = 20849260,
            generation = 20849216
        )

        private val complectationNameCatalogFilter = CatalogFilter(
            mark = "AUDI",
            model = "Q5",
            configuration = 20849260,
            generation = 20849216,
            techParam = 20990370
        )

        private val showAllButtonBodyParameters = arrayOf(
            "state_group" to "NEW",
            "price_from" to "200000",
            "price_to" to "780000"
        )

        private val firstSnippetBodyParameters = arrayOf(
            "state_group" to "NEW",
            "price_from" to "200000",
            "price_to" to "3250000"
        )
    }

}
