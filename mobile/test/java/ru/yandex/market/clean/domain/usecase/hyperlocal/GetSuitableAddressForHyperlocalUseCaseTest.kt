package ru.yandex.market.clean.domain.usecase.hyperlocal

import com.annimon.stream.Optional
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import ru.yandex.market.clean.domain.model.checkout.BucketState
import ru.yandex.market.clean.domain.model.checkout.CheckoutLastParams
import ru.yandex.market.clean.domain.model.checkout.CheckoutParcels
import ru.yandex.market.clean.domain.model.order.orderTestInstance
import ru.yandex.market.clean.domain.usecase.address.GetCurrentRegionUseCase
import ru.yandex.market.clean.domain.usecase.checkout.lastparams.GetCheckoutLastParamsUseCase
import ru.yandex.market.clean.domain.usecase.checkout.lastparams.GetLastTouchedUserAddress
import ru.yandex.market.clean.domain.usecase.order.GetLastOrderUseCase
import ru.yandex.market.clean.domain.usecase.userpreset.UserAddressUseCase
import ru.yandex.market.data.passport.Address
import ru.yandex.market.domain.delivery.model.DeliveryType
import ru.yandex.market.domain.useraddress.model.userAddressTestInstance
import ru.yandex.market.mockResult

class GetSuitableAddressForHyperlocalUseCaseTest {

    private val getLastTouchedUserAddressUseCase = mock<GetLastTouchedUserAddress>()
    private val getCheckoutLastParamsUseCase = mock<GetCheckoutLastParamsUseCase>()
    private val getCurrentRegionUseCase = mock<GetCurrentRegionUseCase> {
        on { execute() } doReturn Single.just(REGION_MOSCOW)
    }
    private val getLastOrderUseCase = mock<GetLastOrderUseCase>()
    private val userAddressUseCase = mock<UserAddressUseCase>()

    private val getSuitableAddressForHyperlocalUseCase = GetSuitableAddressForHyperlocalUseCase(
        getLastTouchedUserAddressUseCase,
        getCheckoutLastParamsUseCase,
        getCurrentRegionUseCase,
        getLastOrderUseCase,
        userAddressUseCase
    )

    @Test
    fun `check valid address from checkout last params`() {
        getCheckoutLastParamsUseCase.execute().mockResult(Single.just(CHECKOUT_STATE_MOSCOW))
        getLastTouchedUserAddressUseCase.execute().mockResult(Single.just(Optional.empty()))
        getLastOrderUseCase.getLastOrder(any(), any()).mockResult(Single.just(Optional.empty()))
        userAddressUseCase.getUserAddresses(any()).mockResult(Single.just(listOf(ADDRESS_MOSCOW)))

        getSuitableAddressForHyperlocalUseCase
            .execute()
            .test()
            .assertNoErrors()
            .assertResult(ADDRESS_MOSCOW)

        verify(getCurrentRegionUseCase).execute()
        verify(getCheckoutLastParamsUseCase).execute()
        verify(getLastTouchedUserAddressUseCase, never()).execute()
        verify(getLastOrderUseCase, never()).getLastOrder(any(), any())
        verify(userAddressUseCase, never()).getUserAddresses(any())
    }

    @Test
    fun `check valid address from last touched`() {
        getCheckoutLastParamsUseCase.execute().mockResult(Single.just(CHECKOUT_STATE_SPB))
        getLastTouchedUserAddressUseCase.execute().mockResult(Single.just(Optional.of(ADDRESS_MOSCOW)))
        getLastOrderUseCase.getLastOrder(any(), any()).mockResult(Single.just(Optional.empty()))
        userAddressUseCase.getUserAddresses(any()).mockResult(Single.just(listOf(ADDRESS_MOSCOW)))

        getSuitableAddressForHyperlocalUseCase
            .execute()
            .test()
            .assertNoErrors()
            .assertResult(ADDRESS_MOSCOW)

        verify(getCurrentRegionUseCase).execute()
        verify(getCheckoutLastParamsUseCase).execute()
        verify(getLastTouchedUserAddressUseCase).execute()
        verify(getLastOrderUseCase, never()).getLastOrder(any(), any())
        verify(userAddressUseCase, never()).getUserAddresses(any())
    }

