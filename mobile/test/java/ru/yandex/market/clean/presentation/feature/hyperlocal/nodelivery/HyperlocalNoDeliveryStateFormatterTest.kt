package ru.yandex.market.clean.presentation.feature.hyperlocal.nodelivery

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.beru.android.R
import ru.yandex.market.checkout.summary.AddressFormatter
import ru.yandex.market.checkout.summary.AddressFormatterFormat
import ru.yandex.market.clean.data.mapper.AddressMapper
import ru.yandex.market.clean.presentation.parcelable.media.simpleImageReferenceParcelableTestInstance
import ru.yandex.market.data.passport.Address
import ru.yandex.market.domain.hyperlocal.model.HyperlocalAddress
import ru.yandex.market.domain.media.model.simpleImageReferenceTestInstance
import ru.yandex.market.domain.models.region.GeoCoordinates
import ru.yandex.market.domain.useraddress.model.UserAddress
import ru.yandex.market.common.android.ResourcesManager

@RunWith(Parameterized::class)
class HyperlocalNoDeliveryStateFormatterTest(
    val hyperlocalAddress: HyperlocalAddress,
    val source: HyperlocalNoDeliverySource,
    val state: HyperlocalNoDeliveryDialogState
) {

    private val addressMapper: AddressMapper = mock()
    private val addressFormatter: AddressFormatter = mock()
    private val resourcesManager: ResourcesManager = mock()
    private val formatter = HyperlocalNoDeliveryStateFormatter(addressMapper, addressFormatter, resourcesManager)

    @Test
    fun `check format`() {
        whenever(
            resourcesManager.getString(R.string.hyperlocal_no_delivery_title_express)
        ).thenReturn("Экспресс-доставка здесь пока не работает")
        whenever(resourcesManager.getString(R.string.hyperlocal_no_delivery_title_product))
            .thenReturn("Нет экспресс-доставки этого товара по вашему адресу")
        whenever(resourcesManager.getString(R.string.hyperlocal_no_delivery_title_product_no_offers))
            .thenReturn("Сюда нельзя доставить этот товар")
        whenever(resourcesManager.getString(R.string.hyperlocal_no_delivery_title_shop_in_shop))
            .thenReturn("Экспресс-доставка из этого магазина здесь пока не работает")
        whenever(resourcesManager.getString(R.string.hyperlocal_no_delivery_title_supermarket))
            .thenReturn("Доставка из супермаркета здесь пока не работает")
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
        val formatted = formatter.format(hyperlocalAddress, source)
        assertThat(formatted).isEqualTo(state)
    }

    companion object {

        private const val FORMATTED_ADDRESS_PREFIX = "Отформатированный адрес для города "

        @Parameterized.Parameters
        @JvmStatic
        fun parameters(): Iterable<Array<*>> = listOf(
            //карточка товара
            arrayOf(
                HyperlocalAddress.Absent,
                HyperlocalNoDeliverySource.ProductCard(
                    hasAnotherOffers = false,
                    productImage = simpleImageReferenceParcelableTestInstance(url = "123")
                ),
                HyperlocalNoDeliveryDialogState(
                    productImage = simpleImageReferenceTestInstance(url = "123"),
                    showErrorImage = false,
                    titleText = "Сюда нельзя доставить этот товар",
                    titleCentered = false,
                    infoText = null,
                    infoCentered = false
                )
            ),
            //карточка товара, не работает в целом
            arrayOf(
                HyperlocalAddress.Exists.Actual(
                    coordinates = GeoCoordinates(0.0, 0.0),
                    userAddress = UserAddress(regionId = 123, country = "Россия", city = "Москва", house = "Дом")
                ),
                HyperlocalNoDeliverySource.ProductCard(
                    hasAnotherOffers = true,
                    productImage = simpleImageReferenceParcelableTestInstance(url = "123")
                ),
                HyperlocalNoDeliveryDialogState(
                    productImage = null,
                    showErrorImage = true,
                    titleText = "Нет экспресс-доставки этого товара по вашему адресу",
                    titleCentered = false,
                    infoText = "Отформатированный адрес для города Москва",
                    infoCentered = false
                )
            ),
            //экспресс-доставка не работает
            arrayOf(
                HyperlocalAddress.Exists.Actual(
                    coordinates = GeoCoordinates(0.0, 0.0),
                    userAddress = UserAddress(regionId = 123, country = "Россия", city = "Москва", house = "Дом")
                ),
                HyperlocalNoDeliverySource.ExpressDelivery.Common,
                HyperlocalNoDeliveryDialogState(
                    productImage = null,
                    showErrorImage = false,
                    titleText = "Экспресс-доставка здесь пока не работает",
                    titleCentered = false,
                    infoText = "Отформатированный адрес для города Москва",
                    infoCentered = false
                )
            ),
            //доставка из супермаркета не работает
            arrayOf(
                HyperlocalAddress.Exists.Actual(
                    coordinates = GeoCoordinates(0.0, 0.0),
                    userAddress = UserAddress(regionId = 123, country = "Россия", city = "Москва", house = "Дом")
                ),
                HyperlocalNoDeliverySource.Supermarket,
                HyperlocalNoDeliveryDialogState(
                    productImage = null,
                    showErrorImage = false,
                    titleText = "Доставка из супермаркета здесь пока не работает",
                    titleCentered = false,
                    infoText = "Отформатированный адрес для города Москва",
                    infoCentered = false
                )
            ),
            //доставка из поиска не работает
            arrayOf(
                HyperlocalAddress.Exists.Actual(
                    coordinates = GeoCoordinates(0.0, 0.0),
                    userAddress = UserAddress(regionId = 123, country = "Россия", city = "Москва", house = "Дом")
                ),
                HyperlocalNoDeliverySource.Search(simpleImageReferenceParcelableTestInstance(url = "123")),
                HyperlocalNoDeliveryDialogState(
                    productImage = simpleImageReferenceTestInstance(url = "123"),
                    showErrorImage = false,
                    titleText = "Нет экспресс-доставки этого товара по вашему адресу",
                    titleCentered = true,
                    infoText = "Отформатированный адрес для города Москва",
                    infoCentered = true
                )
            ),
            //доставка конкретного товара из магазина не работает
            arrayOf(
                HyperlocalAddress.Exists.Actual(
                    coordinates = GeoCoordinates(0.0, 0.0),
                    userAddress = UserAddress(regionId = 123, country = "Россия", city = "Москва", house = "Дом")
                ),
                HyperlocalNoDeliverySource.ShopInShop.Product(simpleImageReferenceParcelableTestInstance(url = "123")),
                HyperlocalNoDeliveryDialogState(
                    productImage = simpleImageReferenceTestInstance(url = "123"),
                    showErrorImage = false,
                    titleText = "Нет экспресс-доставки этого товара по вашему адресу",
                    titleCentered = true,
                    infoText = "Отформатированный адрес для города Москва",
                    infoCentered = true
                )
            ),
            //доставка из магазина не работает
            arrayOf(
                HyperlocalAddress.Exists.Actual(
                    coordinates = GeoCoordinates(0.0, 0.0),
                    userAddress = UserAddress(regionId = 123, country = "Россия", city = "Москва", house = "Дом")
                ),
                HyperlocalNoDeliverySource.ShopInShop.Common,
                HyperlocalNoDeliveryDialogState(
                    productImage = null,
                    showErrorImage = false,
                    titleText = "Экспресс-доставка из этого магазина здесь пока не работает",
                    titleCentered = false,
                    infoText = "Отформатированный адрес для города Москва",
                    infoCentered = false
                )
            )
        )
    }


}