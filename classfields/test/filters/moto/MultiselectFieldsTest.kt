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
import ru.auto.ara.core.testdata.ATV_CYLINDERS_COUNT_PARAMS
import ru.auto.ara.core.testdata.ATV_CYLINDERS_TYPE_PARAMS
import ru.auto.ara.core.testdata.ATV_DRIVE_PARAMS
import ru.auto.ara.core.testdata.ATV_ENGINE_TYPE_PARAMS
import ru.auto.ara.core.testdata.ATV_TRANSMISSION_PARAMS
import ru.auto.ara.core.testdata.ATV_TYPE_PARAMS
import ru.auto.ara.core.testdata.MOTORCYCLE_CYLINDERS_COUNT_PARAMS
import ru.auto.ara.core.testdata.MOTORCYCLE_CYLINDERS_TYPE_PARAMS
import ru.auto.ara.core.testdata.MOTORCYCLE_DRIVE_PARAMS
import ru.auto.ara.core.testdata.MOTORCYCLE_ENGINE_TYPE_PARAMS
import ru.auto.ara.core.testdata.MOTORCYCLE_TRANSMISSION_PARAMS
import ru.auto.ara.core.testdata.MOTORCYCLE_TYPE_PARAMS
import ru.auto.ara.core.testdata.SCOOTERS_ENGINE_TYPE_PARAMS
import ru.auto.ara.core.testdata.SNOWMOBILE_CYLINDERS_COUNT_PARAMS
import ru.auto.ara.core.testdata.SNOWMOBILE_CYLINDERS_TYPE_PARAMS
import ru.auto.ara.core.testdata.SNOWMOBILE_ENGINE_TYPE_PARAMS
import ru.auto.ara.core.testdata.SNOWMOBILE_TYPE_PARAMS
import ru.auto.ara.core.utils.launchFragment
import ru.auto.ara.ui.activity.SearchFeedActivity
import ru.auto.ara.ui.fragment.feed.SearchFeedFragment
import ru.auto.data.model.filter.MotoCategory

@RunWith(Parameterized::class)
class MultiselectFieldsTest(private val testParameter: TestParameter) {
    private val countWatcher = RequestWatcher()
    private val webServerRule = WebServerRule {
        delegateDispatchers(
            CountDispatcher("moto", countWatcher),
            PostSearchOffersDispatcher.getGenericFeed()
        )
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
        activityRule.launchFragment<SearchFeedFragment>(searchFeedMotoWithSubcategoryBundle(testParameter.motoCategory))
        performSearchFeed { openParameters() }
        performFilter {
            clickFieldWithOverScroll(testParameter.nameOfFieldToScroll, testParameter.nameOfFieldToClick)
        }
    }