    @Test
    fun `check valid address from last order`() {
        getCheckoutLastParamsUseCase.execute().mockResult(Single.just(CHECKOUT_STATE_SPB))
        getLastTouchedUserAddressUseCase.execute().mockResult(Single.just(Optional.empty()))
        getLastOrderUseCase.getLastOrder(any(), eq(true)).mockResult(Single.just(Optional.of(ORDER)))
        userAddressUseCase.getUserAddresses(any()).mockResult(Single.just(listOf(ADDRESS_MOSCOW)))

        getSuitableAddressForHyperlocalUseCase
            .execute()
            .test()
            .assertNoErrors()
            .assertResult(ADDRESS_MOSCOW)

        verify(getCurrentRegionUseCase).execute()
        verify(getCheckoutLastParamsUseCase).execute()
        verify(getLastTouchedUserAddressUseCase).execute()
        verify(getLastOrderUseCase).getLastOrder(any(), eq(true))
        verify(userAddressUseCase).getUserAddresses(any())
    }

    @Test
    fun `check valid address from user addresses`() {
        getCheckoutLastParamsUseCase.execute().mockResult(Single.just(CHECKOUT_STATE_SPB))
        getLastTouchedUserAddressUseCase.execute().mockResult(Single.just(Optional.empty()))
        getLastOrderUseCase.getLastOrder(any(), eq(true)).mockResult(Single.just(Optional.empty()))
        userAddressUseCase.getUserAddresses(any()).mockResult(Single.just(listOf(ADDRESS_MOSCOW)))

        getSuitableAddressForHyperlocalUseCase
            .execute()
            .test()
            .assertNoErrors()
            .assertResult(ADDRESS_MOSCOW)

        verify(getCurrentRegionUseCase).execute()
        verify(getCheckoutLastParamsUseCase).execute()
        verify(getLastTouchedUserAddressUseCase).execute()
        verify(getLastOrderUseCase).getLastOrder(any(), eq(true))
        verify(userAddressUseCase).getUserAddresses(any())
    }

    companion object {
        private const val REGION_MOSCOW = 213L
        private const val REGION_SPB = 2L

        private val ADDRESS_MOSCOW = userAddressTestInstance().copy(
            regionId = REGION_MOSCOW,
            country = "Россия",
            city = "Москва",
            street = "Новинский бульвар",
            house = "8"
        )

        private val ADDRESS_SPB = userAddressTestInstance().copy(
            regionId = REGION_SPB,
            country = "Россия",
            city = "Санкт-Петербург"
        )

        private val CHECKOUT_STATE_MOSCOW = CheckoutLastParams.EMPTY.copy(
            parcelsInfo = CheckoutParcels(
                states = listOf(
                    BucketState(
                        id = "id",
                        address = ADDRESS_MOSCOW
                    )
                )
            )
        )

        private val CHECKOUT_STATE_SPB = CheckoutLastParams.EMPTY.copy(
            parcelsInfo = CheckoutParcels(
                states = listOf(
                    BucketState(
                        id = "id2",
                        address = ADDRESS_SPB
                    )
                )
            )
        )

        private val ORDER = orderTestInstance(
            deliveryType = DeliveryType.DELIVERY,
            address = Address.builder()
                .regionId(REGION_MOSCOW)
                .postCode(ADDRESS_MOSCOW.postcode)
                .country(ADDRESS_MOSCOW.country)
                .city(ADDRESS_MOSCOW.city)
                .street(ADDRESS_MOSCOW.street)
                .district(ADDRESS_MOSCOW.district)
                .house(ADDRESS_MOSCOW.house)
                .entrance(ADDRESS_MOSCOW.entrance)
                .intercom(ADDRESS_MOSCOW.intercom)
                .floor(ADDRESS_MOSCOW.floor)
                .room(ADDRESS_MOSCOW.apartment)
                .build()
        )
    }

}