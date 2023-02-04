package ru.auto.ara.test.deeplink

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.catalog.getCatalogSubtree
import ru.auto.ara.core.dispatchers.device.ParseDeeplinkDispatcher
import ru.auto.ara.core.dispatchers.device.getParsedDeeplink
import ru.auto.ara.core.dispatchers.search_offers.MarkModelFiltersDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostEquipmentFiltersDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchSpecialsDispatcher
import ru.auto.ara.core.robot.offercard.performGroupOfferCard
import ru.auto.ara.core.robot.searchfeed.performFeedSort
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.GROUP_CARD_OPTIONS
import ru.auto.ara.core.utils.getResourceString
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(Parameterized::class)
class GroupOfferCardDeeplinkTest(private val testParams: TestParameter) {

    private val markModelFiltersRequestWatcher = RequestWatcher()
    private val markModelFiltersDispatcherHolder: DispatcherHolder = DispatcherHolder()
    private val fullMarkModelFiltersDispatcher: MarkModelFiltersDispatcher =
        MarkModelFiltersDispatcher.full(markModelFiltersRequestWatcher)
    private val emptyMarkModelFiltersDispatcher: MarkModelFiltersDispatcher =
        MarkModelFiltersDispatcher.empty(markModelFiltersRequestWatcher)

    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    private val dispatchers = listOf(
        PostSearchOffersDispatcher.getGenericFeed(),
        ParseDeeplinkDispatcher(testParams.filename, requestWatcher = null),
        PostSearchSpecialsDispatcher.getLadaPresetOffers(),
        markModelFiltersDispatcherHolder,
        PostEquipmentFiltersDispatcher(filePath = "filters/equipment_filters_bmw_x3.json"),
    )

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule {
            getParsedDeeplink(expectedResponse = "search_data_bmw_x3", method = DelegateDispatcher.METHOD_POST)
            getCatalogSubtree("bmw_catalog")
            delegateDispatchers(dispatchers)
        },
        DisableAdsRule(),
        activityTestRule
    )

    @Test
    fun shouldOpenDeeplink() {

        markModelFiltersDispatcherHolder.innerDispatcher = when {
            testParams.isMatchApplicationButtonVisible -> fullMarkModelFiltersDispatcher
            else -> emptyMarkModelFiltersDispatcher
        }

        activityTestRule.launchDeepLinkActivity(testParams.uri)

        performGroupOfferCard {}
            .checkResult {
                isOfferCardTitle("BMW X3 35d III (G01)")
                isSortingTitle(testParams.groupCardDisplayedOption)
                if (testParams.isMatchApplicationButtonVisible) {
                    isMatchApplicationButtonDisplayed()
                } else {
                    isMatchApplicationButtonGone()
                }
                markModelFiltersRequestWatcher.checkQueryParameter("search_tag", "match_applications")
            }

        performGroupOfferCard { clickSortingTitle() }
        performFeedSort().checkResult {
            checkSortSelected(testParams.bottomSheetDisplayedOption)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<out Any?>> = SORTS.map { arrayOf(it) }

        private val SORTS = GROUP_CARD_OPTIONS.zip(
            listOf(
                "relevance" to true,
                "fresh_relevance" to false,
                "price_asc" to false,
                "price_desc" to false,
                "max_discount" to false
            )
        ).map { (pair, secondPair) ->
            val (titleId, queryParameter) = pair
            val (filename, isMatchApplicationEnabled) = secondPair
            TestParameter(
                filename = filename,
                bottomSheetDisplayedOption = getResourceString(titleId),
                groupCardDisplayedOption = "По " + getResourceString(titleId).toLowerCase(),
                uri = "https://auto.ru/cars/new/group/bmw/x3/21029738/21184790/?$queryParameter",
                isMatchApplicationButtonVisible = isMatchApplicationEnabled
            )
        }
    }

    data class TestParameter(
        val filename: String,
        val uri: String,
        val groupCardDisplayedOption: String,
        val bottomSheetDisplayedOption: String,
        val isMatchApplicationButtonVisible: Boolean
    ) {
        override fun toString(): String = filename
    }
}
