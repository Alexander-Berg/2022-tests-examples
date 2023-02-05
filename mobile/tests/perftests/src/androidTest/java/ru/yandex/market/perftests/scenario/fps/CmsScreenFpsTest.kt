package ru.yandex.market.perftests.scenario.fps

import com.yandex.perftests.runner.PerfTest
import com.yandex.perftests.runner.PerfTestJUnit4Runner
import org.junit.runner.RunWith
import ru.yandex.market.perftests.Constants
import ru.yandex.market.perftests.Owners
import ru.yandex.market.perftests.dsl.getCmsRootRecycler
import ru.yandex.market.perftests.dsl.startMainActivity
import ru.yandex.market.perftests.dsl.waitBeforeMeasure
import ru.yandex.market.perftests.dsl.waitFrameMetrics
import ru.yandex.market.perftests.dsl.waitMediaCarousel
import ru.yandex.market.perftests.scenario.MarketBasePerfTest

@PerfTest(
    packageName = Constants.PACKAGE_NAME,
    description = "CmsScreenFpsTest",
    timeoutSeconds = Constants.LONG_TIMEOUT,
    owners = [Owners.DMTRIY_DOGAEV]
)
@RunWith(PerfTestJUnit4Runner::class)
class CmsScreenFpsTest : MarketBasePerfTest() {

    override fun scenario() {
        startMainActivity()
        waitMediaCarousel()
        waitBeforeMeasure()

        val recycler = getCmsRootRecycler()
        repeat(SCROLL_COUNT) {
            recycler.scrollForward()
        }
        waitFrameMetrics("cms_screen_rv")
    }

    companion object {
        private const val SCROLL_COUNT = 7
    }
}