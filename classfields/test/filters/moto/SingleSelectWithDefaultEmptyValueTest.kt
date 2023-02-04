package ru.auto.ara.test.filters.moto

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.robot.searchfeed.checkFilter
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.screenbundles.SearchFeedFragmentBundles.searchFeedMotoWithSubcategoryBundle
import ru.auto.ara.core.testdata.ATV_STROKE_PARAMS
import ru.auto.ara.core.testdata.MOTORCYCLE_STROKE_PARAMS
import ru.auto.ara.core.testdata.SCOOTERS_STROKE_PARAMS
import ru.auto.ara.core.testdata.SNOWMOBILE_STROKE_PARAMS
import ru.auto.ara.core.utils.launchFragment
import ru.auto.ara.ui.activity.SearchFeedActivity
import ru.auto.ara.ui.fragment.feed.SearchFeedFragment
import ru.auto.data.model.filter.MotoCategory

@RunWith(Parameterized::class)
class SingleSelectWithDefaultEmptyValueTest(private val testParameter: TestParameter) {

    val countWatcher = RequestWatcher()
    private val dispatchers = listOf(
        CountDispatcher("moto", countWatcher),
        PostSearchOffersDispatcher.getGenericFeed()
    )
    private val activityRule = lazyActivityScenarioRule<SearchFeedActivity>()

    @JvmField
    @Rule
    val rules = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        activityRule
    )

    @Before
    fun setUp() {
        activityRule.launchFragment<SearchFeedFragment>(searchFeedMotoWithSubcategoryBundle(testParameter.motoCategory))
        performSearchFeed { openParameters() }
        performFilter {
            clickFieldWithHintWithOverScroll(testParameter.nameOfFieldToScroll, testParameter.nameOfFieldToClick)
        }
    }

    @Test
    fun shouldSeeCorrectStateAndRequestsAtFilterScreen() {
        val randomIndex = testParameter.arrayOfParams.indices.random()
        val randomOption = testParameter.arrayOfParams[randomIndex]
        val (nameInBottomSheet, nameInListing, param) = randomOption

        checkFilter {
            isBottomsheetListHasExpectedChildsCount(testParameter.arrayOfParams.size)
            isCheckedOptionDisplayed(testParameter.defaultEmptyValueName)
            testParameter.arrayOfParams.drop(1).map { (optionName) ->
                isNotCheckedOptionDisplayed(optionName)
            }
        }

        performFilter { clickOption(nameInBottomSheet) }.checkResult {
            isInputContainer(testParameter.nameOfFieldToClick, nameInListing)
            if (nameInBottomSheet == testParameter.defaultEmptyValueName) {
                countWatcher.checkNotRequestBodyParameter(testParameter.paramName)
            } else {
                countWatcher.checkRequestBodyArrayParameter(testParameter.paramName, setOf(param))
            }
        }
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<out Any?>> = (listOf(
            TestParameter(
                motoCategory = MotoCategory.MOTORCYCLE,
                nameOfFieldToScroll = "Цвет",
                nameOfFieldToClick = "Число тактов",
                defaultEmptyValueName = "Любое",
                paramName = "moto_params.strokes",
                arrayOfParams = MOTORCYCLE_STROKE_PARAMS
            ),
            TestParameter(
                motoCategory = MotoCategory.SCOOTERS,
                nameOfFieldToScroll = "Цвет",
                nameOfFieldToClick = "Число тактов",
                defaultEmptyValueName = "Любое",
                paramName = "moto_params.strokes",
                arrayOfParams = SCOOTERS_STROKE_PARAMS
            ),
            TestParameter(
                motoCategory = MotoCategory.ATV,
                nameOfFieldToScroll = "Цвет",
                nameOfFieldToClick = "Число тактов",
                defaultEmptyValueName = "Любое",
                paramName = "moto_params.strokes",
                arrayOfParams = ATV_STROKE_PARAMS
            ),
            TestParameter(
                motoCategory = MotoCategory.SNOWMOBILE,
                nameOfFieldToScroll = "Цвет",
                nameOfFieldToClick = "Число тактов",
                defaultEmptyValueName = "Любое",
                paramName = "moto_params.strokes",
                arrayOfParams = SNOWMOBILE_STROKE_PARAMS
            )
        )).map { arrayOf(it) }

        data class TestParameter(
            val motoCategory: MotoCategory,
            val nameOfFieldToScroll: String,
            val nameOfFieldToClick: String,
            val defaultEmptyValueName: String,
            val paramName: String,
            val arrayOfParams: Array<Array<String>>
        ) {
            override fun toString() = "${motoCategory.name} field=$paramName"
        }
    }
}
