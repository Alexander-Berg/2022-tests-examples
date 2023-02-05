package ru.yandex.market.clean.presentation.feature.hyperlocal.nodelivery

import io.reactivex.Single
import org.junit.Test
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

class HyperlocalNoDeliveryDialogPresenterTest {

    private val hyperlocalAddress: HyperlocalAddress = HyperlocalAddress.Exists.Actual(
        coordinates = GeoCoordinates(0.0, 0.0),
        userAddress = UserAddress(regionId = 123, country = "Россия", city = "Москва", house = "Дом")
    )
    private val state = HyperlocalNoDeliveryDialogState(
        productImage = null,
        showErrorImage = false,
        titleText = "Экспресс доставка недоступна",
        titleCentered = false,
        infoText = "Москва",
        infoCentered = false
    )

    private val schedulers = presentationSchedulersMock()
    private val router = mock<Router>()
    private val useCases = mock<HyperlocalNoDeliveryDialogUseCases> {
        on { getCurrentHyperlocalAddress() } doReturn Single.just(hyperlocalAddress)
    }
    private val args =
        HyperlocalNoDeliveryDialogFragment.Arguments(source = HyperlocalNoDeliverySource.ExpressDelivery.Common)
    private val formatter = mock<HyperlocalNoDeliveryStateFormatter> {
        on { format(hyperlocalAddress, args.source) } doReturn state
    }
    private val analytics = mock<HyperlocalAnalytics>()

    private val presenter = HyperlocalNoDeliveryDialogPresenter(
        schedulers = schedulers,
        args = args,
        router = router,
        useCases = useCases,
        formatter = formatter,
        hyperlocalAnalytics = analytics
    )

    private val view = mock<HyperlocalNoDeliveryDialogView>()

    @Test
    fun `check show state on attach`() {
        presenter.attachView(view)
        verify(view).setState(state)

    }

    @Test
    fun `check navigate to map on button press`() {
        presenter.attachView(view)
        presenter.onConfirm()
        verify(router).navigateTo(isA<HyperlocalMapDialogTargetScreen>())
        verify(view).close()
    }

}