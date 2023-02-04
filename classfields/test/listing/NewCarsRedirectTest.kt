package ru.auto.ara.test.listing

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.actions.step
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.catalog.getCatalogSubtree
import ru.auto.ara.core.dispatchers.search_offers.PostEquipmentFiltersDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.dispatchers.stub.StubGetCatalogAllDictionariesDispatcher
import ru.auto.ara.core.robot.filters.performMark
import ru.auto.ara.core.robot.filters.performMultiGeo
import ru.auto.ara.core.robot.offercard.checkGroupOfferCard
import ru.auto.ara.core.robot.searchfeed.checkSearchFeed
import ru.auto.ara.core.robot.searchfeed.performFeedSort
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.screenbundles.SearchFeedFragmentBundles.searchFeedBundle
import ru.auto.ara.core.utils.getResourceString
import ru.auto.ara.core.utils.launchFragment
import ru.auto.ara.core.utils.pressBack
import ru.auto.ara.ui.activity.SearchFeedActivity
import ru.auto.ara.ui.fragment.feed.SearchFeedFragment
import ru.auto.ara.ui.helpers.form.util.VehicleSearchToFormStateConverter
import ru.auto.data.model.filter.CarParams
import ru.auto.data.model.filter.CarSearch
import ru.auto.data.model.filter.CommonVehicleParams
import ru.auto.data.model.filter.StateGroup
import ru.auto.data.model.search.Mark
import ru.auto.data.model.search.Model

@RunWith(AndroidJUnit4::class)
class NewCarsRedirectTest {
    private val offersDispatcherHolder = DispatcherHolder()
    private val dispatchers = listOf(
        offersDispatcherHolder,
        PostEquipmentFiltersDispatcher(filePath = "filters/equipment_filters_bmw_x3.json"),
        StubGetCatalogAllDictionariesDispatcher
    )

    private val activityRule = lazyActivityScenarioRule<SearchFeedActivity>()

    @JvmField
    @Rule
    val rules: RuleChain = baseRuleChain(
        WebServerRule {
            delegateDispatchers(dispatchers)
            getCatalogSubtree("bmw_catalog2")
        },
        DisableAdsRule(),
        activityRule
    )

    @Test
    fun shouldRedirectedWhenSingleMarkAndModelWithSingleOffer() {
        offersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher(NEW_CARS_FEED_SINGLE_OFFER)
        showSearchFeed(
            listOf(
                Mark(
                    id = "BMW",
                    name = "BMW",
                    models = listOf(Model(id = "X6", name = "X6", nameplates = emptyList(), generations = emptyList()))
                )
            ),
            shouldWaitFeed = false
        )
        checkGroupOfferCard {
            isOfferCardTitle(GROUP_OFFER_TITLE)
        }
    }

    @Test
    fun shouldNotRedirectedWhenSingleMarkWithSingleOffer() {
        offersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher(NEW_CARS_FEED_SINGLE_OFFER)
        showSearchFeed(
            listOf(
                Mark(
                    id = "BMW",
                    name = "BMW"
                )
            )
        )
        checkNewCarsFeedIsOpened()
    }

    @Test
    fun shouldNotRedirectedWhenSingleMarkAndMultipleModelsWithSingleOffer() {
        offersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher(NEW_CARS_FEED_SINGLE_OFFER)
        showSearchFeed(
            listOf(
                Mark(
                    id = "BMW",
                    name = "BMW",
                    models = listOf(
                        Model(id = "X6", name = "X6", nameplates = emptyList(), generations = emptyList()),
                        Model(id = "X5", name = "X5", nameplates = emptyList(), generations = emptyList())
                    )
                )
            )
        )
        checkNewCarsFeedIsOpened()
    }

    @Test
    fun shouldNotRedirectedWhenMultipleMarksAndWithSingleOffer() {
        offersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher(NEW_CARS_FEED_SINGLE_OFFER)
        showSearchFeed(
            listOf(
                Mark(
                    id = "BMW",
                    name = "BMW"
                ),
                Mark(
                    id = "Audi",
                    name = "Audi"
                )
            )
        )
        checkNewCarsFeedIsOpened()
    }

