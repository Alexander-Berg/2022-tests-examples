package ru.auto.ara.test.main.transport

import androidx.annotation.StringRes
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.robot.transporttab.performTransport
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.activityScenarioRule

@RunWith(Parameterized::class)
class TransportCategoryTest(
    private val param: TestParam
) {

    private val activityTestRule = activityScenarioRule<MainActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule(),
        DisableAdsRule(),
        activityTestRule
    )

    @Test
    fun shouldUpdateCategoryOnDeeplink() {
        performTransport {
            interactions.onCategorySegmentButton(param.categorySegmentRes).performClick()
        }
        param.check()
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data() = listOf(
            TestParam(
                categorySegmentRes = R.string.category_moto,
                check = {
                    performTransport {}.checkResult {
                        checkCategorySegmentSelected(R.string.category_moto)
                        checkMotoPresetsShown()
                    }
                },
                description = "moto"
            ),
            TestParam(
                categorySegmentRes = R.string.category_comm,
                check = {
                    performTransport {}.checkResult {
                        checkCategorySegmentSelected(R.string.category_comm)
                        checkCommPresetsShown()
                    }
                },
                description = "comm"
            )
        )

        data class TestParam(
            @StringRes val categorySegmentRes: Int,
            val check: () -> Unit,
            val description: String
        ) {
            override fun toString() = description
        }
    }
}
