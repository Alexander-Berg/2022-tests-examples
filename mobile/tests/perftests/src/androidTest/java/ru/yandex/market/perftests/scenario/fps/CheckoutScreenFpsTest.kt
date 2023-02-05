package ru.yandex.market.perftests.scenario.fps

import com.yandex.perftests.runner.PerfTest
import com.yandex.perftests.runner.PerfTestJUnit4Runner
import org.junit.runner.RunWith
import ru.yandex.market.perftests.Constants
import ru.yandex.market.perftests.Owners
import ru.yandex.market.perftests.dsl.findAddToCartInCmsRootRecycler
import ru.yandex.market.perftests.dsl.findCartDeleteItemButton
import ru.yandex.market.perftests.dsl.findCheckoutButton
import ru.yandex.market.perftests.dsl.findNavigateToCartButton
import ru.yandex.market.perftests.dsl.findNavigateToMainButton
import ru.yandex.market.perftests.dsl.getCheckoutRootRecycler
import ru.yandex.market.perftests.dsl.login
import ru.yandex.market.perftests.dsl.longWaitForIdle
import ru.yandex.market.perftests.dsl.pressBack
import ru.yandex.market.perftests.dsl.startMainActivity
import ru.yandex.market.perftests.dsl.waitBeforeMeasure
import ru.yandex.market.perftests.dsl.waitCartActualization
import ru.yandex.market.perftests.dsl.waitCheckoutRootRecycler
import ru.yandex.market.perftests.dsl.waitForIdle
import ru.yandex.market.perftests.dsl.waitFrameMetrics
import ru.yandex.market.perftests.scenario.MarketBasePerfTest

@PerfTest(
    packageName = Constants.PACKAGE_NAME,
    description = "CheckoutScreenFpsTest",
    timeoutSeconds = Constants.LONG_TIMEOUT,
    owners = [Owners.DMTRIY_DOGAEV]
)
@RunWith(PerfTestJUnit4Runner::class)
class CheckoutScreenFpsTest : MarketBasePerfTest() {

    override fun beforeAllTests() {
        super.beforeAllTests()
        login()
    }

    override fun scenario() {
        startMainActivity()
        findNavigateToMainButton().click()
        waitForIdle()

        findAddToCartInCmsRootRecycler().click()
        findNavigateToCartButton().clickAndWaitForNewWindow()

        waitCartActualization()
        findCheckoutButton().clickAndWaitForNewWindow()
        waitCheckoutRootRecycler()
        waitBeforeMeasure()

        val recycler = getCheckoutRootRecycler()
        repeat(SCROLL_COUNT) {
            recycler.scrollForward()
        }
        repeat(SCROLL_COUNT) {
            recycler.scrollBackward()
        }

        pressBack()
        findCartDeleteItemButton().click()
        longWaitForIdle()
        waitFrameMetrics("checkout_screen_rv")
    }

    companion object {
        private const val SCROLL_COUNT = 4
    }
}