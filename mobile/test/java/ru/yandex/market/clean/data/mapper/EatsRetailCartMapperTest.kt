package ru.yandex.market.clean.data.mapper

import android.os.Build
import com.annimon.stream.Exceptional
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.analytics.facades.health.EatsRetailHealthFacade
import ru.yandex.market.clean.data.mapper.money.MoneyMapper
import ru.yandex.market.clean.data.model.dto.retail.RetailActualizeCartDto
import ru.yandex.market.clean.data.model.dto.retail.RetailCartOfferDto
import ru.yandex.market.clean.data.model.dto.retail.RetailPricesDto
import ru.yandex.market.clean.data.model.dto.retail.eatsRetailErrorDtoTestInstance
import ru.yandex.market.clean.data.model.dto.retail.retailActualizeCartDtoTestInstance
import ru.yandex.market.clean.data.model.dto.retail.retailAdditionalFeeDtoTestInstance
import ru.yandex.market.clean.data.model.dto.retail.retailCartOfferDtoTestInstance
import ru.yandex.market.clean.data.model.dto.retail.retailPricesDtoTestInstance
import ru.yandex.market.clean.domain.model.CartItem
import ru.yandex.market.clean.domain.model.cartIdentifierTestInstance
import ru.yandex.market.clean.domain.model.cartItemTestInstance
import ru.yandex.market.clean.domain.model.offerPricesTestInstance
import ru.yandex.market.clean.domain.model.offerTestInstance
import ru.yandex.market.clean.domain.model.productOfferTestInstance
import ru.yandex.market.clean.domain.model.retail.EatsRetailCart
import ru.yandex.market.clean.domain.model.retail.EatsRetailCartItem
import ru.yandex.market.clean.domain.model.retail.EatsRetailCartPrices
import ru.yandex.market.clean.domain.model.retail.EatsRetailItemInfo
import ru.yandex.market.clean.domain.model.retail.eatsRetailCartAdditionalFeeTestInstance
import ru.yandex.market.clean.domain.model.retail.eatsRetailCartItem_ActualizedTestInstance
import ru.yandex.market.clean.domain.model.retail.eatsRetailCartPricesTestInstance
import ru.yandex.market.clean.domain.model.retail.eatsRetailCart_FromMarketCartTestInstance
import ru.yandex.market.clean.domain.model.retail.eatsRetailErrorTestInstance
import ru.yandex.market.clean.domain.model.shop.shopInfoTestInstance
import ru.yandex.market.data.money.dto.PriceDto
import ru.yandex.market.domain.money.model.Currency
import ru.yandex.market.domain.money.model.Money
import ru.yandex.market.domain.product.model.offer.feedTestInstance
import ru.yandex.market.utils.toMapBy
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], qualifiers = "ru")
class EatsRetailCartMapperTest {

    private val moneyMapper = mock<MoneyMapper> {
        on { map(any<PriceDto>()) } doAnswer {
            val price = it.getArgument(0, PriceDto::class.java)
            Exceptional.of {
                Money(
                    amount = price.value ?: BigDecimal.ZERO,
                    currency = when (price.currency) {
                        Currency.RUR.name -> Currency.RUR
                        else -> Currency.UNKNOWN
                    }
                )
            }
        }
    }

    private val eatsRetailHealthFacade: EatsRetailHealthFacade = mock()

    private val mapper = EatsRetailCartMapper(
        moneyMapper = moneyMapper,
        eatsRetailHealthFacade = { eatsRetailHealthFacade },
    )

    private val shop = shopInfoTestInstance(id = SHOP_ID)
    private val additionalFees = listOf(
        eatsRetailCartAdditionalFeeTestInstance(
            value = Money.createRub(dummyAmount("additionalFees")),
        ),
    )

    @Test
    fun `mapToActualizedOrNull successfully mapped`() {
        val actual = mapper.mapToActualizedOrNull(
            previousCart = eatsRetailCart_FromMarketCartTestInstance(
                id = "$CART_ID",
                items = listOf(eatsRetailCartItem_ActualizedTestInstance(cartItem = createCartItem())),
                shop = shop,
            ),
            retailCartDto = createRetailActualizeCartDto(),
            setOfSelectedCartItemSelectedIds = setOf(CART_ITEM_ID),
        )

        val expected = EatsRetailCart.ActualizedOnce.Actualized(
            id = "$SHOP_ID",
            items = listOf(expectedEatsRetailCartItem_Actualized()),
            shop = shop,
            prices = expectedEatsRetailCartPrices(),
            deliveryTimeMinutes = DELIVERY_TIME_MINUTES,
            shopIsAvailable = true,
            additionalFees = additionalFees,
            errors = listOf(eatsRetailErrorTestInstance(message = "from Dto")),
        )

        Assertions.assertThat(actual).isEqualTo(expected)

        inOrder(eatsRetailHealthFacade).verifyNoMoreInteractions()
    }