    @Test
    fun shouldNotRedirectingWhenSingleMarkAndModelWithMultipleToSingleOfferAfterRefresh() {
        offersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher(NEW_CARS_FEED)
        showSearchFeed(
            listOf(
                Mark(
                    id = "BMW",
                    name = "BMW",
                    models = listOf(Model(id = "X6", name = "X6", nameplates = emptyList(), generations = emptyList()))
                )
            )
        )
        checkNewCarsFeedIsOpened()

        offersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher(NEW_CARS_FEED_SINGLE_OFFER)
        performSearchFeed {
            refreshFeed()
            waitSearchFeed()
        }
        checkNewCarsFeedIsOpened()
    }

    @Test
    fun shouldNotRedirectingWhenSingleMarkAndModelWithMultipleToSingleOfferAfterTabSwitch() {
        offersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher(NEW_CARS_FEED)
        showSearchFeed(
            listOf(
                Mark(
                    id = "BMW",
                    name = "BMW",
                    models = listOf(Model(id = "X6", name = "X6", nameplates = emptyList(), generations = emptyList()))
                )
            )
        )
        checkNewCarsFeedIsOpened()

        performSearchFeed {
            selectState(StateGroup.USED)
            waitSearchFeed()

            offersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher(NEW_CARS_FEED_SINGLE_OFFER)
            selectState(StateGroup.NEW)
            waitSearchFeed()
        }
        checkNewCarsFeedIsOpened()
    }

    @Test
    fun shouldSingleRedirectWhenSingleMarkAndModelWithSingleToSingleOfferAfterRefresh() {
        offersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher(NEW_CARS_FEED_SINGLE_OFFER)
        showSearchFeed(
            listOf(
                Mark(
                    id = "BMW",
                    name = "BMW",
                    models = listOf(Model(id = "X6", name = "X6", nameplates = emptyList(), generations = emptyList()))
                )
            ),
            shouldWaitFeed = false
        )
        checkGroupOfferCard {
            isOfferCardTitle(GROUP_OFFER_TITLE)
        }
        performSearchFeed {
            pressBack()
            refreshFeed()
            waitSearchFeed()
        }
        checkNewCarsFeedIsOpened()
    }

    @Test
    fun shouldSingleRedirectWhenSingleMarkAndModelWithSingleToSingleOfferAfterTabSwitch() {
        offersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher(NEW_CARS_FEED_SINGLE_OFFER)
        showSearchFeed(
            listOf(
                Mark(
                    id = "BMW",
                    name = "BMW",
                    models = listOf(Model(id = "X6", name = "X6", nameplates = emptyList(), generations = emptyList()))
                )
            ),
            shouldWaitFeed = false
        )
        checkGroupOfferCard {
            isOfferCardTitle(GROUP_OFFER_TITLE)
        }
        performSearchFeed {
            pressBack()
            performSearchFeed { scrollToTop() }
            selectState(StateGroup.USED)
            waitSearchFeed()
            selectState(StateGroup.NEW)
            waitSearchFeed()
        }
        checkNewCarsFeedIsOpened()
    }

    @Test
    fun shouldSingleRedirectWhenMultipleToSingleMarkAndModelAfterTabSwitchWithSingleOffer() {
        offersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher(NEW_CARS_FEED_SINGLE_OFFER)
        showSearchFeed(emptyList())
        checkNewCarsFeedIsOpened()

        performSearchFeed {
            selectState(StateGroup.USED)
            interactions.onEmptyMMNGField().performClick()
        }
        performMark {
            selectMark("BMW")
            selectModel("1 серия")
        }
        performSearchFeed {
            waitSearchFeed()
        }
        performSearchFeed {
            selectState(StateGroup.NEW)
        }
        checkGroupOfferCard {
            isOfferCardTitle(GROUP_OFFER_TITLE)
        }
    }

    @Test
    fun shouldNotRedirectWhenSingleMarkAndModelWithMultipleToSingleOfferAfterSortSwitch() {
        offersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher(NEW_CARS_FEED)
        showSearchFeed(
            listOf(
                Mark(
                    id = "BMW",
                    name = "BMW",
                    models = listOf(Model(id = "X6", name = "X6", nameplates = emptyList(), generations = emptyList()))
                )
            )
        )
        performSearchFeed {
            waitSearchFeed()
        }
        checkNewCarsFeedIsOpened()
        offersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher(NEW_CARS_FEED_SINGLE_OFFER)

        performSearchFeed {
            openSort()
        }
        performFeedSort {
            selectSort(SORT_NAME_TO_SELECT)
        }
        checkNewCarsFeedIsOpened()
    }

