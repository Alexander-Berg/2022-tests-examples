package ru.yandex.market.perftests.scenario.fps

import com.yandex.perftests.runner.PerfTest
import com.yandex.perftests.runner.PerfTestJUnit4Runner
import org.junit.runner.RunWith
import ru.yandex.market.perftests.Constants
import ru.yandex.market.perftests.Owners
import ru.yandex.market.perftests.dsl.findAppleBrand
import ru.yandex.market.perftests.dsl.findSearchButton
import ru.yandex.market.perftests.dsl.getSearchRootRecycler
import ru.yandex.market.perftests.dsl.longWaitForIdle
import ru.yandex.market.perftests.dsl.search
import ru.yandex.market.perftests.dsl.startMainActivity
import ru.yandex.market.perftests.dsl.waitBeforeMeasure
import ru.yandex.market.perftests.dsl.waitForIdle
import ru.yandex.market.perftests.dsl.waitFrameMetrics
import ru.yandex.market.perftests.scenario.MarketBasePerfTest

@PerfTest(
    packageName = Constants.PACKAGE_NAME,
    description = "SearchScreenFpsTest",
    timeoutSeconds = Constants.LONG_TIMEOUT,
    owners = [Owners.DMTRIY_DOGAEV]
)
@RunWith(PerfTestJUnit4Runner::class)
class SearchScreenFpsTest : MarketBasePerfTest() {

    override fun scenario() {
        startMainActivity()
        waitForIdle()

        val navigateToCatalogButton = findSearchButton()
        navigateToCatalogButton.click()
        waitForIdle()

        search("iphone")
        longWaitForIdle()
        val appleSadjest = findAppleBrand()
        waitForIdle()
        appleSadjest.clickAndWaitForNewWindow()

        waitBeforeMeasure()

        val recycler = getSearchRootRecycler()
        repeat(SCROLL_COUNT) {
            recycler.scrollForward()
        }

        waitFrameMetrics("search_result_screen_rv")
    }

    companion object {
        private const val SCROLL_COUNT = 7
    }
}