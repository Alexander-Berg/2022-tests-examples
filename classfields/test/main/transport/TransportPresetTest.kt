package ru.auto.ara.test.main.transport

import androidx.annotation.StringRes
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.BodyNode.Companion.asObject
import ru.auto.ara.core.dispatchers.BodyNode.Companion.assertValue
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.search_offers.CountDispatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.robot.transporttab.TransportRobot
import ru.auto.ara.core.robot.transporttab.TransportRobotChecker
import ru.auto.ara.core.robot.transporttab.performTransport
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.testdata.COMM_CATEGORY_PRESETS
import ru.auto.ara.core.testdata.MOTO_CATEGORY_PRESETS
import ru.auto.ara.core.testdata.Preset
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.core.waiter.retry
import java.util.concurrent.TimeUnit

@RunWith(Parameterized::class)
class TransportPresetTest(private val param: TestParam) {

    private val postSearchOffersWatcher = RequestWatcher()
    private val dispatchers = listOf(
        PostSearchOffersDispatcher.getGenericFeed(postSearchOffersWatcher),
        CountDispatcher(param.requestCategory)
    )

    var activityTestRule = ActivityTestRule(
        MainActivity::class.java
    )

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        DisableAdsRule(),
        activityTestRule
    )

    @Test
    fun shouldPresetsLookCorrect() {
        val helper = param.testHelper
        performTransport {
            selectCategory(param.category)
            helper.scrollToPreset(this, param.presetIndex)
        }.checkResult {
            helper.isPresetDisplayed(this, param.presetIndex, param.presetLabel, param.presetImage)
        }

        performTransport {
            waitSomething(1, TimeUnit.SECONDS) // avoid click on params button
            helper.clickPreset(this, param.presetIndex)
        }

        performSearchFeed {
            waitSearchFeed()
        }

        param.checkRequest(postSearchOffersWatcher)
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "index={index}")
        fun data() = listOf(
            motoCategoriesData(),
            truckCategoriesData()
        ).flatten()

        fun motoCategoriesData(): List<TestParam> =
            MOTO_CATEGORY_PRESETS.mapIndexed { index: Int, preset: Preset.MotoCategory ->
                TestParam(
                    category = R.string.category_moto,
                    presetIndex = index,
                    presetLabel = preset.label,
                    presetImage = preset.imageRes,
                    requestCategory = "moto",
                    checkRequest = preset.checkRequest(),
                    testHelper = MotoAndTruckTestHelper
                )
            }

        fun truckCategoriesData(): List<TestParam> =
            COMM_CATEGORY_PRESETS.mapIndexed { index: Int, preset: Preset.CommCategory ->
                TestParam(
                    category = R.string.category_comm,
                    presetIndex = index,
                    presetLabel = preset.label,
                    presetImage = preset.imageRes,
                    requestCategory = "trucks",
                    checkRequest = preset.checkRequest(),
                    testHelper = MotoAndTruckTestHelper
                )
            }

        private fun Preset.checkRequest(): (RequestWatcher) -> Unit = when (this) {
            is Preset.MotoCategory -> { watcher ->
                watcher.checkBody {
                    retry {
                        asObject {
                            getValue("moto_params").asObject {
                                getValue("moto_category").assertValue(name)
                            }
                        }
                    }
                }
            }
            is Preset.CommCategory -> { watcher ->
                watcher.checkBody {
                    retry {
                        asObject {
                            getValue("trucks_params").asObject {
                                getValue("trucks_category").assertValue(name)
                            }
                        }
                    }
                }
            }
        }
    }

    data class TestParam(
        @StringRes
        val category: Int,
        val presetIndex: Int,
        val presetLabel: String,
        val presetImage: Int? = null,
        val requestCategory: String,
        val checkRequest: (RequestWatcher) -> Unit,
        val testHelper: TestHelper
    )

    interface TestHelper {
        fun scrollToPreset(robot: TransportRobot, index: Int)
        fun clickPreset(robot: TransportRobot, index: Int)
        fun isPresetDisplayed(robot: TransportRobotChecker, index: Int, label: String, image: Int?)
    }

    object MotoAndTruckTestHelper : TestHelper {

        override fun scrollToPreset(robot: TransportRobot, index: Int) {
            robot.scrollToPreset(index)
        }

        override fun clickPreset(robot: TransportRobot, index: Int) {
            robot.clickPreset(index)
        }

        override fun isPresetDisplayed(robot: TransportRobotChecker, index: Int, label: String, image: Int?) {
            robot.isPresetDisplayed(index, label, image)
        }
    }
}
