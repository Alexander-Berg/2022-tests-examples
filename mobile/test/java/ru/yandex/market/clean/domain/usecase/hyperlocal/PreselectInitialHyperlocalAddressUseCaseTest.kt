package ru.yandex.market.clean.domain.usecase.hyperlocal

import io.reactivex.Maybe
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import ru.yandex.market.domain.hyperlocal.model.HyperlocalAddress
import ru.yandex.market.domain.models.region.geoCoordinatesTestInstance
import ru.yandex.market.domain.useraddress.model.userAddressTestInstance
import ru.yandex.market.mockResult

class PreselectInitialHyperlocalAddressUseCaseTest {

    private val getHyperlocalAddressUseCase = mock<GetHyperlocalAddressUseCase>()
    private val getSuitableAddressForHyperlocalUseCase = mock<GetSuitableAddressForHyperlocalUseCase>()
    private val setRawHyperlocalAddressUseCase = mock<SetRawHyperlocalAddressUseCase>()

    private val preselectInitialHyperlocalAddressUseCase = PreselectInitialHyperlocalAddressUseCase(
        getHyperlocalAddressUseCase, getSuitableAddressForHyperlocalUseCase, setRawHyperlocalAddressUseCase
    )

    @Test
    fun `check successful preselection`() {
        getHyperlocalAddressUseCase.execute().mockResult(Single.just(HyperlocalAddress.Absent))
        getSuitableAddressForHyperlocalUseCase.execute().mockResult(Maybe.just(USER_ADDRESS))

        preselectInitialHyperlocalAddressUseCase
            .execute()
            .test()
            .assertNoErrors()
            .assertComplete()

        verify(setRawHyperlocalAddressUseCase)
            .execute(HyperlocalAddress.Exists.Expired(COORDINATES, USER_ADDRESS))
    }

    @Test
    fun `check no preselection without coordinates`() {
        getHyperlocalAddressUseCase.execute().mockResult(Single.just(HyperlocalAddress.Absent))
        getSuitableAddressForHyperlocalUseCase.execute().mockResult(Maybe.just(USER_ADDRESS.copy(coordinates = null)))

        preselectInitialHyperlocalAddressUseCase
            .execute()
            .test()
            .assertNoErrors()
            .assertComplete()

        verify(setRawHyperlocalAddressUseCase, never()).execute(any())
    }

    companion object {
        private const val REGION_ID = 213L
        private val COORDINATES = geoCoordinatesTestInstance()

        private val USER_ADDRESS = userAddressTestInstance().copy(
            regionId = REGION_ID,
            coordinates = COORDINATES
        )
    }

}