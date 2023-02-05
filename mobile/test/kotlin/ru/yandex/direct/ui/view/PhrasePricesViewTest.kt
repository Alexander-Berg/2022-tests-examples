package ru.yandex.direct.ui.view

import android.app.Application
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.yandex.direct.BuildConfig
import ru.yandex.direct.R
import ru.yandex.direct.domain.FundsAmount
import ru.yandex.direct.domain.banners.CoverageInfo
import ru.yandex.direct.utils.CurrencyInitializer
import ru.yandex.direct.web.api5.bids.AuctionBid

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [Build.VERSION_CODES.P])
class PhrasePricesViewTest {
    private lateinit var mView: PhrasePricesView

    @Before
    fun runBeforeAnyTest() {
        mView = LayoutInflater
                .from(RuntimeEnvironment.application)
                .inflate(R.layout.view_phrase_prices, null) as PhrasePricesView

        println(BuildConfig.APPLICATION_ID)
    }

    @Test
    fun showOnSearchPrices_shouldWorkWithEmptyCollection() {
        mView.showOnSearchPrices(emptyList(), null)

        assertThat(mView.firstColumn.visibility).isEqualTo(View.VISIBLE)
        assertThat(mView.secondColumn.visibility).isEqualTo(View.VISIBLE)
        assertThat(mView.thirdColumn.visibility).isEqualTo(View.VISIBLE)

        assertThat(mView.firstColumn.text).isEqualTo(expectedTrafficVolumes)
        assertThat(mView.secondColumn.text).isEqualTo(amounts(null, null, null, null))
        assertThat(mView.thirdColumn.text).isEqualTo(amounts(null, null, null, null))

        assertThat(mView.firstTitle.visibility).isEqualTo(View.VISIBLE)
        assertThat(mView.secondTitle.visibility).isEqualTo(View.VISIBLE)
        assertThat(mView.thirdTitle.visibility).isEqualTo(View.VISIBLE)

        assertThat(mView.firstTitle.text).isEqualTo(string(R.string.price_column_traffic))
        assertThat(mView.secondTitle.text).isEqualTo(string(R.string.price_column_bid))
        assertThat(mView.thirdTitle.text).isEqualTo(string(R.string.price_column_search_price))
    }

    @Test
    fun showOnSearchPrices_shouldShowOnlyFourValues_ifMaxTrafficIsLessThanOneHundred() {
        val bids = listOf(
                AuctionBid(15, amount(0), amount(0)),
                AuctionBid(5, amount(1), amount(1)),
                AuctionBid(100, amount(2), amount(2)),
                AuctionBid(75, amount(3), amount(3))
        )

        mView.showOnSearchPrices(bids, null)

        assertThat(mView.firstColumn.text).isEqualTo(expectedTrafficVolumes)
        assertThat(mView.secondColumn.text).isEqualTo(amounts(2, 3, 0, 1))
        assertThat(mView.thirdColumn.text).isEqualTo(amounts(2, 3, 0, 1))
    }

    @Test
    fun showOnSearchPrices_shouldShowDashes_forAbsentValues() {
        val bids = listOf(
                AuctionBid(5, amount(1), amount(1)),
                AuctionBid(100, amount(2), amount(2))
        )

        mView.showOnSearchPrices(bids, null)

        assertThat(mView.firstColumn.text).isEqualTo(expectedTrafficVolumes)
        assertThat(mView.secondColumn.text).isEqualTo(amounts(2, null, null, 1))
        assertThat(mView.thirdColumn.text).isEqualTo(amounts(2, null, null, 1))
    }

    @Test
    fun showOnSearchPrices_shouldShowMaxTrafficVolume_ifMaxIsGreaterThanOneHundred() {
        val bids = listOf(
                AuctionBid(15, amount(0), amount(0)),
                AuctionBid(5, amount(1), amount(1)),
                AuctionBid(100, amount(2), amount(2)),
                AuctionBid(75, amount(3), amount(3)),
                AuctionBid(149, amount(4), amount(4))
        )

        mView.showOnSearchPrices(bids, null)

        assertThat(mView.firstColumn.text).isEqualTo("149\n$expectedTrafficVolumes")
        assertThat(mView.secondColumn.text).isEqualTo(amounts(4, 2, 3, 0, 1))
        assertThat(mView.thirdColumn.text).isEqualTo(amounts(4, 2, 3, 0, 1))
    }

    @Test
    fun showOnNetworkPrices_shouldWork_withEmptyCollection() {
        mView.showOnNetworkPrices(emptyList(), null)

        assertThat(mView.firstColumn.visibility).isEqualTo(View.GONE)
        assertThat(mView.secondColumn.visibility).isEqualTo(View.VISIBLE)
        assertThat(mView.thirdColumn.visibility).isEqualTo(View.VISIBLE)

        assertThat(mView.firstColumn.text).isEqualTo("\n\n")
        assertThat(mView.secondColumn.text).isEqualTo(expectedCoverage)
        assertThat(mView.thirdColumn.text).isEqualTo(amounts(null, null, null))

        assertThat(mView.firstTitle.visibility).isEqualTo(View.GONE)
        assertThat(mView.secondTitle.visibility).isEqualTo(View.VISIBLE)
        assertThat(mView.thirdTitle.visibility).isEqualTo(View.VISIBLE)

        assertThat(mView.firstTitle.text).isEqualTo("")
        assertThat(mView.secondTitle.text).isEqualTo(string(R.string.price_column_probability))
        assertThat(mView.thirdTitle.text).isEqualTo(string(R.string.price_column_network_price))
    }

    @Test
    fun showOnNetworkPrices_shouldShowThreeHardcodedValues_ifTheyArePresent() {
        val coverage = listOf(
                CoverageInfo(20.0, amount(0)),
                CoverageInfo(50.0, amount(1)),
                CoverageInfo(100.0, amount(2)),
                CoverageInfo(42.0, amount(3)),
                CoverageInfo(1337.0, amount(4))
        )

        mView.showOnNetworkPrices(coverage, null)

        assertThat(mView.firstColumn.text).isEqualTo("\n\n")
        assertThat(mView.secondColumn.text).isEqualTo(expectedCoverage)
        assertThat(mView.thirdColumn.text).isEqualTo(amounts(2, 1, 0))
    }

    @Test
    fun showOnNetworkPrices_shouldShowDash_ifHasNoValue() {
        val coverage = listOf(
                CoverageInfo(20.0, amount(0)),
                CoverageInfo(100.0, amount(2))
        )

        mView.showOnNetworkPrices(coverage, null)

        assertThat(mView.firstColumn.text).isEqualTo("\n\n")
        assertThat(mView.secondColumn.text).isEqualTo(expectedCoverage)
        assertThat(mView.thirdColumn.text).isEqualTo(amounts(2, null, 0))
    }

    private fun string(id: Int) = RuntimeEnvironment.application.resources.getString(id)

    private fun amount(amount: Number) = FundsAmount.fromDouble(amount.toDouble())

    private fun amounts(vararg amounts: Int?) = amounts.joinToString(separator = "\n") {
        if (it == null) "—" else "$it\u00A0units"
    }

    companion object {
        const val expectedTrafficVolumes = "100\n75\n15\n5"
        const val expectedCoverage = "100%\n50%\n20%"

        @JvmStatic
        @BeforeClass
        fun runBeforeAllTests() {
            CurrencyInitializer.injectTestDataInStaticFields()
        }
    }
}