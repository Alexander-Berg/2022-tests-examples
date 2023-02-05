package ru.yandex.market.ui.view.mvp.cartcounterbutton.hyperlocal

import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.presentation.feature.hyperlocal.address.HyperlocalAddressDialogTargetScreen
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.domain.hyperlocal.model.HyperlocalAddress
import ru.yandex.market.domain.hyperlocal.model.HyperlocalOfferAvailability
import ru.yandex.market.domain.models.region.geoCoordinatesTestInstance
import ru.yandex.market.domain.useraddress.model.userAddressTestInstance
import ru.yandex.market.presentationSchedulersMock

class CartCounterHyperlocalDelegateTest {

    private val useCases = mock<CartCounterHyperlocalUseCases>()
    private val router = mock<Router> {
        on { currentScreen } doReturn CURRENT_SCREEN
    }
    private val schedulers = presentationSchedulersMock()

    private val delegate = CartCounterHyperlocalDelegate(
        useCases = useCases,
        router = router,
        schedulers = schedulers,
    )

    @Test
    fun `Ask hyperlocal address if absent`() {
        whenever(useCases.getHyperlocalAddress()).doReturn(Single.just(HyperlocalAddress.Absent))
        whenever(useCases.observeHyperlocalAddress()).doReturn(Observable.never())
        delegate.checkHyperlocalAddressForOffer(CartCounterHyperlocalDelegate.Params(
            "1", null
        ), {}, {}, {}, true)
        verify(router).navigateTo(any<HyperlocalAddressDialogTargetScreen>())
    }


    @Test
    fun `Add offer if address actual`() {
        val persistentOfferId = "1"
        whenever(useCases.getHyperlocalAddress()).doReturn(
            Single.just(
                HyperlocalAddress.Exists.Actual(
                    geoCoordinatesTestInstance(), userAddressTestInstance()
                )
            )
        )
        whenever(useCases.actualizeLocationForOffer(persistentOfferId)).doReturn(
            Single.just(
                HyperlocalOfferAvailability(
                    true,
                    setOf(persistentOfferId),
                    emptySet()
                )
            )
        )
        val continueAddToCart = mock<()->Unit>()
        delegate.checkHyperlocalAddressForOffer(CartCounterHyperlocalDelegate.Params(
            persistentOfferId, null
        ), continueAddToCart, {}, {}, true)
        verify(continueAddToCart).invoke()
    }

    companion object {
        private val CURRENT_SCREEN = Screen.CART
    }
}