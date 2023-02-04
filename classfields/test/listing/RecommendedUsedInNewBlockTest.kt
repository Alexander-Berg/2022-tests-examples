package ru.auto.ara.test.listing

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.BodyNode.Companion.asArray
import ru.auto.ara.core.dispatchers.BodyNode.Companion.asObject
import ru.auto.ara.core.dispatchers.BodyNode.Companion.asValue
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.dispatchers.search_offers.getOfferCount
import ru.auto.ara.core.dispatchers.search_offers.postSearchOffersGroup
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.searchfeed.checkFilter
import ru.auto.ara.core.robot.searchfeed.checkSearchFeed
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.routing.BodyNodeMatcherDefinition
import ru.auto.ara.core.routing.Routing
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.screenbundles.SearchFeedFragmentBundles
import ru.auto.ara.core.utils.checkCatalogFilter
import ru.auto.ara.core.utils.launchFragment
import ru.auto.ara.ui.activity.SearchFeedActivity
import ru.auto.ara.ui.fragment.feed.SearchFeedFragment
import ru.auto.data.model.filter.CatalogFilter
import ru.auto.data.model.filter.GroupBy
import ru.auto.data.model.filter.StateGroup

@RunWith(AndroidJUnit4::class)
class RecommendedUsedInNewBlockTest {
    private val activityRule = lazyActivityScenarioRule<SearchFeedActivity>()

    private val webServerRule = WebServerRule {
        postSearchOffersGroup("group/empty.json", GroupBy.CONFIGURATION)
        getOffer("1087439802-b6940925")
        stub { getOfferCount() }
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        SetPreferencesRule(),
        SetupTimeRule(date = "01.03.2020")
    )

    @Test
    fun shouldSeeUsedInNewBlock() {
        webServerRule.routing {
             stub {
                 delegateDispatcher(PostSearchOffersDispatcher.getGenericFeed()).watch {
                     checkQueryParameters(listOf("page" to "1", "page_size" to "8", "sort" to "km_age-asc"))
                     checkRequestBodyParameters("year_from" to "2019", "year_to" to "2020")
                     checkCatalogFilter(CatalogFilter(mark = MARK))
                 }
             }
        }
        openBMWCarsSearchFeed(StateGroup.NEW)
        performSearchFeed {
            scrollDownOneScreen() //to hide FAB
            scrollToRecommendedUsedInNewBlock(MARK, YEARS)
        }.checkResult {
            isRecommendedUsedInNewLookCorrect(
                index = 0,
                title = "BMW X5",
                price = "1 000 000 \u20BD",
                techParams = "2010, 150,000 км",
                markName = MARK,
                years = YEARS
            )
        }
    }

    @Test
    fun shouldOpenUsedFeedByTapShowOffersButton() {
        webServerRule.routing {
            stub {
                delegateDispatcher(PostSearchOffersDispatcher.getGenericFeed()).watch {
                    checkQueryParameters("page" to "1", "page_size" to "20", "sort" to "year-desc")
                    checkCatalogFilter(CatalogFilter(mark = MARK))
                }
            }
        }
        openBMWCarsSearchFeed(StateGroup.NEW)
        performSearchFeed {
            scrollToRecommendedUsedInNewBlock(MARK, YEARS)
            scrollDownOneScreen() //to hide FAB
            clickShowOffersButton()
        }.checkResult {
            isStateSelectorChecked(StateGroup.USED)
        }
    }

    @Test
    fun shouldOpenOfferFromSnippet() {
        webServerRule.routing { stub { delegateDispatcher(PostSearchOffersDispatcher.getGenericFeed()) } }
        openBMWCarsSearchFeed(StateGroup.NEW)
        performSearchFeed {
            scrollDownOneScreen() //to hide FAB
            scrollToRecommendedUsedInNewBlock(MARK, YEARS)
            clickRecommendedUnedInNewSnippetWithIndex(0, MARK, YEARS)
        }
        checkOfferCard {
            interactions.onCardTitle().waitUntilIsCompletelyDisplayed()
        }
    }

