package ru.yandex.market.perftests.scenario

import com.yandex.perftests.runner.PerfTest
import com.yandex.perftests.runner.PerfTestJUnit4Runner
import org.junit.runner.RunWith
import ru.yandex.market.perftests.Constants
import ru.yandex.market.perftests.Owners
import ru.yandex.market.perftests.dsl.findNavigateToCartButton
import ru.yandex.market.perftests.dsl.login
import ru.yandex.market.perftests.dsl.measure
import ru.yandex.market.perftests.dsl.startMainActivity
import ru.yandex.market.perftests.dsl.waitBeforeMeasure
import ru.yandex.market.perftests.dsl.waitCartProductImage
import ru.yandex.market.perftests.dsl.waitForIdle

@PerfTest(
    packageName = Constants.PACKAGE_NAME,
    description = "OpenCartTest",
    timeoutSeconds = Constants.LONG_TIMEOUT,
    owners = [Owners.DMTRIY_DOGAEV]
)
@RunWith(PerfTestJUnit4Runner::class)
class OpenCartTest : MarketBasePerfTest() {

    override fun beforeAllTests() {
        super.beforeAllTests()
        login()
    }

    override fun scenario() {
        startMainActivity()
        waitForIdle()

        val navigateToCartButton = findNavigateToCartButton()

        waitBeforeMeasure()
        measure("cart_shown") {
            navigateToCartButton.click()
            waitCartProductImage()
        }
    }
}