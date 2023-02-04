package ru.auto.ara.test.listing

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.core.dispatchers.DelegateDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.DEFAULT_COMMON_PARAMS
import ru.auto.ara.core.testdata.SOME_REGION
import ru.auto.ara.core.utils.launchFragment
import ru.auto.ara.data.models.FormState
import ru.auto.ara.ui.activity.SearchFeedActivity
import ru.auto.ara.ui.fragment.feed.SearchFeedFragment
import ru.auto.ara.ui.helpers.form.util.VehicleSearchToFormStateConverter
import ru.auto.ara.viewmodel.search.SearchFeedContext
import ru.auto.data.model.catalog.SteeringWheel
import ru.auto.data.model.common.Transmission
import ru.auto.data.model.filter.BodyTypeGroup
import ru.auto.data.model.filter.CarGearType
import ru.auto.data.model.filter.CarParams
import ru.auto.data.model.filter.CarSearch
import ru.auto.data.model.filter.EngineGroup
import ru.auto.data.model.filter.OwnersCountGroup
import ru.auto.data.model.filter.StateGroup
import ru.auto.data.model.filter.TruckCategory
import ru.auto.data.model.filter.TruckParams
import ru.auto.data.model.filter.TruckSearch
import ru.auto.data.model.search.Mark
import ru.auto.data.model.search.Model
import ru.auto.data.model.search.SearchContext

@RunWith(Parameterized::class)
class FabFeedParametersCountTest(private val param: TestParameter) {

    private val dispatchers: List<DelegateDispatcher> = listOf(
        PostSearchOffersDispatcher.getGenericFeed()
    )
    private val activityTestRule = lazyActivityScenarioRule<SearchFeedActivity>()

    @Rule
    @JvmField
    val ruleChain = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        DisableAdsRule(),
        activityTestRule
    )

    @Before
    fun setUp() {
        activityTestRule.launchFragment<SearchFeedFragment>(
            SearchFeedFragment.createArgs(
                SearchFeedContext(
                    context = SearchContext.DEFAULT,
                    formState = param.formstate
                )
            )
        )
        performSearchFeed { waitSearchFeed() }
    }

    @Test
    fun shouldShowFabOptionsCount() {
        performSearchFeed().checkResult {
            isFabParameterCount(param.expectedFabOptionsCount)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Any?> = listOf(
            TestParameter(
                description = "no options are selected",
                formstate = FormState.withDefaultCategory(),
                expectedFabOptionsCount = 0
            ),
            TestParameter(
                description = "only options selected are minifilter ones",
                formstate = VehicleSearchToFormStateConverter.convert(
                    TruckSearch(
                        commonParams = DEFAULT_COMMON_PARAMS.copy(
                            geoRadius = 500,
                            geoRadiusSupport = true,
                            regions = listOf(SOME_REGION),
                            stateGroup = StateGroup.NEW,
                            marks = listOf(
                                Mark(
                                    id = "GAZ",
                                    name = "ГАЗ",
                                    models = listOf(
                                        Model(
                                            id = "GAZEL_NEXT",
                                            name = "ГАЗель Next",
                                            nameplates = emptyList(),
                                            generations = emptyList()
                                        )
                                    )
                                ),
                                Mark(
                                    id = "BAW",
                                    name = "BAW",
                                    models = listOf(
                                        Model(
                                            id = "TONIK",
                                            name = "Tonik",
                                            nameplates = emptyList(),
                                            generations = emptyList()
                                        )
                                    )
                                )
                            )
                        ),
                        truckParams = TruckParams(
                            trucksCategory = TruckCategory.LCV
                        )
                    )
                ),
                expectedFabOptionsCount = 0
            ),
            TestParameter(
                description = "some multiselects selected 1 for each type",
                formstate = VehicleSearchToFormStateConverter.convert(
                    CarSearch(
                        carParams = CarParams(
                            transmission = listOf(Transmission.AUTO, Transmission.ROBOT),
                            engineGroup = listOf(EngineGroup.DIESEL, EngineGroup.GASOLINE),
                            gearType = listOf(CarGearType.FORWARD_CONTROL, CarGearType.REAR_DRIVE),
                            bodyTypeGroup = listOf(BodyTypeGroup.ALLROAD_3_DOORS, BodyTypeGroup.ALLROAD_5_DOORS)
                        ),
                        commonParams = DEFAULT_COMMON_PARAMS.copy(
                            catalogEquipment = listOf("halogen,xenon"),
                            priceFrom = 100_000L,
                            priceTo = 1000_000L
                        )
                    )
                ),
                expectedFabOptionsCount = 6
            ),
            TestParameter(
                description = "some single selects selected 1 for each type",
                formstate = VehicleSearchToFormStateConverter.convert(
                    CarSearch(
                        carParams = CarParams(
                            steeringWheel = SteeringWheel.LEFT
                        ),
                        commonParams = DEFAULT_COMMON_PARAMS.copy(
                            catalogEquipment = listOf("automatic-lighting-control"),
                            ownersCountGroup = OwnersCountGroup.LESS_THAN_TWO
                        )
                    )
                ),
                expectedFabOptionsCount = 3
            )
        )
    }

    data class TestParameter(
        val description: String,
        val formstate: FormState,
        val expectedFabOptionsCount: Int
    ) {
        override fun toString(): String = "$expectedFabOptionsCount when $description"
    }
}
