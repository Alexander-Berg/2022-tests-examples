package ru.auto.ara.test.offline

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.reviews.GetReviewsListingDispatcher
import ru.auto.ara.core.dispatchers.reviews.getReviewsCounter
import ru.auto.ara.core.robot.performDevice
import ru.auto.ara.core.robot.reviews.checkReviewFeedTab
import ru.auto.ara.core.robot.reviews.performReviewFeedTab
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.OfflineRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.activityScenarioRule

@RunWith(Parameterized::class)
class OfflineOtherTest(
    private val text: Int,
    private val checkOffline: () -> Unit,
    private val check: () -> Unit
) {


    private val webServerRule = WebServerRule {
        getReviewsCounter("CARS")
        delegateDispatchers(
            GetReviewsListingDispatcher.generic(),
        )
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        OfflineRule(),
        webServerRule,
        activityScenarioRule<MainActivity>()
    )

    @Test
    fun shouldSeeConnectionError() {
        performMain {
            openMainTab(text)
        }
        performReviewFeedTab {
            scrollToError()
        }
        checkOffline()
        performDevice { setInternet(true) }
        performReviewFeedTab { scrollToAutoSnippetAtIndex(0) }
        check()
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "index={index}")
        fun data(): Collection<Array<Any>> =
            listOf<Array<Any>>(
                arrayOf(
                    R.string.reviews,
                    { checkReviewFeedTab { isConnectionError() } },
                    { performReviewFeedTab { clickRepeat() }.checkResult { isReviewFeed() } }
                ))
    }
}
