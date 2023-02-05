package ru.yandex.market.clean.presentation.feature.hyperlocal.map

import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.analytics.facades.HyperlocalAnalytics
import ru.yandex.market.checkout.summary.AddressFormatter
import ru.yandex.market.clean.data.mapper.AddressMapper
import ru.yandex.market.clean.presentation.feature.checkout.map.MapPinView
import ru.yandex.market.clean.presentation.feature.map.SearchAddressItemFormatter
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.domain.hyperlocal.model.HyperlocalAddress
import ru.yandex.market.domain.models.region.GeoCoordinates
import ru.yandex.market.domain.useraddress.model.UserAddress
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.presentationSchedulersMock

class HyperlocalMapPresenterTest {

    private val hyperlocalAddress: HyperlocalAddress.Exists.Expired = HyperlocalAddress.Exists.Expired(
        coordinates = GeoCoordinates(0.0, 0.0),
        userAddress = UserAddress(regionId = 123, country = "Россия", city = "Москва", house = "Дом")
    )
    private val schedulers = presentationSchedulersMock()
    private val router = mock<Router>()
    private val resourcesManager: ResourcesManager = mock()
    private val addressItemFormatter = mock<SearchAddressItemFormatter>()
    private val useCases = mock<HyperlocalMapUseCases> {
        on { getCurrentHyperlocalAddressUseCase() } doReturn Single.just(hyperlocalAddress as HyperlocalAddress)
        on { setCurrentHyperlocalAddressUseCase(any(), any()) } doReturn Completable.complete()
    }
    private val args =
        HyperlocalMapFragment.Arguments()
    private val addressMapper: AddressMapper = mock()
    private val addressFormatter: AddressFormatter = mock()
    private val hyperlocalAnalytics = mock<HyperlocalAnalytics>()
    private val presenter =
        HyperlocalMapPresenter(
            schedulers,
            router,
            args,
            resourcesManager,
            useCases,
            addressItemFormatter,
            addressMapper,
            addressFormatter,
            hyperlocalAnalytics
        )

    private val view = mock<HyperlocalMapView>()

    @Test
    fun `check initial`() {
        presenter.attachView(view)
        verify(view).moveMapToCoordinates(eq(hyperlocalAddress.coordinates), any())
        verify(view).showMap()
        verify(view).showPin(MapPinView.State.Standard)
        verify(view).showFindMeButton()
        verify(view).showOrderItems(emptyList())
    }
}