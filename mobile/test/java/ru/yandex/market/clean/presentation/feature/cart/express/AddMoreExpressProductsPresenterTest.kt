package ru.yandex.market.clean.presentation.feature.cart.express

import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.isA
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.beru.android.R
import ru.yandex.market.clean.domain.model.search.searchResultTestInstance
import ru.yandex.market.data.order.OrderOptionsDto
import ru.yandex.market.data.order.OrderOptionsExchange
import ru.yandex.market.data.order.description.options.orderOptionsDescriptionRequestModelTestInstance
import ru.yandex.market.domain.adult.model.AdultState
import ru.yandex.market.domain.money.model.Money
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.clean.presentation.formatter.MoneyFormatter
import ru.yandex.market.presentationSchedulersMock
import ru.yandex.market.test.extensions.arg

class AddMoreExpressProductsPresenterTest {

    private val schedulers = presentationSchedulersMock()
    private val useCases = mock<AddMoreExpressProductsUseCases> {
        on {
            getAdultState()
        } doReturn Single.just(AdultState.DISABLED)

        on {
            isLoggedIn()
        } doReturn Single.just(false)

        on {
            getSearchResult(params = any())
        } doReturn Single.just(searchResultTestInstance())

        on {
            getCachedOrderOptions()
        } doReturn Observable.just(
            OrderOptionsExchange(
                request = orderOptionsDescriptionRequestModelTestInstance(),
                response = OrderOptionsDto(),
                xMarketRequestId = null,
            )
        )
    }

    private val moneyFormatter = mock<MoneyFormatter> {
        on {
            formatPrice(any<Money>())
        } doReturn MONEY_VALUE
    }

    private val resourceDateStore = mock<ResourcesManager> {
        on {
            getFormattedString(
                eq(R.string.payed_delivery_express_full),
                isA<String>(),
                isA<String>()
            )
        } doAnswer { invocation ->
            val first = invocation.arg<String>(1)
            val second = invocation.arg<String>(2)
            "$first $second"
        }
    }

    private val presenter = AddMoreExpressProductsPresenter(
        schedulers,
        AddMoreExpressProductsArguments(SUPPLIER_ID, SUPPLIER_NAME),
        useCases,
        resourceDateStore,
        moneyFormatter
    )

    @Test
    fun `Load first page on view attach`() {
        val view = mock<AddMoreExpressProductsView>()
        presenter.attachView(view)
        verify(useCases, times(1)).getAdultState()
        verify(useCases, times(1)).getCachedOrderOptions()
        verifyGetSearchResult()

        verify(view, times(1)).setTitle(eq("$SUPPLIER_NAME $MONEY_VALUE"))
        verify(view, times(1)).showOverlayProgress()
        verify(view, times(1)).hideOverlayProgress()
        verify(view, times(1)).initializeList(eq(false), eq(false))
        verify(view, times(1)).showSearchResult(argThat { isNotEmpty() }, eq(false), eq(false))
    }

    @Test
    fun `Load first page error`() {
        whenever(useCases.getSearchResult(params = any())).thenReturn(Single.error(Exception()))

        val view = mock<AddMoreExpressProductsView>()
        presenter.attachView(view)

        verifyGetSearchResult()
        verify(view, times(1)).close()
    }

    @Test
    fun `Load first page adult state error`() {
        whenever(useCases.getAdultState()).thenReturn(Single.error(Exception()))

        val view = mock<AddMoreExpressProductsView>()
        presenter.attachView(view)

        verify(useCases, times(1)).getAdultState()
        verify(view, times(1)).close()
    }

    private fun verifyGetSearchResult() {
        verify(useCases, times(1)).getSearchResult(params = any())
    }

    companion object {

        private const val MONEY_VALUE = "100 RUB"

        private const val SUPPLIER_ID = 1L

        private const val SUPPLIER_NAME = "Test Supplier"

    }

}
