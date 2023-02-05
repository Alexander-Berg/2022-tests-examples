package ru.yandex.market.clean.presentation.feature.cart.formatter

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.beru.android.R
import ru.yandex.market.checkout.summary.SummaryItem
import ru.yandex.market.checkout.summary.SummaryPriceVo
import ru.yandex.market.clean.domain.model.cartItemTestInstance
import ru.yandex.market.clean.domain.model.retail.eatsRetailCartAdditionalFeeTestInstance
import ru.yandex.market.clean.domain.model.retail.eatsRetailCartItem_ActualizedTestInstance
import ru.yandex.market.clean.domain.model.retail.eatsRetailCartItem_ActualizedWithErrorTestInstance
import ru.yandex.market.clean.domain.model.retail.eatsRetailCartItem_NotActualizedTestInstance
import ru.yandex.market.clean.domain.model.retail.eatsRetailCart_ActualizedOnce_ActualizedTestInstance
import ru.yandex.market.clean.domain.model.retail.eatsRetailCart_ActualizedOnce_ActualizingTestInstance
import ru.yandex.market.clean.domain.model.retail.eatsRetailCart_ActualizedWithErrorTestInstance
import ru.yandex.market.clean.domain.model.retail.eatsRetailCart_FromMarketCartTestInstance
import ru.yandex.market.clean.domain.model.retail.eatsRetailErrorTestInstance
import ru.yandex.market.clean.domain.model.retail.eatsRetailItemInfoTestInstance
import ru.yandex.market.clean.domain.model.shop.shopInfoTestInstance
import ru.yandex.market.clean.presentation.feature.cart.CartType
import ru.yandex.market.clean.presentation.feature.cart.selectedByUserDataTestInstance
import ru.yandex.market.clean.presentation.feature.cart.vo.CartButtonStyle
import ru.yandex.market.clean.presentation.feature.cart.vo.CartButtonVo
import ru.yandex.market.clean.presentation.feature.cart.vo.CartElementVo
import ru.yandex.market.clean.presentation.feature.cart.vo.CartItemVo
import ru.yandex.market.clean.presentation.feature.cart.vo.EatsRetailCartVo
import ru.yandex.market.clean.presentation.feature.cart.vo.MulticartJuridicalInfoVo
import ru.yandex.market.clean.presentation.formatter.MoneyFormatter
import ru.yandex.market.clean.presentation.vo.CartTitleFlexibleVo
import ru.yandex.market.clean.presentation.vo.GotoShopInShopBlockVo
import ru.yandex.market.clean.presentation.vo.RemoveBlockVo
import ru.yandex.market.common.android.ResourcesManagerImpl
import ru.yandex.market.feature.money.viewobject.MoneyVo
import ru.yandex.market.internal.sync.Synchronizable
import ru.yandex.market.internal.sync.Synchronized
import ru.yandex.market.internal.sync.Synchronizing
import ru.yandex.market.mockResult

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], qualifiers = "ru")
class EatsRetailCartFormatterTest {

    private val cartItemFormatter: CartItemFormatter = mock()
    private val eatsRetailCartAlertMessageFormatter: EatsRetailCartAlertMessageFormatter = mock()
    private val resourceManager = ResourcesManagerImpl(ApplicationProvider.getApplicationContext<Context>().resources)
    private val moneyFormatter: MoneyFormatter = mock {
        on {
            formatAsMoneyVo(money = any(), allowZeroMoney = any(), prefix = any(), suffix = any())
        } doReturn MoneyVo.empty()
        on {
            formatAsMoneyVo(money = any(), prefix = any(), suffix = any())
        } doReturn MoneyVo.empty()
    }
    private val cartFormatterConfig: CartFormatterConfig = mock()

    private val formatter = EatsRetailCartFormatter(
        cartItemFormatter = cartItemFormatter,
        eatsRetailCartAlertMessageFormatter = eatsRetailCartAlertMessageFormatter,
        resourceManager = resourceManager,
        moneyFormatter = moneyFormatter,
    )