    @Test
    fun shouldRedirectWhenSingleMarkAndModelWithMultipleToSingleOfferAfterRadiusSwitch() {
        offersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher(NEW_CARS_FEED)
        showSearchFeed(
            listOf(
                Mark(
                    id = "BMW",
                    name = "BMW",
                    models = listOf(Model(id = "X6", name = "X6", nameplates = emptyList(), generations = emptyList()))
                )
            )
        )
        performSearchFeed {
            waitSearchFeed()
        }
        checkNewCarsFeedIsOpened()
        offersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher(NEW_CARS_FEED_SINGLE_OFFER)

        performSearchFeed {
            clickGeo()
        }
        performMultiGeo {
            clickRegionSubtitle(GEO_GROUP_NAME)
            clickRegionTitle(GEO_REGION_NAME)
            clickAcceptButton()
        }
        checkGroupOfferCard {
            isOfferCardTitle(GROUP_OFFER_TITLE)
        }
    }

    @Test
    fun shouldNotRedirectedWhenSingleMarkAndModelWithMultipleOffers() {
        offersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher(NEW_CARS_FEED)
        showSearchFeed(
            listOf(
                Mark(
                    id = "BMW",
                    name = "BMW",
                    models = listOf(Model(id = "X6", name = "X6", nameplates = emptyList(), generations = emptyList()))
                )
            )
        )
        checkNewCarsFeedIsOpened()
    }

    @Test
    fun shouldNotRedirectedWhenSingleMarkAndModelWithSingleOfferOnUsedFeed() {
        offersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher("used_cars")
        showSearchFeed(
            listOf(
                Mark(
                    id = "BMW",
                    name = "BMW",
                    models = listOf(Model(id = "X6", name = "X6", nameplates = emptyList(), generations = emptyList()))
                )
            ),
            stateGroup = StateGroup.USED
        )
        performSearchFeed { scrollToTop() }
        checkSearchFeed {
            interactions.onStateSegment().waitUntilIsDisplayed()
            isStateSelectorChecked(StateGroup.USED)
        }
    }

    @Test
    fun shouldNotRedirectedWhenSingleMarkAndModelWithSingleOfferOnAllFeed() {
        offersDispatcherHolder.innerDispatcher = PostSearchOffersDispatcher("used_cars")
        showSearchFeed(
            listOf(
                Mark(
                    id = "BMW",
                    name = "BMW",
                    models = listOf(Model(id = "X6", name = "X6", nameplates = emptyList(), generations = emptyList()))
                )
            ),
            stateGroup = StateGroup.ALL
        )
        performSearchFeed { scrollToTop() }
        checkSearchFeed {
            interactions.onStateSegment().waitUntilIsDisplayed()
            isStateSelectorChecked(StateGroup.ALL)
        }
    }

    private fun checkNewCarsFeedIsOpened() = step("check that search feed opened on new cars segment") {
        performSearchFeed { scrollToTop() }
        checkSearchFeed {
            interactions.onStateSegment().waitUntilIsDisplayed()
            isStateSelectorChecked(StateGroup.NEW)
        }
    }

    private fun showSearchFeed(
        selectedMarks: List<Mark>,
        stateGroup: StateGroup = StateGroup.NEW,
        shouldWaitFeed: Boolean = true
    ) = step(
        "open search feed with ${selectedMarks.size} marks and state group $stateGroup"
    ) {
        activityRule.launchFragment<SearchFeedFragment>(
            searchFeedBundle(
                VehicleSearchToFormStateConverter.convert(
                    CarSearch(
                        carParams = CarParams(),
                        commonParams = CommonVehicleParams(
                            stateGroup = stateGroup,
                            marks = selectedMarks
                        )
                    )
                )
            )
        )
        if (shouldWaitFeed) {
            performSearchFeed {
                waitSearchFeed()
            }
        }
    }

    companion object {
        private val SORT_NAME_TO_SELECT = getResourceString(R.string.sort_name)
        private const val GEO_GROUP_NAME = "Алтайский край"
        private const val GEO_REGION_NAME = "Барнаул"
        private const val NEW_CARS_FEED = "new_cars"
        private const val NEW_CARS_FEED_SINGLE_OFFER = "new_cars_single_offer"
        private const val GROUP_OFFER_TITLE = "BMW X6 40d II (F16)"
    }
}
