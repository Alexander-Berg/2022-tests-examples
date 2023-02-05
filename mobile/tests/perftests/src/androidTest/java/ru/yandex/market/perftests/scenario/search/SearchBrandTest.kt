package ru.yandex.market.perftests.scenario.search

import com.yandex.perftests.runner.PerfTest
import com.yandex.perftests.runner.PerfTestJUnit4Runner
import org.junit.runner.RunWith
import ru.yandex.market.perftests.Constants
import ru.yandex.market.perftests.Owners
import ru.yandex.market.perftests.dsl.findAppleBrand
import ru.yandex.market.perftests.dsl.measure
import ru.yandex.market.perftests.dsl.waitBeforeMeasure
import ru.yandex.market.perftests.dsl.waitProductImage

@PerfTest(
    packageName = Constants.PACKAGE_NAME,
    description = "SearchBrandTest",
    timeoutSeconds = Constants.LONG_TIMEOUT,
    owners = [Owners.KIRILL_SOKOLOV]
)
@RunWith(PerfTestJUnit4Runner::class)
class SearchBrandTest : SearchBaseTest() {

    override fun scenario() {
        super.scenario()

        val appleSadjest = findAppleBrand()

        waitBeforeMeasure()
        measure("search_brand") {
            appleSadjest.click()
            waitProductImage()
        }
    }
}