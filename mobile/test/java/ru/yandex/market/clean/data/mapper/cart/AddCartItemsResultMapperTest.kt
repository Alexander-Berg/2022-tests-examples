package ru.yandex.market.clean.data.mapper.cart

import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.kotlin.mock
import ru.yandex.market.clean.data.fapi.dto.cart.selectedServiceDtoTestInstance
import ru.yandex.market.clean.data.fapi.dto.frontApiCartItemDtoTestInstance
import ru.yandex.market.clean.data.mapper.OrderOptionsServiceMapper
import ru.yandex.market.clean.data.mapper.money.MoneyMapper
import ru.yandex.market.clean.data.mapper.shop.ShopInfoMapper
import ru.yandex.market.clean.data.model.db.cartItemEntityTestInstance
import ru.yandex.market.clean.data.model.dto.cart.persistentMergedCartItemDtoTestInstance
import ru.yandex.market.clean.domain.model.productOfferTestInstance
import ru.yandex.market.datetime.DateTimeProvider

class AddCartItemsResultMapperTest {

    private val moneyMapper = mock<MoneyMapper>()
    private val dateTimeProvider = mock<DateTimeProvider>()
    private val orderOptionsServiceMapper = mock<OrderOptionsServiceMapper>()
    private val shopInfoMapper = mock<ShopInfoMapper>()
    private val cartItemComplementaryDigestMapper = mock<CartItemComplementaryDigestMapper>()

    private val cartItemMapper = CartItemMapper(
        moneyMapper = moneyMapper,
        dateTimeProvider = dateTimeProvider,
        orderOptionsServiceMapper = orderOptionsServiceMapper,
        shopInfoMapper = shopInfoMapper,
        cartItemComplementaryDigestMapper = cartItemComplementaryDigestMapper,
        imageMapper = mock(),
    )

    @get:Rule
    var thrown: ExpectedException = ExpectedException.none()

    @Test
    fun `test require offer id`() {
        thrown.expect(IllegalArgumentException::class.java)
        cartItemMapper.map(
            item = frontApiCartItemDtoTestInstance(objId = null),
            offer = productOfferTestInstance(),
            entity = persistentMergedCartItemDtoTestInstance(),
            selectedService = selectedServiceDtoTestInstance(),
            dateInStock = null,
        ).orThrow
    }

    @Test
    fun `test require offer id 2`() {
        thrown.expect(IllegalArgumentException::class.java)
        cartItemMapper.map(
            frontApiCartItemDtoTestInstance(objId = null),
            productOfferTestInstance(),
            persistentMergedCartItemDtoTestInstance(),
            selectedServiceDtoTestInstance(),
            null,
        ).orThrow
    }

    @Test
    fun `test require creationTime`() {
        thrown.expect(IllegalArgumentException::class.java)
        cartItemMapper.map(
            cartItemEntityTestInstance(creationTime = null),
            productOfferTestInstance(),
            null,
        ).orThrow
    }

    @Test
    fun `test require count more than 0`() {
        thrown.expect(IllegalArgumentException::class.java)
        cartItemMapper.map(
            productOfferTestInstance(),
            null,
            0,
            "",
            null
        )
    }
}
