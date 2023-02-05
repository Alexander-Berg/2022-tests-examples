package ru.yandex.market.clean.domain.usecase.hyperlocal

import com.annimon.stream.Optional
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.clean.domain.usecase.userpreset.UserAddressUseCase
import ru.yandex.market.domain.hyperlocal.model.HyperlocalAddress
import ru.yandex.market.domain.models.region.GeoCoordinates
import ru.yandex.market.domain.useraddress.model.userAddressTestInstance
import ru.yandex.market.mockResult

class FindHyperlocalUserAddressUseCaseTest {

    private val getHyperlocalAddressUseCase = mock<GetHyperlocalAddressUseCase>()
    private val getUserAddressUseCase = mock<UserAddressUseCase>()

    private val useCase = FindHyperlocalUserAddressUseCase(
        getHyperlocalAddressUseCase = getHyperlocalAddressUseCase,
        getUserAddressUseCase = getUserAddressUseCase,
    )

    @Test
    fun `check on correct address getting`() {

        getHyperlocalAddressUseCase.execute().mockResult(Single.just(HYPERLOCAL_ACTUAL_ADDRESS))
        getUserAddressUseCase.getUserAddresses().mockResult(Single.just(listOf(USER_ADDRESS)))

        useCase.execute()
            .test()
            .assertNoErrors()
            .assertResult(Optional.of(USER_ADDRESS))

        verify(getHyperlocalAddressUseCase).execute()
        verify(getUserAddressUseCase).getUserAddresses()
    }

    @Test
    fun `check on empty when user address is null`() {

        getHyperlocalAddressUseCase.execute().mockResult(Single.just(HyperlocalAddress.Absent))
        getUserAddressUseCase.getUserAddresses().mockResult(Single.just(listOf(USER_ADDRESS)))

        useCase.execute()
            .test()
            .assertNoErrors()
            .assertResult(Optional.empty())

        verify(getHyperlocalAddressUseCase).execute()
        verify(getUserAddressUseCase).getUserAddresses()
    }

    private companion object {

        val USER_ADDRESS = userAddressTestInstance()

        val HYPERLOCAL_ACTUAL_ADDRESS = HyperlocalAddress.Exists.Actual(

            coordinates = GeoCoordinates(0.0, 0.0),
            userAddress = USER_ADDRESS
        )
    }
}