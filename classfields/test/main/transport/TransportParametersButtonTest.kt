package ru.auto.ara.test.main.transport

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.robot.searchfeed.checkFilter
import ru.auto.ara.core.robot.transporttab.checkTransport
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.SearchFeedDefaultParams
import ru.auto.data.model.VehicleCategory

@RunWith(Parameterized::class)
class TransportParametersButtonTest(private val testParameter: TestParameter) {

    private val counterWatcher = RequestWatcher()
    private val dispatchers: List<DelegateDispatcher> = listOf(
        CountDispatcher(testParameter.categoryName, counterWatcher)
    )

    private val activityRule = lazyActivityScenarioRule<MainActivity>()

    @JvmField
    @Rule
    val rules = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        activityRule
    )

    @Before
    fun setUp() {
        activityRule.launchActivity()
    }

    @Test
    fun shouldOpenFilterWithDefaultParams() {
        performMain {
            selectCategory(testParameter.categoryNameRes)
        }
        testParameter.subCategoryName?.let { name ->
            checkTransport { isPresetDisplayed(0, name) }
        }
        performMain { openFilters() }
        SearchFeedDefaultParams.stringParams(testParameter.categoryEnum).map { (name, value) ->
            counterWatcher.checkRequestBodyParameter(name, value)
        }
        SearchFeedDefaultParams.arrayParams(testParameter.categoryEnum).map { (name, value) ->
            counterWatcher.checkRequestBodyExactlyArrayParameter(name, value)
        }
        checkFilter {
            isToggleButtonChecked(testParameter.categoryNameRes)
            isToggleButtonChecked(R.string.all)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = listOf(
            TestParameter(
                "cars",
                VehicleCategory.CARS,
                R.string.category_auto
            ),
            TestParameter(
                "trucks",
                VehicleCategory.TRUCKS,
                R.string.category_comm,
                "Лёгкий коммерческий"
            ),
            TestParameter(
                "moto",
                VehicleCategory.MOTO,
                R.string.category_moto,
                "Мотоциклы"
            )
        )

        data class TestParameter(
            val categoryName: String,
            val categoryEnum: VehicleCategory,
            val categoryNameRes: Int,
            val subCategoryName: String? = null
        ) {
            override fun toString() = categoryName
        }
    }
}
