package ru.yandex.market.clean.presentation.feature.hyperlocal.address

import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.isA
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.analytics.facades.HyperlocalAnalytics
import ru.yandex.market.clean.presentation.feature.hyperlocal.map.dialog.HyperlocalMapDialogTargetScreen
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.domain.hyperlocal.model.HyperlocalAddress
import ru.yandex.market.domain.models.region.GeoCoordinates
import ru.yandex.market.domain.useraddress.model.UserAddress
import ru.yandex.market.presentationSchedulersMock

class HyperlocalAddressDialogPresenterTest {

    private val hyperlocalAddress: HyperlocalAddress = HyperlocalAddress.Exists.Expired(
        coordinates = GeoCoordinates(0.0, 0.0),
        userAddress = UserAddress(regionId = 123, country = "Россия", city = "Москва", house = "Дом")
    )
    private val schedulers = presentationSchedulersMock()
    private val router = mock<Router>()
    private val useCases = mock<HyperlocalAddressDialogUseCases> {
        on { getCurrentHyperlocalAddress() } doReturn Single.just(hyperlocalAddress)
        on { setHyperlocalAddress(any(), any()) } doReturn Completable.complete()
        on { getUserAddressList() } doReturn Single.just(emptyList())
    }
    private val args =
        HyperlocalAddressDialogFragment.Arguments(
            images = emptySet(),
            from = null,
        )
    private val state = HyperlocalAddressDialogState.NeedConfirmation.ExpiredAddress("Москва, Дом")
    private val analytics = mock<HyperlocalAnalytics>()
    private val formatter = mock<HyperlocalAddressStateFormatter>() {
        on { format(hyperlocalAddress, emptyList(), false) } doReturn state
    }

    private val view = mock<HyperlocalAddressDialogView>()

    private val presenter = HyperlocalAddressDialogPresenter(
        schedulers = schedulers,
        args = args,
        router = router,
        useCases = useCases,
        formatter = formatter,
        hyperlocalAnalytics = analytics
    )

    @Test
    fun `show state on attach`() {
        presenter.attachView(view)
        verify(view).setState(state)
    }

    @Test
    fun `close view on confirm`() {
        presenter.attachView(view)
        presenter.onConfirmAddress()
        verify(view).close()
    }

    @Test
    fun `open map and close view on change address`() {
        presenter.attachView(view)
        presenter.onNewAddressInput()
        verify(router).navigateTo(isA<HyperlocalMapDialogTargetScreen>())
        verify(view).close()
    }
}