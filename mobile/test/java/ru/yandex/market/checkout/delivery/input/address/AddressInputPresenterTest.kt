package ru.yandex.market.checkout.delivery.input.address

import io.reactivex.Single
import io.reactivex.schedulers.TestScheduler
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.domain.model.deliveryLocalitySuggestionTestInstance
import ru.yandex.market.clean.presentation.formatter.StreetFormatter
import ru.yandex.market.data.passport.Address
import ru.yandex.market.domain.models.region.Country
import ru.yandex.market.domain.models.region.deliveryLocalityTestInstance
import ru.yandex.market.presentationSchedulersMock
import ru.yandex.market.utils.Duration
import ru.yandex.market.utils.advanceTimeBy
import ru.yandex.market.utils.seconds

class AddressInputPresenterTest {

    private val useCases = mock<AddressInputPresenterUseCases>()
    private val citySuggestionsMapper = mock<CitySuggestionsFormatter>()
    private val streetSuggestionsMapper = mock<StreetSuggestionsViewObjectMapper>()
    private val addressSuggestFormatter = mock<AddressSuggestFormatter>()
    private val streetFormatter = mock<StreetFormatter>()
    private val addressInputFormatter = mock<AddressInputFormatter>()
    private val view = mock<AddressInputView>()

    @Suppress("DEPRECATION")
    private val analyticsService = mock<ru.yandex.market.analitycs.AnalyticsService>()
    private val addressInputFragmentArguments = mock<AddressInputFragmentArguments>()
    private val configuration = spy(AddressInputPresenterConfiguration(Duration.EMPTY))
    private val schedulers = presentationSchedulersMock()
    private val noNameDeliveryLocality = deliveryLocalityTestInstance(name = "")

    private val presenter = AddressInputPresenter(
        schedulers,
        null,
        useCases,
        streetSuggestionsMapper,
        citySuggestionsMapper,
        configuration,
        analyticsService,
        addressInputFragmentArguments,
        addressSuggestFormatter,
        streetFormatter,
        addressInputFormatter,
    )

    @Test
    fun `Requests new city suggestions until success`() {
        val suggestions = listOf(deliveryLocalitySuggestionTestInstance())
        whenever(useCases.getDeliveryLocalitySuggestions(any()))
            .thenReturn(Single.error(RuntimeException()))
            .thenReturn(Single.error(RuntimeException()))
            .thenReturn(Single.just(suggestions))
        whenever(useCases.userSuggestUseCase).thenReturn(Single.just(emptyList()))

        val viewObjects = listOf(addressSuggestionVoTestInstance())
        whenever(citySuggestionsMapper.format(any())).thenReturn(viewObjects)
        whenever(useCases.locateDeliveryLocality())
            .thenReturn(Single.just(deliveryLocalityTestInstance()))

        presenter.attachView(view)
        presenter.requestNewCitySuggestions("Москва")

        verify(view).showCitySuggestions(viewObjects)
    }

    @Test
    fun `Asks to select city from suggestions after first load error`() {
        val timerScheduler = TestScheduler()
        whenever(schedulers.timer).thenReturn(timerScheduler)
        whenever(useCases.userSuggestUseCase).thenReturn(Single.just(emptyList()))
        val timeout = 2.seconds
        doReturn(timeout).whenever(configuration).suggestionReloadTimeout
        whenever(useCases.getDeliveryLocalitySuggestions(any()))
            .thenReturn(Single.error(RuntimeException()))

        whenever(useCases.locateDeliveryLocality()).thenReturn(Single.just(noNameDeliveryLocality))

        presenter.attachView(view)
        presenter.requestNewCitySuggestions("Москва")
        timerScheduler.advanceTimeBy(timeout)

        verify(view).showSelectSuggestedCityRequest()
    }

    @Test
    fun `Clears city error after user select city from suggestion`() {
        val suggestions = listOf(deliveryLocalitySuggestionTestInstance())
        whenever(useCases.getDeliveryLocalitySuggestions(any()))
            .thenReturn(Single.error(RuntimeException()))
            .thenReturn(Single.error(RuntimeException()))
            .thenReturn(Single.just(suggestions))
        whenever(useCases.userSuggestUseCase).thenReturn(Single.just(emptyList()))
        val viewObjects = listOf(addressSuggestionVoTestInstance())
        whenever(citySuggestionsMapper.format(any())).thenReturn(viewObjects)

        whenever(useCases.locateDeliveryLocality()).thenReturn(Single.just(noNameDeliveryLocality))

        presenter.attachView(view)
        presenter.requestNewCitySuggestions("Москва")
        presenter.selectSuggestedCity(0)

        inOrder(view) {
            verify(view).showSelectSuggestedCityRequest()
            verify(view).showCitySuggestions(viewObjects)
            verify(view).clearCityError()
        }
    }

    @Test
    fun `Pause between city suggestion requests`() {
        val timerScheduler = TestScheduler()
        val timeout = 2.seconds
        doReturn(timeout).whenever(configuration).suggestionReloadTimeout
        whenever(useCases.userSuggestUseCase).thenReturn(Single.just(emptyList()))
        whenever(schedulers.timer).doReturn(timerScheduler)
        whenever(useCases.getDeliveryLocalitySuggestions(any()))
            .thenReturn(Single.error(RuntimeException()))
        val viewObjects = listOf(addressSuggestionVoTestInstance())
        whenever(citySuggestionsMapper.format(any())).thenReturn(viewObjects)
        whenever(useCases.locateDeliveryLocality())
            .thenReturn(Single.just(deliveryLocalityTestInstance()))

        presenter.attachView(view)
        presenter.requestNewCitySuggestions("Москва")
        verify(useCases).getDeliveryLocalitySuggestions(any())

        timerScheduler.advanceTimeBy(timeout)
        verify(useCases, times(2)).getDeliveryLocalitySuggestions(any())
    }

    @Test
    fun `enrich country name after select suggested city`() {
        val suggestion = deliveryLocalitySuggestionTestInstance(
            locality = deliveryLocalityTestInstance(
                country = Country.UNKNOWN
            )
        )
        val countryName = "Russia"

        whenever(useCases.userSuggestUseCase).thenReturn(Single.just(emptyList()))
        whenever(useCases.locateDeliveryLocality()).thenReturn(Single.just(deliveryLocalityTestInstance()))
        whenever(useCases.getDeliveryLocalitySuggestions(any())).thenReturn(Single.just(listOf(suggestion)))
        whenever(useCases.getRegionCountryName(any())).thenReturn(Single.just(countryName))

        presenter.attachView(view)
        presenter.requestNewCitySuggestions("Москва")
        presenter.selectSuggestedCity(0)

        val expectedAddress1 = Address.builder()
            .regionId(suggestion.locality.regionId)
            .city(suggestion.locality.name)
            .country("")
            .street("")
            .district("")
            .house("")
            .entrance("")
            .intercom("")
            .floor("")
            .room("")
            .comment("")
            .description("")
            .build()

        val expectedAddress2 = expectedAddress1.toBuilder()
            .country(countryName)
            .build()

        inOrder(view) {
            verify(view).dispatchAddressChanged(any())
            verify(view).dispatchAddressChanged(expectedAddress1)
            verify(view).dispatchAddressChanged(expectedAddress2)
        }
    }
}
