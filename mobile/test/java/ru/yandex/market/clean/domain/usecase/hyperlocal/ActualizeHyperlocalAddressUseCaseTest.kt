package ru.yandex.market.clean.domain.usecase.hyperlocal

import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.clean.domain.usecase.checkout.EnableLocationUseCase
import ru.yandex.market.domain.hyperlocal.model.HyperlocalAddress
import ru.yandex.market.domain.hyperlocal.model.HyperlocalAddressActualizationStatus
import ru.yandex.market.domain.models.region.geoCoordinatesTestInstance
import ru.yandex.market.domain.useraddress.model.userAddressTestInstance
import ru.yandex.market.mockResult

class ActualizeHyperlocalAddressUseCaseTest {

    private val getHyperlocalAddressUseCase = mock<GetHyperlocalAddressUseCase>()
    private val enableLocationUseCase = mock<EnableLocationUseCase>()
    private val checkHyperlocalAddressIsNearGeoUseCase = mock<CheckHyperlocalAddressIsNearGeoUseCase>()
    private val getHyperlocalAddressByGeoLocationUseCase = mock<GetHyperlocalAddressByGeoLocationUseCase>()
    private val setRawHyperlocalAddressUseCase = mock<SetRawHyperlocalAddressUseCase>()

    private val actualizeHyperlocalAddressUseCase = ActualizeHyperlocalAddressUseCase(
        getHyperlocalAddressUseCase,
        enableLocationUseCase,
        checkHyperlocalAddressIsNearGeoUseCase,
        getHyperlocalAddressByGeoLocationUseCase,
        setRawHyperlocalAddressUseCase,
    )

    @Test
    fun `actualization by geo from empty address`() {
        getHyperlocalAddressUseCase.execute().mockResult(Single.just(HyperlocalAddress.Absent))

        enableLocationUseCase.isLocationEnabled().mockResult(Single.just(true))
        enableLocationUseCase.enableLocation().mockResult(Single.just(true))

        getHyperlocalAddressByGeoLocationUseCase.execute(any(), any())
            .mockResult(Single.just(HYPERLOCAL_ADDRESS_EXPIRED))

        setRawHyperlocalAddressUseCase.execute(any()).mockResult(Completable.complete())

        actualizeHyperlocalAddressUseCase.execute()
            .test()
            .assertNoErrors()
            .assertResult(
                HyperlocalAddressActualizationStatus(
                    needsUserActualization = true,
                    isGeoActualization = true
                )
            )

        verify(setRawHyperlocalAddressUseCase).execute(HYPERLOCAL_ADDRESS_EXPIRED)
    }

    @Test
    fun `actualize by geo from expired address, near`() {
        getHyperlocalAddressUseCase.execute().mockResult(Single.just(HYPERLOCAL_ADDRESS_EXPIRED))

        enableLocationUseCase.isLocationEnabled().mockResult(Single.just(true))
        enableLocationUseCase.enableLocation().mockResult(Single.just(true))

        checkHyperlocalAddressIsNearGeoUseCase.execute(any()).mockResult(Single.just(true))

        setRawHyperlocalAddressUseCase.execute(any()).mockResult(Completable.complete())

        actualizeHyperlocalAddressUseCase.execute()
            .test()
            .assertNoErrors()
            .assertResult(
                HyperlocalAddressActualizationStatus(
                    needsUserActualization = false,
                    isGeoActualization = true
                )
            )

        verify(checkHyperlocalAddressIsNearGeoUseCase).execute(HYPERLOCAL_ADDRESS_EXPIRED)
        verify(setRawHyperlocalAddressUseCase).execute(HYPERLOCAL_ADDRESS_ACTUAL)
    }

    @Test
    fun `actualize by geo from expired address, far`() {
        getHyperlocalAddressUseCase.execute().mockResult(Single.just(HYPERLOCAL_ADDRESS_EXPIRED))

        enableLocationUseCase.isLocationEnabled().mockResult(Single.just(true))
        enableLocationUseCase.enableLocation().mockResult(Single.just(true))

        checkHyperlocalAddressIsNearGeoUseCase.execute(any()).mockResult(Single.just(false))

        actualizeHyperlocalAddressUseCase.execute()
            .test()
            .assertNoErrors()
            .assertResult(
                HyperlocalAddressActualizationStatus(
                    needsUserActualization = true,
                    isGeoActualization = true
                )
            )

        verify(checkHyperlocalAddressIsNearGeoUseCase).execute(HYPERLOCAL_ADDRESS_EXPIRED)
    }

    @Test
    fun `omit actualization on actual hyperlocal address`() {
        getHyperlocalAddressUseCase.execute().mockResult(Single.just(HYPERLOCAL_ADDRESS_ACTUAL))

        actualizeHyperlocalAddressUseCase.execute()
            .test()
            .assertNoErrors()
            .assertResult(
                HyperlocalAddressActualizationStatus(
                    needsUserActualization = false,
                    isGeoActualization = true
                )
            )
    }

    companion object {
        private val COORDINATES = geoCoordinatesTestInstance()
        private val USER_ADDRESS = userAddressTestInstance().copy(coordinates = COORDINATES)
        private val HYPERLOCAL_ADDRESS_EXPIRED = HyperlocalAddress.Exists.Expired(COORDINATES, USER_ADDRESS)
        private val HYPERLOCAL_ADDRESS_ACTUAL = HyperlocalAddress.Exists.Actual(COORDINATES, USER_ADDRESS)
    }

}