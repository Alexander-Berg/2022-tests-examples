package ru.yandex.direct.ui.adapter

import android.app.Application
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.verify
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.yandex.direct.R
import ru.yandex.direct.domain.FundsAmount
import ru.yandex.direct.domain.banners.ShortBannerPhraseInfo
import ru.yandex.direct.domain.enums.PhrasesSelector
import ru.yandex.direct.ui.callback.ValueValidator
import ru.yandex.direct.ui.fragment.pricemaster.tab.PriceByPhrasesItem
import ru.yandex.direct.util.functional.Converter
import ru.yandex.direct.utils.CurrencyInitializer


/**
 * @Config annotation should prevent MultiDEX deploy errors.
 * Use it if you encounter "java.lang.RuntimeException: MultiDex installation failed".
 * The solution is that a plain Application should be used here instead of the YandexDirectApp, because
 * MultiDexApplication (from which the YandexDirectApp is inherited) is not available in the Robolectric test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [Build.VERSION_CODES.P])
class PhrasePriceAdapterTest {
    private lateinit var mView: View

    private lateinit var mValueValidator: ValueValidator<String>

    private fun buildHolder(selector: PhrasesSelector): PhrasePriceAdapter.PhrasePriceContentViewHolder {
        val holder = PhrasePriceAdapter.PhrasePriceContentViewHolder(
            mView, mValueValidator,
            selector, null, currency, true,
            Converter<Long, PriceByPhrasesItem> { PriceByPhrasesItem(0, price, contextPrice) }
        )

        holder.searchPrice = spy(holder.searchPrice)
        holder.searchPricesHint = spy(holder.searchPricesHint)
        holder.searchPricesView = spy(holder.searchPricesView)
        holder.adNetworkPrice = spy(holder.adNetworkPrice)
        holder.adNetworkPricesHint = spy(holder.adNetworkPricesHint)
        holder.adNetworkPricesView = spy(holder.adNetworkPricesView)
        holder.title = spy(holder.title)
        holder.bottomDivider = spy(holder.bottomDivider)
        holder.bottomSpace = spy(holder.bottomSpace)

        return holder
    }

    @Before
    fun runBeforeEachTest() {
        mValueValidator = mock()
        mView = LayoutInflater.from(RuntimeEnvironment.application)
                .inflate(R.layout.item_phrase_price_master, null)
    }

    @Test
    fun bind_shouldShowNetworkPrices_withData() {
        buildHolder(PhrasesSelector.AdNetwork).apply {
            val item = ShortBannerPhraseInfo().apply {
                isRarelyServed = false
                phrase = PhrasePriceAdapterTest.phrase
            }

            setDividerIsVisible(false)

            bind(RuntimeEnvironment.application.resources, item)

            verify(title).text = phrase
            verify(adNetworkPrice).setPrice(contextPrice, currency)
            verify(adNetworkPricesHint).visibility = View.GONE
            verify(adNetworkPricesView).visibility = View.VISIBLE
            verify(adNetworkPricesView).showOnNetworkPrices(item.contextCoverage, currency)
            verify(bottomDivider).visibility = View.INVISIBLE
            verify(bottomSpace).visibility = View.VISIBLE
        }
    }

    @Test
    fun bind_shouldShowRarelyServedHint_ifNetworkAndRarelyServed() {
        buildHolder(PhrasesSelector.AdNetwork).apply {
            val item = ShortBannerPhraseInfo().apply {
                isRarelyServed = true
                phrase = PhrasePriceAdapterTest.phrase
            }

            setDividerIsVisible(true)

            bind(RuntimeEnvironment.application.resources, item)

            verify(title).text = phrase
            verify(adNetworkPrice).setPrice(contextPrice, currency)
            verify(adNetworkPricesHint).visibility = View.VISIBLE
            verify(adNetworkPricesView).visibility = View.GONE
            verify(adNetworkPricesView, never()).showOnNetworkPrices(item.contextCoverage, currency)
            verify(adNetworkPricesHint).setText(R.string.phrase_hint_rarely_served)
            verify(bottomDivider).visibility = View.VISIBLE
            verify(bottomSpace).visibility = View.GONE
        }
    }

    @Test
    fun bind_shouldShowSearchPrices_withData() {
        buildHolder(PhrasesSelector.Search).apply {
            val item = ShortBannerPhraseInfo().apply {
                isRarelyServed = false
                phrase = PhrasePriceAdapterTest.phrase
            }

            setDividerIsVisible(false)

            bind(RuntimeEnvironment.application.resources, item)

            verify(title).text = phrase
            verify(searchPrice).setPrice(price, currency)
            verify(searchPricesHint).visibility = View.GONE
            verify(searchPricesView).visibility = View.VISIBLE
            verify(searchPricesView).showOnSearchPrices(item.auctionBids, currency)
            verify(bottomDivider).visibility = View.INVISIBLE
            verify(bottomSpace).visibility = View.VISIBLE
        }
    }

    @Test
    fun bind_shouldShowRarelyServedHint_ifSearchAndRarelyServed() {
        buildHolder(PhrasesSelector.Search).apply {
            val item = ShortBannerPhraseInfo().apply {
                isRarelyServed = true
                phrase = PhrasePriceAdapterTest.phrase
            }

            setDividerIsVisible(false)

            bind(RuntimeEnvironment.application.resources, item)

            verify(title).text = phrase
            verify(searchPrice).setPrice(price, currency)
            verify(searchPricesHint).visibility = View.VISIBLE
            verify(searchPricesHint).setText(R.string.phrase_hint_rarely_served)
            verify(searchPricesView).visibility = View.GONE
            verify(searchPricesView, never()).showOnSearchPrices(item.auctionBids, currency)
            verify(bottomDivider).visibility = View.INVISIBLE
            verify(bottomSpace).visibility = View.VISIBLE
        }
    }

    @Test
    fun bind_searchAndNetwork_shouldBehave_exactlyAsJustSearch() {
        buildHolder(PhrasesSelector.SearchAndAdNetwork).apply {
            val item = ShortBannerPhraseInfo().apply {
                isRarelyServed = false
                phrase = PhrasePriceAdapterTest.phrase
            }

            setDividerIsVisible(false)

            bind(RuntimeEnvironment.application.resources, item)

            verify(title).text = phrase
            verify(searchPrice).setPrice(price, currency)
            verify(searchPricesHint).visibility = View.GONE
            verify(searchPricesView).visibility = View.VISIBLE
            verify(searchPricesView).showOnSearchPrices(item.auctionBids, currency)
            verify(bottomDivider).visibility = View.INVISIBLE
            verify(bottomSpace).visibility = View.VISIBLE
        }
    }

    companion object {
        private val price: FundsAmount = FundsAmount.fromLong(1)
        private val contextPrice: FundsAmount = FundsAmount.fromLong(2)
        private const val currency: String = "CURRENCY"
        private const val phrase: String = "PHRASE"

        @BeforeClass
        @JvmStatic
        fun runBeforeAllTests() {
            CurrencyInitializer.injectTestDataInStaticFields()
        }
    }
}