package ru.yandex.market.perftests.scenario.fps

import com.yandex.perftests.runner.PerfTest
import com.yandex.perftests.runner.PerfTestJUnit4Runner
import org.junit.runner.RunWith
import ru.yandex.market.perftests.Constants
import ru.yandex.market.perftests.Owners
import ru.yandex.market.perftests.dsl.findNavigateToProfileButton
import ru.yandex.market.perftests.dsl.getProfileRootRecycler
import ru.yandex.market.perftests.dsl.login
import ru.yandex.market.perftests.dsl.startMainActivity
import ru.yandex.market.perftests.dsl.waitBeforeMeasure
import ru.yandex.market.perftests.dsl.waitFrameMetrics
import ru.yandex.market.perftests.scenario.MarketBasePerfTest

@PerfTest(
    packageName = Constants.PACKAGE_NAME,
    description = "ProfileScreenFpsTest",
    timeoutSeconds = Constants.LONG_TIMEOUT,
    owners = [Owners.DMTRIY_DOGAEV]
)
@RunWith(PerfTestJUnit4Runner::class)
class ProfileScreenFpsTest : MarketBasePerfTest() {

    override fun beforeAllTests() {
        super.beforeAllTests()
        login()
    }

    override fun scenario() {
        startMainActivity()
        findNavigateToProfileButton().clickAndWaitForNewWindow()

        val recycler = getProfileRootRecycler() ?: return

        waitBeforeMeasure()

        repeat(SCROLL_COUNT) {
            recycler.scrollForward()
        }
        repeat(SCROLL_COUNT) {
            recycler.scrollBackward()
        }
        waitFrameMetrics("profile_screen_rv")
    }

    companion object {
        private const val SCROLL_COUNT = 2
    }
}