    @Test
    fun `mapToActualizedOrNull successful but with one Last item`() {
        val actual = mapper.mapToActualizedOrNull(
            previousCart = eatsRetailCart_FromMarketCartTestInstance(
                id = "$CART_ID",
                items = listOf(
                    eatsRetailCartItem_ActualizedTestInstance(cartItem = createCartItem(index = 0)),
                    eatsRetailCartItem_ActualizedTestInstance(
                        cartItem = createCartItem(
                            index = 1,
                            userBuyCount = USER_BUY_COUNT.toInt() * 2, // for make it isLast
                        ),
                    ),
                ),
                shop = shop,
            ),
            retailCartDto = createRetailActualizeCartDto(
                offers = listOf(
                    createRetailCartOfferDto(index = 0),
                    createRetailCartOfferDto(index = 1),
                ),
            ),
            setOfSelectedCartItemSelectedIds = setOf(CART_ITEM_ID, CART_ITEM_ID + 1),
        )

        val expected = EatsRetailCart.ActualizedOnce.Actualized(
            id = "$SHOP_ID",
            items = listOf(
                expectedEatsRetailCartItem_Actualized(index = 0),
                expectedEatsRetailCartItem_Actualized(index = 1, isLast = true),
            ),
            shop = shop,
            prices = expectedEatsRetailCartPrices(itemsCount = 2),
            deliveryTimeMinutes = DELIVERY_TIME_MINUTES,
            shopIsAvailable = true,
            additionalFees = additionalFees,
            errors = listOf(eatsRetailErrorTestInstance(message = "from Dto")),
        )

        Assertions.assertThat(actual).isEqualTo(expected)

        inOrder(eatsRetailHealthFacade).verifyNoMoreInteractions()
    }

    @Test
    fun `mapToActualizedOrNull successful but with one Expired item`() {
        val actual = mapper.mapToActualizedOrNull(
            previousCart = eatsRetailCart_FromMarketCartTestInstance(
                id = "$CART_ID",
                items = listOf(
                    eatsRetailCartItem_ActualizedTestInstance(cartItem = createCartItem(index = 0)),
                    eatsRetailCartItem_ActualizedTestInstance(cartItem = createCartItem(index = 1))
                ),
                shop = shop,
            ),
            retailCartDto = createRetailActualizeCartDto(
                offers = listOf(
                    createRetailCartOfferDto(index = 0),
                    createRetailCartOfferDto(index = 1, isAvailable = false),
                ),
            ),
            setOfSelectedCartItemSelectedIds = setOf(CART_ITEM_ID, CART_ITEM_ID + 1),
        )

        val expected = EatsRetailCart.ActualizedOnce.Actualized(
            id = "$SHOP_ID",
            items = listOf(
                expectedEatsRetailCartItem_Actualized(index = 0),
                EatsRetailCartItem.ActualizedWithError(cartItem = createCartItem(index = 1, isExpired = true))
            ),
            shop = shop,
            prices = expectedEatsRetailCartPrices(itemsCount = 2),
            deliveryTimeMinutes = DELIVERY_TIME_MINUTES,
            shopIsAvailable = true,
            additionalFees = additionalFees,
            errors = listOf(eatsRetailErrorTestInstance(message = "from Dto")),
        )

        Assertions.assertThat(actual).isEqualTo(expected)

        inOrder(eatsRetailHealthFacade).verifyNoMoreInteractions()
    }

