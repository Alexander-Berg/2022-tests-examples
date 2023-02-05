package ru.yandex.market.ui.view.mvp.cartcounterbutton.order

import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.analitycs.AnalyticsService
import ru.yandex.market.checkout.CheckoutTargetScreen
import ru.yandex.market.checkout.PreselectedOptions
import ru.yandex.market.clean.data.mapper.cart.CartItemMapper
import ru.yandex.market.clean.domain.model.OfferPromoInfo
import ru.yandex.market.clean.domain.model.offerTestInstance
import ru.yandex.market.clean.domain.model.order.OrderValidationResult
import ru.yandex.market.clean.domain.model.productOfferTestInstance
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.parcelable.money.MoneyParcelable
import ru.yandex.market.domain.money.model.Currency
import ru.yandex.market.presentationSchedulersMock
import ru.yandex.market.test.extensions.asSingle
import java.math.BigDecimal

class CartCounterOrderDelegateTest {

    private val useCases = mock<CartCounterOrderUseCases> {
        on { resetCheckoutFlow() } doReturn Completable.complete()
        on { getProductsDateInStock(any()) } doReturn Single.just(emptyMap())
        on { getOffer(anyOrNull(), any(), any()) } doReturn productOfferTestInstance().copy(
            offer = offerTestInstance(
                isExpressDelivery = false
            ),
            promoInfo = OfferPromoInfo(emptyList(), emptyList(), null),
        ).asSingle()
        on { savePromocode(null) } doReturn Completable.complete()
    }

    private val router = mock<Router>()
    private val schedulers = presentationSchedulersMock()
    private val cartItemMapper = mock<CartItemMapper>()
    private val analyticsService = mock<AnalyticsService>()

    private val delegate = CartCounterOrderDelegate(
        useCases = useCases,
        cartItemMapper = cartItemMapper,
        router = router,
        schedulers = schedulers,
        analyticsService = analyticsService,
    )

    @Test
    fun `Navigate to checkout`() {
        whenever(useCases.validateOrderWithoutPromocode(any())) doReturn OrderValidationResult.testInstance().asSingle()

        delegate.orderNavigate(
            CartCounterOrderDelegate.Params(
                null,
                "offerId",
                emptySet(),
                1,
                null,
                1,
                null,
                MoneyParcelable(BigDecimal(100), Currency.RUR),
                PreselectedOptions.NotPreselected
            ),
            {}, {}
        )
        verify(router).navigateTo(any<CheckoutTargetScreen>())

    }


}
