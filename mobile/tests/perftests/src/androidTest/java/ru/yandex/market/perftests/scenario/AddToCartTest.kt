package ru.yandex.market.perftests.scenario

import com.yandex.perftests.runner.PerfTest
import com.yandex.perftests.runner.PerfTestJUnit4Runner
import org.junit.runner.RunWith
import ru.yandex.market.perftests.Constants
import ru.yandex.market.perftests.Owners
import ru.yandex.market.perftests.dsl.findAddToCartInCmsRootRecycler
import ru.yandex.market.perftests.dsl.findCartDeleteItemButton
import ru.yandex.market.perftests.dsl.findNavigateToCartButton
import ru.yandex.market.perftests.dsl.longWaitForIdle
import ru.yandex.market.perftests.dsl.measure
import ru.yandex.market.perftests.dsl.startMainActivity
import ru.yandex.market.perftests.dsl.waitBeforeMeasure
import ru.yandex.market.perftests.dsl.waitCartProductImage
import ru.yandex.market.perftests.dsl.waitMediaCarousel

@PerfTest(
    packageName = Constants.PACKAGE_NAME,
    description = "AddToCartTest",
    timeoutSeconds = Constants.LONG_TIMEOUT,
    owners = [Owners.KIRILL_SOKOLOV]
)
@RunWith(PerfTestJUnit4Runner::class)
class AddToCartTest : MarketBasePerfTest() {

    override fun scenario() {
        startMainActivity()
        waitMediaCarousel()
        findAddToCartInCmsRootRecycler().clickAndWaitForNewWindow()

        val navigateToCartButton = findNavigateToCartButton()

        waitBeforeMeasure()
        measure("add_to_cart") {
            navigateToCartButton.click()
            waitCartProductImage()
        }
        longWaitForIdle()
        findCartDeleteItemButton().click()
        longWaitForIdle()
    }
}