    @Test
    fun `mapToActualizedOrNull successful but with all Expired items`() {
        val actual = mapper.mapToActualizedOrNull(
            previousCart = eatsRetailCart_FromMarketCartTestInstance(
                id = "$CART_ID",
                items = listOf(
                    eatsRetailCartItem_ActualizedTestInstance(cartItem = createCartItem(index = 0)),
                    eatsRetailCartItem_ActualizedTestInstance(cartItem = createCartItem(index = 1)),
                ),
                shop = shop,
            ),
            retailCartDto = createRetailActualizeCartDto(
                offers = listOf(
                    createRetailCartOfferDto(index = 0, isAvailable = false),
                    createRetailCartOfferDto(index = 1, isAvailable = false),
                ),
            ),
            setOfSelectedCartItemSelectedIds = setOf(CART_ITEM_ID, CART_ITEM_ID + 1),
        )

        val expected = EatsRetailCart.ActualizedOnce.Actualized(
            id = "$SHOP_ID",
            items = listOf(
                EatsRetailCartItem.ActualizedWithError(cartItem = createCartItem(index = 0, isExpired = true)),
                EatsRetailCartItem.ActualizedWithError(cartItem = createCartItem(index = 1, isExpired = true)),
            ),
            shop = shop,
            prices = expectedEatsRetailCartPrices(itemsCount = 2),
            deliveryTimeMinutes = DELIVERY_TIME_MINUTES,
            shopIsAvailable = true,
            additionalFees = additionalFees,
            errors = listOf(eatsRetailErrorTestInstance(message = "from Dto")),
        )

        Assertions.assertThat(actual).isEqualTo(expected)

        inOrder(eatsRetailHealthFacade).verifyNoMoreInteractions()
    }

    @Test
    fun `mapToActualizedOrNull successful but with one unSelected item`() {
        val actual = mapper.mapToActualizedOrNull(
            previousCart = eatsRetailCart_FromMarketCartTestInstance(
                id = "$CART_ID",
                items = listOf(
                    eatsRetailCartItem_ActualizedTestInstance(cartItem = createCartItem(index = 0)),
                    eatsRetailCartItem_ActualizedTestInstance(cartItem = createCartItem(index = 1)),
                ),
                shop = shop,
            ),
            retailCartDto = createRetailActualizeCartDto(
                offers = listOf(
                    createRetailCartOfferDto(index = 0),
                    createRetailCartOfferDto(index = 1),
                ),
            ),
            setOfSelectedCartItemSelectedIds = setOf(CART_ITEM_ID),
        )

        val expected = EatsRetailCart.ActualizedOnce.Actualized(
            id = "$SHOP_ID",
            items = listOf(
                expectedEatsRetailCartItem_Actualized(index = 0),
                expectedEatsRetailCartItem_Actualized(index = 1, isSelected = false),
            ),
            shop = shop,
            prices = expectedEatsRetailCartPrices(itemsCount = 2),
            deliveryTimeMinutes = DELIVERY_TIME_MINUTES,
            shopIsAvailable = true,
            additionalFees = additionalFees,
            errors = listOf(eatsRetailErrorTestInstance(message = "from Dto")),
        )

        Assertions.assertThat(actual).isEqualTo(expected)

        inOrder(eatsRetailHealthFacade).verifyNoMoreInteractions()
    }

    @Test
    fun `mapToActualizedOrNull successful but with all unSelected items`() {
        val actual = mapper.mapToActualizedOrNull(
            previousCart = eatsRetailCart_FromMarketCartTestInstance(
                id = "$CART_ID",
                items = listOf(
                    eatsRetailCartItem_ActualizedTestInstance(cartItem = createCartItem(index = 0)),
                    eatsRetailCartItem_ActualizedTestInstance(cartItem = createCartItem(index = 1)),
                ),
                shop = shop,
            ),
            retailCartDto = createRetailActualizeCartDto(
                offers = listOf(
                    createRetailCartOfferDto(index = 0),
                    createRetailCartOfferDto(index = 1),
                ),
            ),
            setOfSelectedCartItemSelectedIds = emptySet(),
        )

        val expected = EatsRetailCart.ActualizedOnce.Actualized(
            id = "$SHOP_ID",
            items = listOf(
                expectedEatsRetailCartItem_Actualized(index = 0, isSelected = false),
                expectedEatsRetailCartItem_Actualized(index = 1, isSelected = false),
            ),
            shop = shop,
            prices = expectedEatsRetailCartPrices(itemsCount = 2),
            deliveryTimeMinutes = DELIVERY_TIME_MINUTES,
            shopIsAvailable = true,
            additionalFees = additionalFees,
            errors = listOf(eatsRetailErrorTestInstance(message = "from Dto")),
        )

        Assertions.assertThat(actual).isEqualTo(expected)

        inOrder(eatsRetailHealthFacade).verifyNoMoreInteractions()
    }

