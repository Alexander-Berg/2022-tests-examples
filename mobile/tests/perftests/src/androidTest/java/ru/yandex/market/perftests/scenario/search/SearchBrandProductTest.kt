package ru.yandex.market.perftests.scenario.search

import com.yandex.perftests.runner.PerfTest
import com.yandex.perftests.runner.PerfTestJUnit4Runner
import org.junit.runner.RunWith
import ru.yandex.market.perftests.Constants
import ru.yandex.market.perftests.Owners
import ru.yandex.market.perftests.dsl.findAppleBrand
import ru.yandex.market.perftests.dsl.findProductImageInSearchRecycler
import ru.yandex.market.perftests.dsl.longWaitForIdle
import ru.yandex.market.perftests.dsl.measure
import ru.yandex.market.perftests.dsl.waitBeforeMeasure
import ru.yandex.market.perftests.dsl.waitForIdle
import ru.yandex.market.perftests.dsl.waitMediaCarousel
import ru.yandex.market.perftests.dsl.waitProductRootRecycler

@PerfTest(
    packageName = Constants.PACKAGE_NAME,
    description = "SearchBrandProductTest",
    timeoutSeconds = Constants.LONG_TIMEOUT,
    owners = [Owners.KIRILL_SOKOLOV]
)
@RunWith(PerfTestJUnit4Runner::class)
class SearchBrandProductTest : SearchBaseTest() {

    override fun scenario() {
        super.scenario()

        val appleSadjest = findAppleBrand()
        waitForIdle()
        appleSadjest.clickAndWaitForNewWindow()

        val productImage = findProductImageInSearchRecycler()

        waitBeforeMeasure()
        measure("search_brand_product") {
            productImage.click()
            waitProductRootRecycler()
            waitMediaCarousel()
        }
    }
}