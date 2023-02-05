package ru.yandex.market.perftests.scenario

import androidx.annotation.CallSuper
import androidx.test.uiautomator.UiDevice
import com.yandex.perftests.runner.PerfTestAfterClass
import com.yandex.perftests.runner.PerfTestBeforeClass
import com.yandex.perftests.runner.PerfTestJUnit4Runner
import com.yandex.perftests.runner.PerfTestUtils
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.market.perftests.Constants
import ru.yandex.market.perftests.dsl.clearData
import ru.yandex.market.perftests.dsl.clearSettings
import ru.yandex.market.perftests.dsl.removeAccounts
import ru.yandex.market.perftests.dsl.setFeatureConfigs
import ru.yandex.market.perftests.dsl.setMetricaIdentifiers
import ru.yandex.market.perftests.dsl.setNavNodeDepthFeatureConfigs
import ru.yandex.market.perftests.dsl.setRandomGeneratedString
import ru.yandex.market.perftests.dsl.setRearrFactorsQueryString
import ru.yandex.market.perftests.dsl.setRegionId

@RunWith(PerfTestJUnit4Runner::class)
abstract class MarketBasePerfTest : MarketPerfTest {

    final override val packageName: String = Constants.PACKAGE_NAME

    final override val perfTestUtils: PerfTestUtils = PerfTestUtils(packageName)

    final override val device: UiDevice = perfTestUtils.device

    @PerfTestBeforeClass
    fun prepare() {
        clearData()
        removeAccounts()
        setMetricaIdentifiers(Constants.TEST_UUID, Constants.TEST_DEVICE_ID)
        setRandomGeneratedString(Constants.TEST_RANDOM_STRING)
        setRearrFactorsQueryString(getRearrFactorsQueryString())
        setRegionId(Constants.REGION_ID)
        setFeatureConfigs(listOf(setNavNodeDepthFeatureConfigs()))
        beforeAllTests()
    }

    @PerfTestAfterClass
    fun shutdown() {
        clearData()
        removeAccounts()
        clearSettings()
    }

    @CallSuper
    open fun beforeAllTests() {
    }

    @Test
    abstract fun scenario()

    open fun getRearrFactorsQueryString() = REAR_FACTOR_QUERY

    companion object {
        const val REAR_FACTOR_QUERY = "market_white_cpa_on_blue=2;" +
                "show_digital_dsbs_goods=0;market_promo_blue_flash=1;market_cashback_for_not_ya_plus=1;" +
                "sku_offers_show_all_alternative_offers=1;;" +
                "market_rebranded=1"
    }

}
