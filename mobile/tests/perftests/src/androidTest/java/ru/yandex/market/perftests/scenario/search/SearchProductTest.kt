package ru.yandex.market.perftests.scenario.search

import com.yandex.perftests.runner.PerfTest
import com.yandex.perftests.runner.PerfTestJUnit4Runner
import org.junit.runner.RunWith
import ru.yandex.market.perftests.Constants
import ru.yandex.market.perftests.Owners
import ru.yandex.market.perftests.dsl.findIphone12
import ru.yandex.market.perftests.dsl.measure
import ru.yandex.market.perftests.dsl.waitBeforeMeasure
import ru.yandex.market.perftests.dsl.waitMediaCarousel
import ru.yandex.market.perftests.dsl.waitProductRootRecycler

@PerfTest(
    packageName = Constants.PACKAGE_NAME,
    description = "SearchProductTest",
    timeoutSeconds = Constants.LONG_TIMEOUT,
    owners = [Owners.KIRILL_SOKOLOV]
)
@RunWith(PerfTestJUnit4Runner::class)
class SearchProductTest : SearchBaseTest() {

    override fun scenario() {
        super.scenario()

        val appleSaggest = findIphone12()

        waitBeforeMeasure()
        measure("search_product") {
            appleSaggest.click()
            waitProductRootRecycler()
            waitMediaCarousel()
        }
    }
}