    @Test
    fun shouldSeeAllAvailableValuesAndSelect() {
        val randomIndex = testParameter.arrayOfParams.indices.random()
        val randomOption = testParameter.arrayOfParams[randomIndex]
        val (name, param) = randomOption

        checkFilter {
            isBottomsheetListHasExpectedChildsCount(testParameter.arrayOfParams.size)
            isMultiselectNotCheckedOptionsDisplayed(testParameter.arrayOfParams.map { it.first() })
        }
        performFilter {
            clickMultiSelectOptionWithScroll(name)
            clickAcceptButton()
        }.checkResult {
            isContainer(testParameter.nameOfFieldToClick, name)
            countWatcher.checkRequestBodyArrayParameter(testParameter.paramName, setOf(param))
        }
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<out Any?>> = (listOf(
            TestParameter(
                motoCategory = MotoCategory.MOTORCYCLE,
                nameOfFieldToScroll = "Коробка",
                nameOfFieldToClick = "Тип мотоцикла",
                paramName = "moto_params.moto_type",
                arrayOfParams = MOTORCYCLE_TYPE_PARAMS
            ),
            TestParameter(
                motoCategory = MotoCategory.MOTORCYCLE,
                nameOfFieldToScroll = "Привод",
                nameOfFieldToClick = "Двигатель",
                paramName = "moto_params.engine_type",
                arrayOfParams = MOTORCYCLE_ENGINE_TYPE_PARAMS
            ),
            TestParameter(
                motoCategory = MotoCategory.MOTORCYCLE,
                nameOfFieldToScroll = "Привод",
                nameOfFieldToClick = "Коробка",
                paramName = "moto_params.transmission",
                arrayOfParams = MOTORCYCLE_TRANSMISSION_PARAMS
            ),
            TestParameter(
                motoCategory = MotoCategory.MOTORCYCLE,
                nameOfFieldToScroll = "Расположение цилиндров",
                nameOfFieldToClick = "Привод",
                paramName = "moto_params.gear_type",
                arrayOfParams = MOTORCYCLE_DRIVE_PARAMS
            ),
            TestParameter(
                motoCategory = MotoCategory.MOTORCYCLE,
                nameOfFieldToScroll = "Цвет",
                nameOfFieldToClick = "Количество цилиндров",
                paramName = "moto_params.cylinders",
                arrayOfParams = MOTORCYCLE_CYLINDERS_COUNT_PARAMS
            ),
            TestParameter(
                motoCategory = MotoCategory.MOTORCYCLE,
                nameOfFieldToScroll = "Цвет",
                nameOfFieldToClick = "Расположение цилиндров",
                paramName = "moto_params.cylinders_type",
                arrayOfParams = MOTORCYCLE_CYLINDERS_TYPE_PARAMS
            ),
            TestParameter(
                motoCategory = MotoCategory.SCOOTERS,
                nameOfFieldToScroll = "Цвет",
                nameOfFieldToClick = "Двигатель",
                paramName = "moto_params.engine_type",
                arrayOfParams = SCOOTERS_ENGINE_TYPE_PARAMS
            ),
            TestParameter(
                motoCategory = MotoCategory.ATV,
                nameOfFieldToScroll = "Коробка",
                nameOfFieldToClick = "Тип мотовездехода",
                paramName = "moto_params.atv_type",
                arrayOfParams = ATV_TYPE_PARAMS
            ),
            TestParameter(
                motoCategory = MotoCategory.ATV,
                nameOfFieldToScroll = "Цвет",
                nameOfFieldToClick = "Количество цилиндров",
                paramName = "moto_params.cylinders",
                arrayOfParams = ATV_CYLINDERS_COUNT_PARAMS
            ),
            TestParameter(
                motoCategory = MotoCategory.ATV,
                nameOfFieldToScroll = "Цвет",
                nameOfFieldToClick = "Расположение цилиндров",
                paramName = "moto_params.cylinders_type",
                arrayOfParams = ATV_CYLINDERS_TYPE_PARAMS
            ),
            TestParameter(
                motoCategory = MotoCategory.ATV,
                nameOfFieldToScroll = "Привод",
                nameOfFieldToClick = "Коробка",
                paramName = "moto_params.transmission",
                arrayOfParams = ATV_TRANSMISSION_PARAMS
            ),
            TestParameter(
                motoCategory = MotoCategory.ATV,
                nameOfFieldToScroll = "Цвет",
                nameOfFieldToClick = "Двигатель",
                paramName = "moto_params.engine_type",
                arrayOfParams = ATV_ENGINE_TYPE_PARAMS
            ),
            TestParameter(
                motoCategory = MotoCategory.ATV,
                nameOfFieldToScroll = "Расположение цилиндров",
                nameOfFieldToClick = "Привод",
                paramName = "moto_params.gear_type",
                arrayOfParams = ATV_DRIVE_PARAMS
            ),
            TestParameter(
                motoCategory = MotoCategory.SNOWMOBILE,
                nameOfFieldToScroll = "Двигатель",
                nameOfFieldToClick = "Тип снегохода",
                paramName = "moto_params.snowmobile_type",
                arrayOfParams = SNOWMOBILE_TYPE_PARAMS
            ),
            TestParameter(
                motoCategory = MotoCategory.SNOWMOBILE,
                nameOfFieldToScroll = "Расположение цилиндров",
                nameOfFieldToClick = "Двигатель",
                paramName = "moto_params.engine_type",
                arrayOfParams = SNOWMOBILE_ENGINE_TYPE_PARAMS
            ),
            TestParameter(
                motoCategory = MotoCategory.SNOWMOBILE,
                nameOfFieldToScroll = "Цвет",
                nameOfFieldToClick = "Количество цилиндров",
                paramName = "moto_params.cylinders",
                arrayOfParams = SNOWMOBILE_CYLINDERS_COUNT_PARAMS
            ),
            TestParameter(
                motoCategory = MotoCategory.SNOWMOBILE,
                nameOfFieldToScroll = "Цвет",
                nameOfFieldToClick = "Расположение цилиндров",
                paramName = "moto_params.cylinders_type",
                arrayOfParams = SNOWMOBILE_CYLINDERS_TYPE_PARAMS
            )
        )).map { arrayOf(it) }

        data class TestParameter(
            val motoCategory: MotoCategory,
            val nameOfFieldToScroll: String,
            val nameOfFieldToClick: String,
            val paramName: String,
            val arrayOfParams: Array<Array<String>>
        ) {
            override fun toString() = "${motoCategory.name} field=$paramName"
        }
    }
}
