package ru.auto.ara.test.deeplink

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.core.dispatchers.device.ParseDeeplinkDispatcher
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(Parameterized::class)
class MiniFilterDeeplinkTest(private val testParam: TestParameter) {

    private val dispatchers = listOf(
        PostSearchOffersDispatcher.getGenericFeed(),
        ParseDeeplinkDispatcher(testParam.dispatcherFile, requestWatcher = null),
        CountDispatcher("cars")
    )
    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        DisableAdsRule(),
        activityTestRule
    )

    @Test
    fun shouldOpenDeeplink() {
        activityTestRule.launchDeepLinkActivity(testParam.uri)
        testParam.check()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<TestParameter> =
            listOf(
                TestParameter(
                    "autoru://app/sankt-peterburg/cars/used/",
                    "sankt-peterburg_cars_used",
                    check = {
                        performSearchFeed {}.checkResult {
                            isEmptyMMNGFilterDisplayed()
                            isRegionWithText("Санкт-Петербург", "+ 200 км")
                            isSavedSearchSelected(false)
                        }
                    }
                ),
                TestParameter(
                    "https://auto.ru/cars/ford/used/",
                    "cars_ford_used",
                    check = {
                        performSearchFeed {}.checkResult {
                            isMarkFilterWithText("Ford")
                            isEmptyModelFilterDisplayed()
                            isGenerationFilterIsHidden()
                        }
                    }
                ),
                TestParameter(
                    "https://auto.ru/cars/ford/mondeo/used/",
                    "cars_ford_mondeo_used",
                    check = {
                        performSearchFeed {}.checkResult {
                            isMarkFilterWithText("Ford")
                            isModelFilterWithText("Mondeo")
                            isEmptyGenerationFilterDisplayed()
                        }
                    }
                ),
                TestParameter(
                    "https://auto.ru/cars/ford/mondeo/20270679/all/",
                    "cars_ford_mondeo_20270679_all",
                    check = {
                        performSearchFeed {}.checkResult {
                            isMarkFilterWithText("Ford")
                            isModelFilterWithText("Mondeo")
                            isGenerationFilterWithText("2014 - 2019 V")
                        }
                    }
                ),
                TestParameter(
                    "https://auto.ru/rossiya/cars/used/",
                    "rossiya_cars_used",
                    check = {
                        performSearchFeed {}.checkResult {
                            isEmptyMMNGFilterDisplayed()
                            isRegionWithText("Россия")
                        }
                    }
                ),
                TestParameter(
                    "https://auto.ru/rossiya/cars/all/?mark_model_nameplate=AUDI&mark_model_nameplate=FORD&mark_model_nameplate=BMW&mark_model_nameplate=CHERY&mark_model_nameplate=HONDA",
                    "rossiya_cars_AUDI_FORD_BMW_CHERY_HONDA",
                    check = {
                        performSearchFeed {}.checkResult {
                            isEmptyMMNGFilterNotDisplayed()
                        }
                    }
                )
            )

        data class TestParameter(val uri: String, val dispatcherFile: String, val check: () -> Unit) {
            override fun toString(): String = dispatcherFile
        }
    }
}