    @Test
    fun shouldNotSeeBlockInAllListing() {
        webServerRule.routing { stub { delegateDispatcher(PostSearchOffersDispatcher.getEmptyAllBMWOffers()) } }
        openBMWCarsSearchFeed(StateGroup.ALL)
        checkSearchFeed {
            isRecommendedUsedInNewAbsent(YEARS)
        }
    }

    @Test
    fun shouldNotSeeBlockInUsedListing() {
        webServerRule.routing { delegateDispatcher(PostSearchOffersDispatcher.getEmptyUsedBMWOffers()) }
        openBMWCarsSearchFeed(StateGroup.USED)
        checkSearchFeed {
            isRecommendedUsedInNewAbsent(YEARS)
        }
    }

    @Test
    fun shouldSeeShowAdvsButtonWhenNewEmptyUsedNotEmptyFromAllParameters() {
        openBMWCarsSearchFeed(StateGroup.NEW)
        webServerRule.routing {
            getNewOfferCount()
            getOldOfferCount()
        }
        performSearchFeed {
            openParameters()
        }
        checkFilter {
            isDoSearchButtonWithTextResource(R.string.show_advs)
        }
    }

    @Test
    fun shouldSeeShowAdvsButtonWhenNewEmptyUsedNotEmptyFromMarkPicker() {
        openBMWCarsSearchFeed(StateGroup.NEW)
        webServerRule.routing {
            getNewOfferCount()
            getOldOfferCount()
            delegateDispatcher(PostSearchOffersDispatcher.getEmptyUsedBMWOffers())
        }
        performSearchFeed {
            clickMarkFieldUntilFiltersOpened(R.string.show_advs)
        }
    }

    @Test
    fun shouldSeeShowZeroAdvsButtonWhenNewEmptyUsedEmptyFromAllParameters() {
        openBMWCarsSearchFeed(StateGroup.NEW)
        webServerRule.routing { getOfferCount("filters/count_2019-2020_BMW_empty.json") }
        performSearchFeed {
            openParameters()
        }
        checkFilter {
            isDoSearchButtonWithTextResource(R.string.show_zero_offers)
        }
    }

    @Test
    fun shouldSeeShowZeroAdvsButtonWhenNewEmptyUsedEmptyFromModelPicker() {
        openBMWCarsSearchFeed(StateGroup.NEW)
        webServerRule.routing {
            getOfferCount("filters/count_2019-2020_BMW_empty.json")
            delegateDispatcher(PostSearchOffersDispatcher.getEmptyUsedBMWOffers())
        }
        performSearchFeed {
            clickModelFieldUntilFiltersOpened(R.string.show_zero_offers)
        }
    }

    private fun openBMWCarsSearchFeed(stateGroup: StateGroup) {
        activityRule.launchFragment<SearchFeedFragment>(
            SearchFeedFragmentBundles.searchFeedBMWBundle(stateGroup)
        )
        performSearchFeed { waitSearchFeed() }
    }

    private fun Routing.getNewOfferCount() =
        getOfferCount(
            assetPath = "filters/count_empty_new_BMW.json",
            bodyMatcher = isSearchRequestParamsNewBmw()
        )

    private fun Routing.getOldOfferCount() =
        getOfferCount(
            assetPath = "filters/count_2019-2020_BMW_not_empty.json",
            bodyMatcher = isSearchRequestParamsUsedBmwFrom2019to2020()
        )

    private fun isSearchRequestParamsNewBmw(): BodyNodeMatcherDefinition = {
        asObject {
            get("state_group").asValue() == "NEW"
                && get("catalog_filter").asArray { first().asObject { get("mark").asValue() == "BMW" } }
        }
    }

    private fun isSearchRequestParamsUsedBmwFrom2019to2020(): BodyNodeMatcherDefinition = {
        asObject {
            get("state_group").asValue() == "USED"
                && get("catalog_filter").asArray { first().asObject { get("mark").asValue() == "BMW" } }
                && get("year_from").asValue() == "2019"
                && get("year_to").asValue() == "2020"
        }
    }

    companion object {
        private const val YEARS = "2019-2020"
        private const val MARK = "BMW"
    }
}
