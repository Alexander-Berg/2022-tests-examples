package ru.yandex.market.clean.presentation.feature.hyperlocal.address

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.checkout.summary.AddressFormatter
import ru.yandex.market.checkout.summary.AddressFormatterFormat
import ru.yandex.market.clean.data.mapper.AddressMapper
import ru.yandex.market.clean.presentation.feature.checkout.editdata.delivery.address.formatter.CourierDeliveryAddressFormatter
import ru.yandex.market.data.passport.Address
import ru.yandex.market.domain.hyperlocal.model.HyperlocalAddress
import ru.yandex.market.domain.models.region.GeoCoordinates
import ru.yandex.market.domain.useraddress.model.UserAddress

@RunWith(Parameterized::class)
class HyperlocalAddressStateFormatterTest(
    private val hyperlocalAddress: HyperlocalAddress,
    private val hasItems: Boolean,
    private val state: HyperlocalAddressDialogState
) {
    private val addressMapper: AddressMapper = mock()
    private val addressFormatter: AddressFormatter = mock()
    private val deliveryAddressFormatter: CourierDeliveryAddressFormatter = mock()
    private val formatter = HyperlocalAddressStateFormatter(addressMapper, addressFormatter, deliveryAddressFormatter)

    @Test
    fun `check format`() {
        whenever(
            addressMapper.map(
                any(),
                eq(0L)
            )
        ).thenReturn(
            Address.builder().build()
        )
        whenever(
            addressFormatter.format(
                any<Address>(),
                eq(AddressFormatterFormat.excludePostCode())
            )
        ).thenReturn(
            FORMATTED_ADDRESS_PREFIX + (hyperlocalAddress as? HyperlocalAddress.Exists)?.userAddress?.city
        )
        val resultState = formatter.format(hyperlocalAddress, emptyList(), hasItems)
        assertThat(resultState).isEqualTo(state)
    }

    companion object {

        private const val FORMATTED_ADDRESS_PREFIX = "Отформатированный адрес для города "

        @Parameterized.Parameters
        @JvmStatic
        fun parameters(): Iterable<Array<*>> = listOf(
            arrayOf(
                HyperlocalAddress.Exists.Expired(
                    coordinates = GeoCoordinates(0.0, 0.0),
                    userAddress = UserAddress(regionId = 123, country = "Россия", city = "Москва", house = "Дом")
                ),
                false,
                HyperlocalAddressDialogState.NeedConfirmation.ExpiredAddress("Отформатированный адрес для города Москва")
            ),
            arrayOf(
                HyperlocalAddress.Absent,
                false,
                HyperlocalAddressDialogState.NeedConfirmation.NoAddress(false)
            ),
            arrayOf(
                HyperlocalAddress.Absent,
                true,
                HyperlocalAddressDialogState.NeedConfirmation.NoAddress(true)
            )
        )
    }
}