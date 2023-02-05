package ru.yandex.market.perftests.scenario

import com.yandex.perftests.runner.PerfTest
import com.yandex.perftests.runner.PerfTestJUnit4Runner
import org.junit.runner.RunWith
import ru.yandex.market.perftests.Constants
import ru.yandex.market.perftests.Owners
import ru.yandex.market.perftests.dsl.findProductImageInCmsRootRecycler
import ru.yandex.market.perftests.dsl.measure
import ru.yandex.market.perftests.dsl.startMainActivity
import ru.yandex.market.perftests.dsl.waitBeforeMeasure
import ru.yandex.market.perftests.dsl.waitMediaCarousel
import ru.yandex.market.perftests.dsl.waitProductRootRecycler

@PerfTest(
    packageName = Constants.PACKAGE_NAME,
    description = "OpenProductBaseTest",
    timeoutSeconds = Constants.LONG_TIMEOUT,
    owners = [Owners.DMTRIY_DOGAEV]
)
@RunWith(PerfTestJUnit4Runner::class)
class OpenProductBaseTest : MarketBasePerfTest() {

    override fun scenario() {
        startMainActivity()
        waitMediaCarousel()
        val productImage = findProductImageInCmsRootRecycler()
        waitBeforeMeasure()
        measure("open_product_screen") {
            productImage.click()
            waitProductRootRecycler()
            waitMediaCarousel()
        }
    }
}