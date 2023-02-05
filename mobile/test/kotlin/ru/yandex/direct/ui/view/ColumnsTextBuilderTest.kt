package ru.yandex.direct.ui.view

import android.content.Context
import android.content.res.Resources
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyVararg
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import ru.yandex.direct.R
import ru.yandex.direct.domain.FundsAmount
import ru.yandex.direct.domain.banners.CoverageInfo
import ru.yandex.direct.utils.CurrencyInitializer
import ru.yandex.direct.web.api5.bids.AuctionBid

class ColumnsTextBuilderTest {
    private lateinit var mBuilder: PhrasePricesView.ColumnsTextBuilder

    @Before
    fun runBeforeEachTest() {
        val resources = mock<Resources> {
            on { getIdentifier(any(), any(), any()) } doReturn 0
        }
        val context = mock<Context> {
            on { getString(R.string.long_dash) } doReturn DASH
            on { getString(eq(R.string.format_price_with_currency), anyVararg()) } doReturn CURRENCY
            on { getResources() } doReturn resources
        }
        mBuilder = PhrasePricesView.ColumnsTextBuilder(context, CURRENCY)
    }

    @Test
    fun appendAuctionBid_shouldAppendDash_ifAuctionBidIsNull() {
        mBuilder.append(0, null as AuctionBid?)

        assertThat(mBuilder.firstColumnText).isEqualTo("0")
        assertThat(mBuilder.secondColumnText).isEqualTo(DASH)
        assertThat(mBuilder.thirdColumnText).isEqualTo(DASH)
    }

    @Test
    fun appendAuctionBid_shouldAppendValue_ifAuctionBidIsNotNull() {
        mBuilder.append(0, AuctionBid(0, amount(0), amount(0)))

        assertThat(mBuilder.firstColumnText).isEqualTo("0")
        assertThat(mBuilder.secondColumnText).isEqualTo(CURRENCY)
        assertThat(mBuilder.thirdColumnText).isEqualTo(CURRENCY)
    }

    @Test
    fun appendAuctionBid_shouldJoinValuesWithNewLine_ifGotMultipleAuctionBids() {
        mBuilder.append(0, AuctionBid(0, amount(0), amount(0)))
        mBuilder.append(1, AuctionBid(1, amount(1), amount(1)))

        assertThat(mBuilder.firstColumnText).isEqualTo("0\n1")
        assertThat(mBuilder.secondColumnText).isEqualTo("$CURRENCY\n$CURRENCY")
        assertThat(mBuilder.thirdColumnText).isEqualTo("$CURRENCY\n$CURRENCY")
    }

    @Test
    fun appendAuctionBid_shouldWorkAfter_appendCoverageInfo() {
        mBuilder.append(0.0, CoverageInfo(0.0, amount(0)))
        mBuilder.append(1, AuctionBid(1, amount(1), amount(1)))

        assertThat(mBuilder.firstColumnText).isEqualTo("\n1")
        assertThat(mBuilder.secondColumnText).isEqualTo("0%\n$CURRENCY")
        assertThat(mBuilder.thirdColumnText).isEqualTo("$CURRENCY\n$CURRENCY")
    }

    @Test
    fun appendCoverageInfo_shouldWorkAfter_appendAuctionBid() {
        mBuilder.append(1, AuctionBid(1, amount(1), amount(1)))
        mBuilder.append(0.0, CoverageInfo(0.0, amount(0)))

        assertThat(mBuilder.firstColumnText).isEqualTo("1\n")
        assertThat(mBuilder.secondColumnText).isEqualTo("$CURRENCY\n0%")
        assertThat(mBuilder.thirdColumnText).isEqualTo("$CURRENCY\n$CURRENCY")
    }

    @Test
    fun appendCoverageInfo_shouldAppendDash_ifCoverageInfoIsNull() {
        mBuilder.append(0.0, null as CoverageInfo?)

        assertThat(mBuilder.firstColumnText).isEqualTo("")
        assertThat(mBuilder.secondColumnText).isEqualTo("0%")
        assertThat(mBuilder.thirdColumnText).isEqualTo(DASH)
    }

    @Test
    fun appendCoverageInfo_shouldAppendPrice_ifCoverageInfoIsNotNull() {
        mBuilder.append(0.0, CoverageInfo(0.0, amount(1)))

        assertThat(mBuilder.firstColumnText).isEqualTo("")
        assertThat(mBuilder.secondColumnText).isEqualTo("0%")
        assertThat(mBuilder.thirdColumnText).isEqualTo(CURRENCY)
    }

    @Test
    fun appendCoverageInfo_shouldJoinValuesWithNewLine_ifMultipleValues() {
        mBuilder.append(0.0, CoverageInfo(0.0, amount(1)))
        mBuilder.append(1.0, CoverageInfo(1.0, amount(2)))

        assertThat(mBuilder.firstColumnText).isEqualTo("\n")
        assertThat(mBuilder.secondColumnText).isEqualTo("0%\n1%")
        assertThat(mBuilder.thirdColumnText).isEqualTo("$CURRENCY\n$CURRENCY")
    }

    private fun amount(amount: Number) = FundsAmount.fromDouble(amount.toDouble())

    companion object {
        const val CURRENCY = "RUB"
        const val DASH = "-"

        @BeforeClass
        @JvmStatic
        fun runBeforeAllTests() {
            CurrencyInitializer.injectTestDataInStaticFields()
        }
    }
}