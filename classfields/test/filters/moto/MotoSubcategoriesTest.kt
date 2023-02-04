package ru.auto.ara.test.filters.moto

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.core.dispatchers.BodyNode.Companion.asObject
import ru.auto.ara.core.dispatchers.search_offers.getOfferCount
import ru.auto.ara.core.robot.searchfeed.FilterRobotChecker
import ru.auto.ara.core.robot.searchfeed.checkFilter
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.routing.Route
import ru.auto.ara.core.routing.RoutingDefinition
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.screenbundles.SearchFeedFragmentBundles.searchFeedMotoWithSubcategoryBundle
import ru.auto.ara.core.testdata.MOTO_SUBCATEGORIES_PARAMS
import ru.auto.ara.core.utils.launchFragment
import ru.auto.ara.ui.activity.SearchFeedActivity
import ru.auto.ara.ui.fragment.feed.SearchFeedFragment
import ru.auto.data.model.filter.MotoCategory.MOTORCYCLE

@RunWith(Parameterized::class)
class MotoSubcategoriesTest(private val testParams: TestParameter) {
    private val webServerRule = WebServerRule {
        getOfferCount(100, "moto").watch {
            checkRequestBodyParameter(MOTO_SUBCATEGORIES_PARAM, DEFAULT_SUBCATEGORY.name)
        }
    }
    private val activityRule = lazyActivityScenarioRule<SearchFeedActivity>()
    @JvmField
    @Rule
    val rules = baseRuleChain(
        webServerRule,
        activityRule
    )

    @Before
    fun setUp() {
        activityRule.launchFragment<SearchFeedFragment>(searchFeedMotoWithSubcategoryBundle(MOTORCYCLE))
        performSearchFeed { openParameters() }
        if (isNonDefaultCategory(testParams.param)){
            webServerRule.routing(testParams.dispatchers)
        }
        performFilter {
            clickFieldWithHintWithOverScroll(FIELD_NAME, MOTO_SUBCATEGORIES_FIELD_NAME)
            clickOption(testParams.name)
        }
    }

    @Test
    fun shouldSeeCorrectStateAndRequestsAtFilterScreen() {
        checkFilter { testParams.checkAtFilterScreenAndWatcher(this) }
    }

    @Test
    fun shouldSeeCorrectStateAtBottomSheet() {
        performFilter {
            waitBottomSheet()
            clickFieldWithHintWithOverScroll(FIELD_NAME, MOTO_SUBCATEGORIES_FIELD_NAME)
        }
        checkFilter { testParams.checkPicker(this) }
    }

    companion object {
        const val FIELD_NAME = "Год выпуска"
        const val MOTO_SUBCATEGORIES_FIELD_NAME = "Раздел"
        private const val MOTO_SUBCATEGORIES_PARAM = "moto_params.moto_category"
        val DEFAULT_SUBCATEGORY = MOTORCYCLE

        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<out Any?>> = MOTO_SUBCATEGORIES.map { arrayOf(it) }

        private val MOTO_SUBCATEGORIES = MOTO_SUBCATEGORIES_PARAMS.map { (name, param) ->
            TestParameter(
                name = name,
                param = param,
                dispatchers = {
                    getOfferCount(50826, "moto") { asObject { !containsKey("creation_date_to") } }
                        .watch { checkRequestBodyParameter(MOTO_SUBCATEGORIES_PARAM, param) }
                },
                checkAtFilterScreenAndWatcher = {
                    isInputContainer(MOTO_SUBCATEGORIES_FIELD_NAME, name)
                    if (isNonDefaultCategory(param)) {
                        isDoSearchButtonWithText("Показать 50,826 предложений")
                    }
                },
                checkPicker = {
                    isCheckedOptionDisplayed(name)
                }
            )
        }

        fun isNonDefaultCategory(subcategory: String) = !subcategory.equals(DEFAULT_SUBCATEGORY.name,true)

        data class TestParameter(
            val name: String,
            val param: String,
            val dispatchers: RoutingDefinition<Route>,
            val checkAtFilterScreenAndWatcher: FilterRobotChecker.() -> Unit,
            val checkPicker: FilterRobotChecker.() -> Unit
        ) {
            override fun toString() = param
        }
    }
}
