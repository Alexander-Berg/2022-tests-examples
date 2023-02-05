package ru.yandex.market.perftests.scenario

import com.yandex.perftests.runner.PerfTest
import com.yandex.perftests.runner.PerfTestJUnit4Runner
import org.junit.runner.RunWith
import ru.yandex.market.perftests.Constants
import ru.yandex.market.perftests.Owners
import ru.yandex.market.perftests.dsl.findAddToCartInCmsRootRecycler
import ru.yandex.market.perftests.dsl.findCartDeleteItemButton
import ru.yandex.market.perftests.dsl.findCheckoutButton
import ru.yandex.market.perftests.dsl.findNavigateToCartButton
import ru.yandex.market.perftests.dsl.longWaitForIdle
import ru.yandex.market.perftests.dsl.measure
import ru.yandex.market.perftests.dsl.pressBack
import ru.yandex.market.perftests.dsl.startMainActivity
import ru.yandex.market.perftests.dsl.waitBeforeMeasure
import ru.yandex.market.perftests.dsl.waitCheckoutMap
import ru.yandex.market.perftests.dsl.waitMediaCarousel

@PerfTest(
    packageName = Constants.PACKAGE_NAME,
    description = "OpenCheckoutFirstFlowTest",
    timeoutSeconds = Constants.LONG_TIMEOUT,
    owners = [Owners.KIRILL_SOKOLOV]
)
@RunWith(PerfTestJUnit4Runner::class)
class OpenCheckoutFirstFlowTest : MarketBasePerfTest() {

    override fun scenario() {
        startMainActivity()
        waitMediaCarousel()
        val addToCartButton = findAddToCartInCmsRootRecycler()
        addToCartButton.click()
        val navigateToCartButton = findNavigateToCartButton()
        navigateToCartButton.click()
        val checkoutButton = findCheckoutButton()
        longWaitForIdle()
        waitBeforeMeasure()
        measure("open_checkout_first_flow_test") {
            checkoutButton.click()
            waitCheckoutMap()
        }
        pressBack()
        longWaitForIdle()
        findCartDeleteItemButton().click()
        longWaitForIdle()
    }
}