    @Test
    fun `mapToActualizedOrNull shop is not available`() {
        val actual = mapper.mapToActualizedOrNull(
            previousCart = eatsRetailCart_FromMarketCartTestInstance(
                id = "$CART_ID",
                items = listOf(eatsRetailCartItem_ActualizedTestInstance(cartItem = createCartItem())),
                shop = shop,
            ),
            retailCartDto = createRetailActualizeCartDto(shopIsAvailable = false),
            setOfSelectedCartItemSelectedIds = setOf(CART_ITEM_ID),
        )

        val expected = EatsRetailCart.ActualizedOnce.Actualized(
            id = "$SHOP_ID",
            items = listOf(expectedEatsRetailCartItem_Actualized()),
            shop = shop,
            prices = expectedEatsRetailCartPrices(),
            deliveryTimeMinutes = DELIVERY_TIME_MINUTES,
            shopIsAvailable = false,
            additionalFees = additionalFees,
            errors = listOf(eatsRetailErrorTestInstance(message = "from Dto")),
        )

        Assertions.assertThat(actual).isEqualTo(expected)

        inOrder(eatsRetailHealthFacade).verifyNoMoreInteractions()
    }

    @Test
    fun `mapToActualizedOrNull Error when offers is null`() {
        val actual = mapper.mapToActualizedOrNull(
            previousCart = eatsRetailCart_FromMarketCartTestInstance(),
            retailCartDto = retailActualizeCartDtoTestInstance(
                offers = null,
            ),
            setOfSelectedCartItemSelectedIds = emptySet(),
        )

        Assertions.assertThat(actual).isNull()

        eatsRetailHealthFacade.inOrder {
            verify().mapToActualizedCartError(any())
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `mapToActualizedOrNull Error when offer not found`() {
        val previousCartItem = expectedEatsRetailCartItem_Actualized()

        val actual = mapper.mapToActualizedOrNull(
            previousCart = eatsRetailCart_FromMarketCartTestInstance(
                id = "$CART_ID",
                items = listOf(previousCartItem),
                shop = shop,
            ),
            retailCartDto = createRetailActualizeCartDto(
                offers = emptyList(),
            ),
            setOfSelectedCartItemSelectedIds = setOf(CART_ITEM_ID),
        )

        val expected = EatsRetailCart.ActualizedOnce.Actualized(
            id = "$SHOP_ID",
            items = listOf(previousCartItem),
            shop = shop,
            prices = expectedEatsRetailCartPrices(),
            deliveryTimeMinutes = DELIVERY_TIME_MINUTES,
            shopIsAvailable = true,
            additionalFees = additionalFees,
            errors = listOf(eatsRetailErrorTestInstance(message = "from Dto")),
        )

        Assertions.assertThat(actual).isEqualTo(expected)

        eatsRetailHealthFacade.inOrder {
            verify().mapToActualizedCartItemError(any())
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun `mapToNotActualizedOrNull successfully mapped`() {
        val actual = mapper.mapToNotActualizedOrNull(
            shop = shop,
            cartItems = listOf(createCartItem()),
            setOfSelectedCartItemSelectedIds = setOf(CART_ITEM_ID),
        )

        val expected = EatsRetailCart.FromMarketCart(
            id = "$SHOP_ID",
            items = listOf(EatsRetailCartItem.NotActualized(cartItem = createCartItem(), isSelected = true)),
            shop = shop,
        )

        Assertions.assertThat(actual).isEqualTo(expected)

        inOrder(eatsRetailHealthFacade).verifyNoMoreInteractions()
    }

    @Test
    fun `mapToNotActualizedOrNull successfully mapped with one unSelected items`() {
        val actual = mapper.mapToNotActualizedOrNull(
            shop = shop,
            cartItems = listOf(
                createCartItem(index = 0),
                createCartItem(index = 1),
            ),
            setOfSelectedCartItemSelectedIds = setOf(CART_ITEM_ID),
        )

        val expected = EatsRetailCart.FromMarketCart(
            id = "$SHOP_ID",
            items = listOf(
                EatsRetailCartItem.NotActualized(
                    cartItem = createCartItem(index = 0),
                    isSelected = true,
                ),
                EatsRetailCartItem.NotActualized(
                    cartItem = createCartItem(index = 1),
                    isSelected = false,
                ),
            ),
            shop = shop,
        )

        Assertions.assertThat(actual).isEqualTo(expected)

        inOrder(eatsRetailHealthFacade).verifyNoMoreInteractions()
    }

    @Test
    fun `mapToNotActualizedOrNull with all unSelected items`() {
        val actual = mapper.mapToNotActualizedOrNull(
            shop = shop,
            cartItems = listOf(
                createCartItem(index = 0),
                createCartItem(index = 1),
            ),
            setOfSelectedCartItemSelectedIds = emptySet(),
        )

        val expected = EatsRetailCart.FromMarketCart(
            id = "$SHOP_ID",
            items = listOf(
                EatsRetailCartItem.NotActualized(cartItem = createCartItem(index = 0), isSelected = false),
                EatsRetailCartItem.NotActualized(cartItem = createCartItem(index = 1), isSelected = false),
            ),
            shop = shop,
        )

        Assertions.assertThat(actual).isEqualTo(expected)

        inOrder(eatsRetailHealthFacade).verifyNoMoreInteractions()
    }

    @Test
    fun `mapToNotActualizedOrNull with offer is null`() {
        val previousCartItem = cartItemTestInstance(
            userBuyCount = USER_BUY_COUNT.toInt(),
            offer = null,
            cartIdentifier = cartIdentifierTestInstance(id = "cartIdentifier_$SHOP_ID"),
        )

        val actual = mapper.mapToNotActualizedOrNull(
            shop = shop,
            cartItems = listOf(previousCartItem),
            setOfSelectedCartItemSelectedIds = setOf(CART_ITEM_ID),
        )

        val expected = EatsRetailCart.FromMarketCart(
            id = "$SHOP_ID",
            items = listOf(
                EatsRetailCartItem.ActualizedWithError(
                    cartItem = previousCartItem.copy(
                        userBuyCount = 0,
                    ),
                )
            ),
            shop = shop,
        )

        Assertions.assertThat(actual).isEqualTo(expected)

        inOrder(eatsRetailHealthFacade).verifyNoMoreInteractions()
    }

    @Test
    fun `mapToNotActualizedOrNull with Expired item`() {
        val previousCartItem = cartItemTestInstance(
            userBuyCount = USER_BUY_COUNT.toInt(),
            isExpired = true,
            cartIdentifier = cartIdentifierTestInstance(id = "cartIdentifier_$SHOP_ID"),
        )

        val actual = mapper.mapToNotActualizedOrNull(
            shop = shop,
            cartItems = listOf(previousCartItem),
            setOfSelectedCartItemSelectedIds = setOf(CART_ITEM_ID),
        )

        val expected = EatsRetailCart.FromMarketCart(
            id = "$SHOP_ID",
            items = listOf(
                EatsRetailCartItem.ActualizedWithError(
                    cartItem = previousCartItem.copy(
                        isExpired = true,
                    ),
                )
            ),
            shop = shop,
        )

        Assertions.assertThat(actual).isEqualTo(expected)

        inOrder(eatsRetailHealthFacade).verifyNoMoreInteractions()
    }

    private fun createRetailActualizeCartDto(
        shopIsAvailable: Boolean? = true,
        offers: List<RetailCartOfferDto>? = listOf(
            createRetailCartOfferDto(),
        ),
    ): RetailActualizeCartDto {
        return retailActualizeCartDtoTestInstance(
            id = "$CART_ID",
            shopIsAvailable = shopIsAvailable,
            deliveryTimeMinutes = DELIVERY_TIME_MINUTES,
            offers = offers?.toMapBy { it.feedOfferId.orEmpty() },
            prices = createRetailPricesDto(),
            errors = listOf(eatsRetailErrorDtoTestInstance(message = "from Dto")),
        )
    }

    private fun createRetailCartOfferDto(
        index: Int = 0,
        isAvailable: Boolean? = true,
    ): RetailCartOfferDto {
        return retailCartOfferDtoTestInstance(
            feedOfferId = "${OFFER_ID + index}",
            isAvailable = isAvailable,
            price = PriceDto.createRub(dummyAmount("price")),
            discountPrice = PriceDto.createRub(dummyAmount("discountPrice")),
            count = USER_BUY_COUNT,
            errors = listOf(eatsRetailErrorDtoTestInstance(message = "from offer")),
        )
    }

    private fun createCartItem(
        index: Int = 0,
        userBuyCount: Int = USER_BUY_COUNT.toInt(),
        isExpired: Boolean = false,
        isLast: Boolean = false,
    ): CartItem {
        return cartItemTestInstance(
            serverId = CART_ITEM_ID + index,
            userBuyCount = userBuyCount,
            offer = productOfferTestInstance(
                offer = offerTestInstance(
                    prices = offerPricesTestInstance(
                        purchasePrice = Money.createRub(dummyAmount("purchasePrice")),
                        basePrice = Money.createRub(dummyAmount("basePrice")),
                    ),
                    feed = feedTestInstance(offerId = "${OFFER_ID + index}"),
                ),
            ),
            isExpired = isExpired,
            carterPrice = Money.createRub(dummyAmount("carterPrice")),
            cartId = CART_ID,
            cartIdentifier = cartIdentifierTestInstance(id = "cartIdentifier_$SHOP_ID"),
            isLast = isLast,
            shop = shop,
        )
    }

    private fun createRetailPricesDto(): RetailPricesDto {
        return retailPricesDtoTestInstance(
            itemsTotal = PriceDto.createRub(dummyAmount("itemsTotal")),
            itemsTotalBeforeDiscount = PriceDto.createRub(dummyAmount("itemsTotalBeforeDiscount")),
            itemsTotalDiscount = PriceDto.createRub(dummyAmount("itemsTotalDiscount")),
            total = PriceDto.createRub(dummyAmount("total")),
            deliveryTotal = PriceDto.createRub(dummyAmount("deliveryTotal")),
            leftForFreeDelivery = PriceDto.createRub(dummyAmount("leftForFreeDelivery")),
            leftForNextDelivery = PriceDto.createRub(dummyAmount("leftForNextDelivery")),
            nextDelivery = PriceDto.createRub(dummyAmount("nextDelivery")),
            leftForMinOrderPrice = PriceDto.createRub(dummyAmount("leftForMinOrderPrice")),
            overMaxOrderPrice = PriceDto.createRub(dummyAmount("overMaxOrderPrice")),
            additionalFees = listOf(
                retailAdditionalFeeDtoTestInstance(
                    value = PriceDto.createRub(dummyAmount("additionalFees")),
                ),
            ),
        )
    }

    private fun expectedEatsRetailCartItem_Actualized(
        index: Int = 0,
        isSelected: Boolean = true,
        isLast: Boolean = false,
    ): EatsRetailCartItem.Actualized {
        return EatsRetailCartItem.Actualized(
            cartItem = createCartItem(
                index = index,
                isLast = isLast,
            ),
            isSelected = isSelected,
            itemInfo = EatsRetailItemInfo(
                feedOfferId = "${OFFER_ID + index}",
                isAvailable = true,
                price = Money.createRub(dummyAmount("price")),
                discountPrice = Money.createRub(dummyAmount("discountPrice")),
                count = USER_BUY_COUNT,
            ),
            errors = listOf(eatsRetailErrorTestInstance(message = "from offer")),
        )
    }

    private fun expectedEatsRetailCartPrices(
        itemsCount: Int = 1,
    ): EatsRetailCartPrices {
        return eatsRetailCartPricesTestInstance(
            itemsTotal = Money.createRub(dummyAmount("itemsTotal")),
            itemsTotalBeforeDiscount = Money.createRub(dummyAmount("itemsTotalBeforeDiscount")),
            itemsTotalDiscount = Money.createRub(dummyAmount("itemsTotalDiscount")),
            total = Money.createRub(dummyAmount("total")),
            deliveryTotal = Money.createRub(dummyAmount("deliveryTotal")),
            itemsCount = itemsCount,
            leftForFreeDelivery = Money.createRub(dummyAmount("leftForFreeDelivery")),
            leftForNextDelivery = Money.createRub(dummyAmount("leftForNextDelivery")),
            nextDelivery = Money.createRub(dummyAmount("nextDelivery")),
            leftForMinOrderPrice = Money.createRub(dummyAmount("leftForMinOrderPrice")),
            overMaxOrderPrice = Money.createRub(dummyAmount("overMaxOrderPrice")),
        )
    }

    private companion object {
        private const val CART_ID = 11L
        private const val SHOP_ID = 222L
        private const val CART_ITEM_ID = 3333L
        private const val OFFER_ID = 44444L

        private const val DELIVERY_TIME_MINUTES = 88L
        private const val USER_BUY_COUNT = 9L

        private fun dummyAmount(key: String): Int {
            return when (key) {
                "purchasePrice", "discountPrice", "carterPrice" -> 51
                "basePrice", "price" -> 520
                "itemsTotal" -> 5300
                "itemsTotalBeforeDiscount" -> 54000
                "itemsTotalDiscount" -> 550000
                "total" -> 5600000
                "deliveryTotal" -> 57000000
                "leftForFreeDelivery" -> 58
                "leftForNextDelivery" -> 590
                "nextDelivery" -> 6000
                "leftForMinOrderPrice" -> 61000
                "overMaxOrderPrice" -> 620000
                "additionalFees" -> 6300000
                else -> Int.MIN_VALUE
            }
        }
    }
}