    @Test
    fun `No retail carts`() {
        val actual = formatter.formatEatsRetailCartVos(
            eatsRetailCarts = emptyMap(),
            selectedByUserData = selectedByUserDataTestInstance(),
            cartFormatterConfig = cartFormatterConfig,
        )

        val expected = emptyList<EatsRetailCartVo>()

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `NotActualized retail carts`() {
        val expectedItems = listOf<CartItemVo>(
            mock {
                on { count } doReturn 1
                on { isSelected } doReturn true
            },
        )

        cartItemFormatter.formatEatsRetailItems(any(), any(), any(), anyOrNull())
            .mockResult(expectedItems)

        val actual = formatter.formatEatsRetailCartVos(
            eatsRetailCarts = mapOf(
                "id1" to eatsRetailCart_FromMarketCartTestInstance(
                    id = "id1",
                    items = listOf(
                        eatsRetailCartItem_NotActualizedTestInstance(
                            cartItem = cartItemTestInstance(userBuyCount = 1, isExpired = true),
                            isSelected = true,
                        ),
                    ),
                    shop = shopInfoTestInstance(
                        id = DUMMY_SHOP_ID,
                        businessId = DUMMY_BUSINESS_ID,
                    ),
                ),
                "id2" to eatsRetailCart_FromMarketCartTestInstance(
                    id = "id2",
                    items = listOf(
                        eatsRetailCartItem_NotActualizedTestInstance(
                            cartItem = cartItemTestInstance(userBuyCount = 1, isExpired = false),
                            isSelected = true,
                        ),
                    ),
                    shop = shopInfoTestInstance(
                        id = DUMMY_SHOP_ID,
                        businessId = DUMMY_BUSINESS_ID,
                    ),
                ),
            ),
            selectedByUserData = selectedByUserDataTestInstance(),
            cartFormatterConfig = cartFormatterConfig,
        )

        val expected = listOf(
            expectedEatsRetailCartVo(
                id = "id1",
                cartItemPacks = expectedItems,
                itemsCount = 1,
                itemsCountFormatted = "1 товар",
                isAnyItemAvailable = false,
                isRemoveItemsBlockActive = false,
                isSelectAllVisible = false,
                isSelectAllSelected = true,
                isCheckoutEnabled = false,
                summary = Synchronizing(expectedSummaryPriceVo("id1", 0, expectedDelivery())),
            ),
            expectedEatsRetailCartVo(
                id = "id2",
                cartItemPacks = expectedItems,
                itemsCount = 1,
                itemsCountFormatted = "1 товар",
                isAnyItemAvailable = true,
                isRemoveItemsBlockActive = true,
                isSelectAllVisible = true,
                isSelectAllSelected = true,
                isCheckoutEnabled = false,
                summary = Synchronizing(expectedSummaryPriceVo("id2", 0, expectedDelivery())),
            ),
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Actualizing retail carts`() {
        val expectedItems = listOf<CartItemVo>(
            mock {
                on { count } doReturn 2
                on { isExpired } doReturn false
                on { isSelected } doReturn true
            },
            mock {
                on { count } doReturn 3
                on { isExpired } doReturn false
                on { isSelected } doReturn true
            },
        )

        cartItemFormatter.formatEatsRetailItems(any(), any(), any(), anyOrNull())
            .mockResult(expectedItems)

        val actual = formatter.formatEatsRetailCartVos(
            eatsRetailCarts = mapOf(
                "id4" to eatsRetailCart_ActualizedOnce_ActualizingTestInstance(
                    id = "id4",
                    items = listOf(
                        eatsRetailCartItem_ActualizedTestInstance(
                            cartItem = cartItemTestInstance(userBuyCount = 2, isExpired = false),
                            itemInfo = eatsRetailItemInfoTestInstance(isAvailable = true),
                            isSelected = true,
                        ),
                        eatsRetailCartItem_NotActualizedTestInstance(
                            cartItem = cartItemTestInstance(userBuyCount = 3, isExpired = false),
                            isSelected = true,
                        ),
                    ),
                    shop = shopInfoTestInstance(
                        id = DUMMY_SHOP_ID,
                        businessId = DUMMY_BUSINESS_ID,
                    ),
                    additionalFees = listOf(eatsRetailCartAdditionalFeeTestInstance(title = "Сервисный сбор")),
                    errors = emptyList(),
                ),
            ),
            selectedByUserData = selectedByUserDataTestInstance(),
            cartFormatterConfig = cartFormatterConfig,
        )

        val expected = listOf(
            expectedEatsRetailCartVo(
                id = "id4",
                cartItemPacks = expectedItems,
                itemsCount = 5,
                itemsCountFormatted = "5 товаров",
                isAnyItemAvailable = true,
                isRemoveItemsBlockActive = true,
                isSelectAllVisible = true,
                isSelectAllSelected = true,
                isCheckoutEnabled = true,
                summary = Synchronizing(
                    expectedSummaryPriceVo(
                        "id4",
                        2,
                        expectedDiscounts(),
                        expectedAdditionalFees(),
                        expectedDelivery(42)
                    )
                ),
            ),
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Successfully Actualized retail carts`() {
        val expectedItems = listOf<CartItemVo>(
            mock {
                on { count } doReturn 15
                on { isExpired } doReturn false
                on { isSelected } doReturn true
            },
            mock {
                on { count } doReturn 5
                on { isExpired } doReturn false
                on { isSelected } doReturn true
            },
        )

        cartItemFormatter.formatEatsRetailItems(any(), any(), any(), anyOrNull())
            .mockResult(expectedItems)

        val actual = formatter.formatEatsRetailCartVos(
            eatsRetailCarts = mapOf(
                "id2" to eatsRetailCart_ActualizedOnce_ActualizedTestInstance(
                    id = "id2",
                    items = listOf(
                        eatsRetailCartItem_ActualizedTestInstance(
                            cartItem = cartItemTestInstance(userBuyCount = 15, isExpired = false),
                            itemInfo = eatsRetailItemInfoTestInstance(isAvailable = true),
                            isSelected = true,
                        ),
                        eatsRetailCartItem_ActualizedTestInstance(
                            cartItem = cartItemTestInstance(userBuyCount = 5, isExpired = false),
                            itemInfo = eatsRetailItemInfoTestInstance(isAvailable = true),
                            isSelected = true,
                        ),
                    ),
                    shop = shopInfoTestInstance(
                        id = DUMMY_SHOP_ID,
                        businessId = DUMMY_BUSINESS_ID,
                    ),
                    additionalFees = listOf(eatsRetailCartAdditionalFeeTestInstance(title = "Сервисный сбор")),
                    errors = emptyList(),
                ),
            ),
            selectedByUserData = selectedByUserDataTestInstance(),
            cartFormatterConfig = cartFormatterConfig,
        )

        val expected = listOf(
            expectedEatsRetailCartVo(
                id = "id2",
                cartItemPacks = expectedItems,
                itemsCount = 20,
                itemsCountFormatted = "20 товаров",
                isAnyItemAvailable = true,
                isRemoveItemsBlockActive = true,
                isSelectAllVisible = true,
                isSelectAllSelected = true,
                isCheckoutEnabled = true,
                summary = Synchronized(
                    expectedSummaryPriceVo(
                        "id2",
                        2,
                        expectedDiscounts(),
                        expectedAdditionalFees(),
                        expectedDelivery(42)
                    )
                ),
            ),
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Actualized retail carts with unSelected item`() {
        val expectedItems = listOf<CartItemVo>(
            mock {
                on { count } doReturn 15
                on { isExpired } doReturn false
                on { isSelected } doReturn true
            },
            mock {
                on { count } doReturn 1
                on { isExpired } doReturn false
                on { isSelected } doReturn true
            },
            mock {
                on { count } doReturn 5
                on { isExpired } doReturn false
                on { isSelected } doReturn false
            },
        )

        cartItemFormatter.formatEatsRetailItems(any(), any(), any(), anyOrNull())
            .mockResult(expectedItems)

        val actual = formatter.formatEatsRetailCartVos(
            eatsRetailCarts = mapOf(
                "id2" to eatsRetailCart_ActualizedOnce_ActualizedTestInstance(
                    id = "id2",
                    items = listOf(
                        eatsRetailCartItem_ActualizedTestInstance(
                            cartItem = cartItemTestInstance(userBuyCount = 15, isExpired = false),
                            itemInfo = eatsRetailItemInfoTestInstance(isAvailable = true),
                            isSelected = true,
                        ),
                        eatsRetailCartItem_ActualizedTestInstance(
                            cartItem = cartItemTestInstance(userBuyCount = 1, isExpired = false),
                            itemInfo = eatsRetailItemInfoTestInstance(isAvailable = true),
                            isSelected = true,
                        ),
                        eatsRetailCartItem_ActualizedTestInstance(
                            cartItem = cartItemTestInstance(userBuyCount = 5, isExpired = false),
                            itemInfo = eatsRetailItemInfoTestInstance(isAvailable = true),
                            isSelected = false,
                        ),
                    ),
                    shop = shopInfoTestInstance(
                        id = DUMMY_SHOP_ID,
                        businessId = DUMMY_BUSINESS_ID,
                    ),
                    additionalFees = listOf(eatsRetailCartAdditionalFeeTestInstance(title = "Сервисный сбор")),
                    errors = emptyList(),
                ),
            ),
            selectedByUserData = selectedByUserDataTestInstance(),
            cartFormatterConfig = cartFormatterConfig,
        )

        val expected = listOf(
            expectedEatsRetailCartVo(
                id = "id2",
                cartItemPacks = expectedItems,
                itemsCount = 21,
                itemsCountFormatted = "21 товар",
                isAnyItemAvailable = true,
                isRemoveItemsBlockActive = true,
                isSelectAllVisible = true,
                isSelectAllSelected = false,
                isCheckoutEnabled = true,
                summary = Synchronized(
                    expectedSummaryPriceVo(
                        "id2",
                        3,
                        expectedDiscounts(),
                        expectedAdditionalFees(),
                        expectedDelivery(42)
                    )
                ),
            ),
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Actualized retail carts without Selected items`() {
        val expectedItems = listOf<CartItemVo>(
            mock {
                on { count } doReturn 15
                on { isExpired } doReturn false
                on { isSelected } doReturn false
            },
            mock {
                on { count } doReturn 1
                on { isExpired } doReturn false
                on { isSelected } doReturn false
            },
        )

        cartItemFormatter.formatEatsRetailItems(any(), any(), any(), anyOrNull())
            .mockResult(expectedItems)

        val actual = formatter.formatEatsRetailCartVos(
            eatsRetailCarts = mapOf(
                "id2" to eatsRetailCart_ActualizedOnce_ActualizedTestInstance(
                    id = "id2",
                    items = listOf(
                        eatsRetailCartItem_ActualizedTestInstance(
                            cartItem = cartItemTestInstance(userBuyCount = 15, isExpired = false),
                            itemInfo = eatsRetailItemInfoTestInstance(isAvailable = true),
                            isSelected = false,
                        ),
                        eatsRetailCartItem_ActualizedTestInstance(
                            cartItem = cartItemTestInstance(userBuyCount = 1, isExpired = false),
                            itemInfo = eatsRetailItemInfoTestInstance(isAvailable = true),
                            isSelected = false,
                        ),
                    ),
                    shop = shopInfoTestInstance(
                        id = DUMMY_SHOP_ID,
                        businessId = DUMMY_BUSINESS_ID,
                    ),
                    additionalFees = listOf(eatsRetailCartAdditionalFeeTestInstance(title = "Сервисный сбор")),
                    errors = emptyList(),
                ),
            ),
            selectedByUserData = selectedByUserDataTestInstance(),
            cartFormatterConfig = cartFormatterConfig,
        )

        val expected = listOf(
            expectedEatsRetailCartVo(
                id = "id2",
                cartItemPacks = expectedItems,
                itemsCount = 16,
                itemsCountFormatted = "16 товаров",
                isAnyItemAvailable = true,
                isRemoveItemsBlockActive = false,
                isSelectAllVisible = true,
                isSelectAllSelected = false,
                isCheckoutEnabled = true,
                summary = Synchronized(
                    expectedSummaryPriceVo(
                        "id2",
                        2,
                        expectedDiscounts(),
                        expectedAdditionalFees(),
                        expectedDelivery(42)
                    )
                ),
            ),
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Actualized retail carts with one Expired item`() {
        val expectedItems = listOf<CartItemVo>(
            mock {
                on { count } doReturn 15
                on { isExpired } doReturn false
                on { isSelected } doReturn true
            },
            mock {
                on { count } doReturn 5
                on { isExpired } doReturn true
                on { isSelected } doReturn true
            },
        )

        cartItemFormatter.formatEatsRetailItems(any(), any(), any(), anyOrNull())
            .mockResult(expectedItems)

        val actual = formatter.formatEatsRetailCartVos(
            eatsRetailCarts = mapOf(
                "id2" to eatsRetailCart_ActualizedOnce_ActualizedTestInstance(
                    id = "id2",
                    items = listOf(
                        eatsRetailCartItem_ActualizedTestInstance(
                            cartItem = cartItemTestInstance(userBuyCount = 15, isExpired = false),
                            itemInfo = eatsRetailItemInfoTestInstance(isAvailable = true),
                            isSelected = true,
                        ),
                        eatsRetailCartItem_ActualizedTestInstance(
                            cartItem = cartItemTestInstance(userBuyCount = 5, isExpired = true),
                            itemInfo = eatsRetailItemInfoTestInstance(isAvailable = false),
                            isSelected = true,
                        ),
                    ),
                    shop = shopInfoTestInstance(
                        id = DUMMY_SHOP_ID,
                        businessId = DUMMY_BUSINESS_ID,
                    ),
                    additionalFees = listOf(eatsRetailCartAdditionalFeeTestInstance(title = "Сервисный сбор")),
                    errors = emptyList(),
                ),
            ),
            selectedByUserData = selectedByUserDataTestInstance(),
            cartFormatterConfig = cartFormatterConfig,
        )

        val expected = listOf(
            expectedEatsRetailCartVo(
                id = "id2",
                cartItemPacks = expectedItems,
                itemsCount = 20,
                itemsCountFormatted = "20 товаров",
                isAnyItemAvailable = true,
                isRemoveItemsBlockActive = true,
                isSelectAllVisible = true,
                isSelectAllSelected = true,
                isCheckoutEnabled = true,
                summary = Synchronized(
                    expectedSummaryPriceVo(
                        "id2",
                        2,
                        expectedDiscounts(),
                        expectedAdditionalFees(),
                        expectedDelivery(42)
                    )
                ),
            ),
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Actualized retail carts with all Expired items`() {
        val expectedItems = listOf<CartItemVo>(
            mock {
                on { count } doReturn 0
                on { isExpired } doReturn true
                on { isSelected } doReturn false
            },
            mock {
                on { count } doReturn 0
                on { isExpired } doReturn true
                on { isSelected } doReturn false
            },
        )

        cartItemFormatter.formatEatsRetailItems(any(), any(), any(), anyOrNull())
            .mockResult(expectedItems)

        val actual = formatter.formatEatsRetailCartVos(
            eatsRetailCarts = mapOf(
                "id2" to eatsRetailCart_ActualizedOnce_ActualizedTestInstance(
                    id = "id2",
                    items = listOf(
                        eatsRetailCartItem_ActualizedTestInstance(
                            cartItem = cartItemTestInstance(userBuyCount = 0, isExpired = true),
                            itemInfo = eatsRetailItemInfoTestInstance(isAvailable = false),
                            isSelected = false,
                        ),
                        eatsRetailCartItem_ActualizedTestInstance(
                            cartItem = cartItemTestInstance(userBuyCount = 0, isExpired = true),
                            itemInfo = eatsRetailItemInfoTestInstance(isAvailable = false),
                            isSelected = false,
                        ),
                    ),
                    shop = shopInfoTestInstance(
                        id = DUMMY_SHOP_ID,
                        businessId = DUMMY_BUSINESS_ID,
                    ),
                    additionalFees = listOf(eatsRetailCartAdditionalFeeTestInstance(title = "Сервисный сбор")),
                    errors = emptyList(),
                ),
            ),
            selectedByUserData = selectedByUserDataTestInstance(),
            cartFormatterConfig = cartFormatterConfig,
        )

        val expected = listOf(
            expectedEatsRetailCartVo(
                id = "id2",
                cartItemPacks = expectedItems,
                itemsCount = 0,
                itemsCountFormatted = "0 товаров",
                isAnyItemAvailable = false,
                isRemoveItemsBlockActive = false,
                isSelectAllVisible = false,
                isSelectAllSelected = false,
                isCheckoutEnabled = false,
                summary = Synchronized(
                    expectedSummaryPriceVo(
                        "id2",
                        2,
                        expectedDiscounts(),
                        expectedAdditionalFees(),
                        expectedDelivery(42)
                    )
                ),
            ),
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Actualized retail cart with Alert`() {
        val expectedItems = listOf<CartItemVo>(
            mock {
                on { count } doReturn 5
                on { isExpired } doReturn false
                on { isSelected } doReturn true
            },
        )

        cartItemFormatter.formatEatsRetailItems(any(), any(), any(), anyOrNull())
            .mockResult(expectedItems)

        val actual = formatter.formatEatsRetailCartVos(
            eatsRetailCarts = mapOf(
                "id3" to eatsRetailCart_ActualizedOnce_ActualizedTestInstance(
                    id = "id3",
                    items = listOf(
                        eatsRetailCartItem_ActualizedTestInstance(
                            cartItem = cartItemTestInstance(userBuyCount = 5, isExpired = false),
                            itemInfo = eatsRetailItemInfoTestInstance(isAvailable = true),
                            isSelected = true,
                        ),
                    ),
                    shop = shopInfoTestInstance(
                        id = DUMMY_SHOP_ID,
                        businessId = DUMMY_BUSINESS_ID,
                    ),
                    additionalFees = listOf(eatsRetailCartAdditionalFeeTestInstance(title = "Сервисный сбор")),
                    errors = listOf(eatsRetailErrorTestInstance()),
                ),
            ),
            selectedByUserData = selectedByUserDataTestInstance(),
            cartFormatterConfig = cartFormatterConfig,
        )

        val expected = listOf(
            expectedEatsRetailCartVo(
                id = "id3",
                cartItemPacks = expectedItems,
                itemsCount = 5,
                itemsCountFormatted = "5 товаров",
                isAlert = true,
                isAnyItemAvailable = true,
                isRemoveItemsBlockActive = true,
                isSelectAllVisible = true,
                isSelectAllSelected = true,
                isCheckoutEnabled = false,
                summary = Synchronized(
                    expectedSummaryPriceVo(
                        "id3",
                        1,
                        expectedDiscounts(),
                        expectedAdditionalFees(),
                        expectedDelivery(42)
                    )
                ),
            ),
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Actualized retail carts with Item with Error`() {
        val expectedItems = listOf<CartItemVo>(
            mock {
                on { count } doReturn 20
                on { isExpired } doReturn false
                on { isSelected } doReturn true
            },
            mock {
                on { count } doReturn 21
                on { isExpired } doReturn false
                on { isSelected } doReturn true
            },
        )

        cartItemFormatter.formatEatsRetailItems(any(), any(), any(), anyOrNull())
            .mockResult(expectedItems)

        val actual = formatter.formatEatsRetailCartVos(
            eatsRetailCarts = mapOf(
                "id2" to eatsRetailCart_ActualizedOnce_ActualizedTestInstance(
                    id = "id2",
                    items = listOf(
                        eatsRetailCartItem_ActualizedTestInstance(
                            cartItem = cartItemTestInstance(userBuyCount = 20, isExpired = false),
                            itemInfo = eatsRetailItemInfoTestInstance(isAvailable = true),
                            isSelected = true,
                        ),
                        eatsRetailCartItem_ActualizedWithErrorTestInstance(
                            cartItem = cartItemTestInstance(userBuyCount = 21, isExpired = false),
                        ),
                    ),
                    shop = shopInfoTestInstance(
                        id = DUMMY_SHOP_ID,
                        businessId = DUMMY_BUSINESS_ID,
                    ),
                    additionalFees = listOf(eatsRetailCartAdditionalFeeTestInstance(title = "Сервисный сбор")),
                    errors = emptyList(),
                ),
            ),
            selectedByUserData = selectedByUserDataTestInstance(),
            cartFormatterConfig = cartFormatterConfig,
        )

        val expected = listOf(
            expectedEatsRetailCartVo(
                id = "id2",
                cartItemPacks = expectedItems,
                itemsCount = 41,
                itemsCountFormatted = "41 товар",
                isAnyItemAvailable = true,
                isRemoveItemsBlockActive = true,
                isSelectAllVisible = true,
                isSelectAllSelected = false,
                isCheckoutEnabled = true,
                summary = Synchronized(
                    expectedSummaryPriceVo(
                        "id2",
                        2,
                        expectedDiscounts(),
                        expectedAdditionalFees(),
                        expectedDelivery(42)
                    )
                ),
            ),
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Actualized retail carts with all Items with Error`() {
        val expectedItems = listOf<CartItemVo>(
            mock {
                on { count } doReturn 0
                on { isExpired } doReturn false
                on { isSelected } doReturn true
            },
        )

        cartItemFormatter.formatEatsRetailItems(any(), any(), any(), anyOrNull())
            .mockResult(expectedItems)

        val actual = formatter.formatEatsRetailCartVos(
            eatsRetailCarts = mapOf(
                "id2" to eatsRetailCart_ActualizedOnce_ActualizedTestInstance(
                    id = "id2",
                    items = listOf(
                        eatsRetailCartItem_ActualizedWithErrorTestInstance(
                            cartItem = cartItemTestInstance(userBuyCount = 0, isExpired = false),
                        ),
                    ),
                    shop = shopInfoTestInstance(
                        id = DUMMY_SHOP_ID,
                        businessId = DUMMY_BUSINESS_ID,
                    ),
                    additionalFees = listOf(eatsRetailCartAdditionalFeeTestInstance(title = "Сервисный сбор")),
                    errors = emptyList(),
                ),
            ),
            selectedByUserData = selectedByUserDataTestInstance(),
            cartFormatterConfig = cartFormatterConfig,
        )

        val expected = listOf(
            expectedEatsRetailCartVo(
                id = "id2",
                cartItemPacks = expectedItems,
                itemsCount = 0,
                itemsCountFormatted = "0 товаров",
                isAnyItemAvailable = false,
                isRemoveItemsBlockActive = false,
                isSelectAllVisible = false,
                isSelectAllSelected = false,
                isCheckoutEnabled = false,
                summary = Synchronizing(),
            ),
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `Actualized with Errors retail carts`() {
        val expectedItems = listOf<CartItemVo>(
            mock {
                on { count } doReturn 21
                on { isExpired } doReturn true
                on { isSelected } doReturn true
            },
        )

        cartItemFormatter.formatEatsRetailItems(any(), any(), any(), anyOrNull())
            .mockResult(expectedItems)

        val actual = formatter.formatEatsRetailCartVos(
            eatsRetailCarts = mapOf(
                "id5" to eatsRetailCart_ActualizedWithErrorTestInstance(
                    id = "id5",
                    items = listOf(
                        eatsRetailCartItem_ActualizedWithErrorTestInstance(
                            cartItem = cartItemTestInstance(userBuyCount = 21, isExpired = true),
                        ),
                    ),
                    shop = shopInfoTestInstance(
                        id = DUMMY_SHOP_ID,
                        businessId = DUMMY_BUSINESS_ID,
                    ),
                ),
            ),
            selectedByUserData = selectedByUserDataTestInstance(),
            cartFormatterConfig = cartFormatterConfig,
        )

        val expected = listOf(
            expectedEatsRetailCartVo(
                id = "id5",
                cartItemPacks = expectedItems,
                itemsCount = 21,
                itemsCountFormatted = "21 товар",
                isAnyItemAvailable = false,
                isRemoveItemsBlockActive = false,
                isSelectAllVisible = false,
                isSelectAllSelected = false,
                isCheckoutEnabled = false,
                summary = Synchronizing(),
            ),
        )

        assertThat(actual).isEqualTo(expected)
    }

    private fun expectedDiscounts(): SummaryItem {
        return SummaryItem.Benefits.Discounts(
            name = "Скидка",
            price = MoneyVo.empty(),
        )
    }

    private fun expectedAdditionalFees(): SummaryItem {
        return SummaryItem.Regular(
            name = "Сервисный сбор",
            price = MoneyVo.empty(),
            leftDrawableResId = R.drawable.ic_question_cashback_summary,
            rightDrawableResId = null,
            textColor = R.color.black,
            hasBottomPadding = true,
            navigationTarget = SummaryItem.Regular.NavigationTarget.AboutRetailAdditionalFee(
                title = "Сервисный сбор",
                description = "description",
                confirmText = "confirmText",
            )
        )
    }

    private fun expectedDelivery(deliveryTimeMinutes: Long? = null): SummaryItem {
        return SummaryItem.Delivery(
            name = "Доставка",
            price = MoneyVo.empty(),
            deliveryTimeMinutes,
            icon = null,
            textColor = R.color.black,
        )
    }

    private fun expectedSummaryPriceVo(
        id: String,
        productsCount: Int,
        vararg summaryItems: SummaryItem,
    ): SummaryPriceVo {
        return SummaryPriceVo(
            summaryItems = summaryItems.toList(),
            isCreditVisible = false,
            canShowPromocode = false,
            isPromocodeVisible = false,
            productsCount = productsCount,
            cartType = expectedCartType(id),
        )
    }

    private fun expectedCartType(id: String): CartType.Retail {
        return CartType.Retail(
            cartId = id,
            shopName = "name",
        )
    }

    private fun expectedEatsRetailCartVo(
        id: String,
        cartItemPacks: List<CartElementVo>,
        itemsCount: Int,
        itemsCountFormatted: String,
        isAlert: Boolean = false,
        isAnyItemAvailable: Boolean,
        isRemoveItemsBlockActive: Boolean,
        isSelectAllVisible: Boolean,
        isSelectAllSelected: Boolean,
        isCheckoutEnabled: Boolean,
        summary: Synchronizable<SummaryPriceVo>,
    ): EatsRetailCartVo {
        val cartType = expectedCartType(id)

        return EatsRetailCartVo(
            cartItemPacks = cartItemPacks,
            cartTitleVo = CartTitleFlexibleVo(
                cartId = id,
                title = "name",
                toShopButtonTitle = "В магазин",
                message = null,
                isAlert = isAlert,
                itemsCountFormatted = itemsCountFormatted,
                itemsCount = itemsCount,
                gotoShopBlock = GotoShopInShopBlockVo(
                    businessId = DUMMY_BUSINESS_ID,
                    isExpress = true,
                ).takeIf { isAnyItemAvailable },
                removeBlock = RemoveBlockVo(
                    isRemoveBlockVisible = isAnyItemAvailable,
                    isRemoveItemsBlockActive = isRemoveItemsBlockActive,
                    isSelectAllVisible = isSelectAllVisible,
                    isSelectAllSelected = isSelectAllSelected,
                    removeButtonTitle = "Удалить выбранные",
                ),
                cartType = cartType,
            ),
            summary = summary,
            openCheckoutButton = CartButtonVo(
                cartType = cartType,
                isEnabled = isCheckoutEnabled,
                text = "Перейти к оформлению",
                textAboveButton = null,
                backgroundResource = CartButtonStyle.YELLOW,
            ),
            juridicalInfo = MulticartJuridicalInfoVo(
                dialogTitle = "Информация об исполнителе",
                bageTitle = "Заказ оформляет партнёр Маркета — Яндекс.Еда",
                cartType = cartType,
                shopId = DUMMY_SHOP_ID,
            ),
        )
    }

    private companion object {
        private const val DUMMY_SHOP_ID = 111L
        private const val DUMMY_BUSINESS_ID = 222L
    }
}
