package ru.yandex.market.clean.data.repository

import dagger.Lazy
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.analytics.facades.health.EatsRetailHealthFacade
import ru.yandex.market.clean.data.fapi.source.retail.RetailFapiClient
import ru.yandex.market.clean.data.mapper.EatsRetailCartMapper
import ru.yandex.market.clean.data.model.dto.retail.RetailActualizeCartDto
import ru.yandex.market.clean.data.model.dto.retail.retailActualizeCartDtoTestInstance
import ru.yandex.market.clean.data.repository.cart.CartPartialPurchaseRepository
import ru.yandex.market.clean.data.repository.retail.RetailRepository
import ru.yandex.market.clean.data.store.EatsRetailCartsDataStore
import ru.yandex.market.clean.domain.model.CartItem
import ru.yandex.market.clean.domain.model.cartIdentifierTestInstance
import ru.yandex.market.clean.domain.model.cartItemTestInstance
import ru.yandex.market.clean.domain.model.retail.EatsRetailCart
import ru.yandex.market.clean.domain.model.retail.EatsRetailCartItem
import ru.yandex.market.clean.domain.model.retail.eatsRetailCartItem_NotActualizedTestInstance
import ru.yandex.market.clean.domain.model.retail.eatsRetailCart_ActualizedOnce_ActualizedTestInstance
import ru.yandex.market.clean.domain.model.retail.eatsRetailCart_FromMarketCartTestInstance
import ru.yandex.market.clean.domain.model.shop.ShopInfo
import ru.yandex.market.clean.domain.model.shop.shopInfoTestInstance
import ru.yandex.market.optional.Optional

class RetailRepositoryTest {

    private val retailFapiClient = mock<RetailFapiClient> {
        on { deleteItemsFromEatsCart(SHOP_ID, emptyList(), "") } doReturn Completable.complete()
    }

    private val eatsRetailCartsDataStore = mock<EatsRetailCartsDataStore> {
        on { observeCartsWithoutDistinct() } doReturn Observable.just(emptyMap())

        on { observeCarts() } doReturn Observable.just(emptyMap())

        on { getCartById(SHOP_ID) } doReturn Single.just(Optional.empty())

        on { saveActualizedCarts(any()) } doReturn Completable.complete()
    }

    private val eatsRetailCartMapper = mock<EatsRetailCartMapper>()

    private val cartPartialPurchaseRepository = mock<CartPartialPurchaseRepository> {
        on { isSelected(any()) } doReturn true
    }

    private val eatsRetailHealthFacade = mock<EatsRetailHealthFacade>()

    private val lazyEatsRetailHealthFacade = mock<Lazy<EatsRetailHealthFacade>> {
        on { get() } doReturn eatsRetailHealthFacade
    }

    private val retailRepository = RetailRepository(
        retailFapiClient = retailFapiClient,
        cartPartialPurchaseRepository = cartPartialPurchaseRepository,
        eatsRetailCartMapper = eatsRetailCartMapper,
        eatsRetailCartsDataStore = eatsRetailCartsDataStore,
        eatsRetailHealthFacade = lazyEatsRetailHealthFacade,
    )

    private val retailActualizeCartDto: RetailActualizeCartDto = retailActualizeCartDtoTestInstance(
        shopId = SHOP_ID
    )

    private val shopInfo: ShopInfo = shopInfoTestInstance(
        id = SHOP_ID.toLong()
    )

    private val eatsRetailCartFromMarketCart: EatsRetailCart.FromMarketCart = eatsRetailCart_FromMarketCartTestInstance(
        shop = shopInfo
    )

    private val cartIdentifiers: Set<String> = setOf(
        CART_IDENTIFIER_ID_1,
        CART_IDENTIFIER_ID_2,
        CART_IDENTIFIER_ID_3,
    )

    private val cartItems1: List<CartItem> = listOf(
        cartItemTestInstance(cartIdentifier = cartIdentifierTestInstance(id = cartIdentifiers.first())),
        cartItemTestInstance(cartIdentifier = cartIdentifierTestInstance(id = cartIdentifiers.first())),
        cartItemTestInstance(cartIdentifier = cartIdentifierTestInstance(id = cartIdentifiers.first())),
    )

    private val cartItems2: List<CartItem> = listOf(
        cartItemTestInstance(cartIdentifier = cartIdentifierTestInstance(id = cartIdentifiers.elementAt(1))),
        cartItemTestInstance(cartIdentifier = cartIdentifierTestInstance(id = cartIdentifiers.elementAt(1))),
    )

    private val cartItems3: List<CartItem> = listOf(
        cartItemTestInstance(cartIdentifier = cartIdentifierTestInstance(id = cartIdentifiers.last())),
    )

    private val cartItems: List<CartItem> = cartItems1
        .plus(cartItems2)
        .plus(cartItems3)

    @Test
    fun `create retail cart with positive result`() {
        val isCartCreated = true

        whenever(
            retailFapiClient.createEatsCart(any())
        ) doReturn Single.just(
            retailActualizeCartDto.copy(
                isCartCreated = isCartCreated,
                shopId = SHOP_ID,
            )
        )

        retailRepository.createEatsCart(
            eatsRetailCart = eatsRetailCartFromMarketCart,
            appMetricUuid = "",
            deviceId = ""
        )
            .test()
            .assertResult(isCartCreated)

        verify(retailFapiClient, times(1)).createEatsCart(any())
    }

    @Test
    fun `create retail cart with negative result`() {
        val isCartCreated = false

        whenever(
            retailFapiClient.createEatsCart(any())
        ) doReturn Single.just(
            retailActualizeCartDto.copy(
                isCartCreated = isCartCreated,
                shopId = SHOP_ID,
            )
        )

        retailRepository.createEatsCart(
            eatsRetailCart = eatsRetailCartFromMarketCart,
            appMetricUuid = "",
            deviceId = ""
        )
            .test()
            .assertResult(isCartCreated)

        verify(retailFapiClient, times(1)).createEatsCart(any())
    }

    @Test
    fun `delete items from retail cart`() {
        retailRepository.deleteItemsFromEatsCart(SHOP_ID.toLong(), emptyList(), "")
            .test()
            .assertComplete()

        verify(retailFapiClient, times(1)).deleteItemsFromEatsCart(SHOP_ID, emptyList(), "")
    }

    @Test
    fun `observe retail carts without distinct`() {
        retailRepository.observeEatsRetailCartsWithoutDistinct()
            .test()
            .assertResult(emptyMap())

        verify(eatsRetailCartsDataStore, times(1)).observeCartsWithoutDistinct()
    }

    @Test
    fun `observe retail carts with distinct`() {
        retailRepository.observeEatsRetailCarts()
            .test()
            .assertResult(emptyMap())

        verify(eatsRetailCartsDataStore, times(1)).observeCarts()
    }

    @Test
    fun `get cart by id`() {
        retailRepository.getCartById(SHOP_ID)
            .test()
            .assertResult(Optional.empty())

        verify(eatsRetailCartsDataStore, times(1)).getCartById(SHOP_ID)
    }

    @Test
    fun `save cart items as retail with empty cartItems list`() {
        whenever(eatsRetailCartsDataStore.saveCarts(emptyMap())) doReturn Completable.complete()

        retailRepository.saveCartItemsAsRetailCarts(emptyList())
            .test()
            .assertComplete()

        verify(eatsRetailCartsDataStore, times(1)).saveCarts(emptyMap())
    }

    @Test
    fun `check that on save cart items as retail mapping not called`() {
        whenever(eatsRetailCartsDataStore.saveCarts(emptyMap())) doReturn Completable.complete()

        retailRepository.saveCartItemsAsRetailCarts(emptyList())
            .test()
            .assertComplete()

        verify(eatsRetailCartMapper, times(0)).mapToNotActualizedOrNull(any(), any(), any())
    }

    @Test
    fun `check save emptyMap with cartItems with nullable shop`() {
        val cartItemsWithAllNullableShop = listOf(cartItemTestInstance(shop = null))

        whenever(eatsRetailCartsDataStore.saveCarts(any())) doReturn Completable.complete()

        retailRepository.saveCartItemsAsRetailCarts(cartItemsWithAllNullableShop)
            .test()
            .assertComplete()

        verify(eatsRetailCartsDataStore, times(1)).saveCarts(emptyMap())
    }

    @Test
    fun `mapping should not be called on cartItems with nullable shop saving`() {
        val cartItemsWithAllNullableShop = listOf(cartItemTestInstance(shop = null))

        whenever(eatsRetailCartsDataStore.saveCarts(any())) doReturn Completable.complete()

        retailRepository.saveCartItemsAsRetailCarts(cartItemsWithAllNullableShop)
            .test()
            .assertComplete()

        verify(eatsRetailCartMapper, times(0)).mapToNotActualizedOrNull(any(), any(), any())
    }

    @Test
    fun `save cart items with all correct data`() {
        whenever(eatsRetailCartsDataStore.saveCarts(any())) doReturn Completable.complete()
        whenever(
            eatsRetailCartMapper.mapToNotActualizedOrNull(any(), any(), any())
        ) doReturn eatsRetailCartFromMarketCart

        retailRepository.saveCartItemsAsRetailCarts(cartItems)
            .test()
            .assertComplete()

        verify(eatsRetailCartMapper, times(cartIdentifiers.size)).mapToNotActualizedOrNull(any(), any(), any())
        verify(cartPartialPurchaseRepository, times(cartItems.size)).isSelected(any())
        verify(eatsRetailCartsDataStore, times(1)).saveCarts(any())
    }

    @Test
    fun `on empty cart actualization should be called only emptyMap saving`() {
        retailRepository.getRetailActualizedCart(emptyMap(), "", "")
            .test()
            .assertNoErrors()
            .assertValue {
                it.isEmpty()
            }

        verify(cartPartialPurchaseRepository, times(0)).isSelected(any())
        verify(retailFapiClient, times(0)).resolveRetailActualizedCart(any())
        verify(eatsRetailCartMapper, times(0)).mapToActualizedOrNull(any(), any(), any())
        verify(eatsRetailCartsDataStore, times(1)).saveActualizedCarts(emptyMap())
    }

    @Test
    fun `actualization with non empty eatsRetailCarts and all available items`() {
        val cart = eatsRetailCartFromMarketCart.copy(
            items = listOf(
                eatsRetailCartItem_NotActualizedTestInstance(
                    cartItem = cartItemTestInstance(isExpired = false)
                ),
                eatsRetailCartItem_NotActualizedTestInstance(
                    cartItem = cartItemTestInstance(isExpired = false)
                )
            )
        )
        val eatsRetailCarts = mapOf(SHOP_ID to cart)
        val retailActualizedCart = eatsRetailCart_ActualizedOnce_ActualizedTestInstance(id = SHOP_ID)

        whenever(
            retailFapiClient.resolveRetailActualizedCart(any())
        ) doReturn Single.just(mapOf(SHOP_ID to retailActualizeCartDto))

        whenever(
            eatsRetailCartMapper.mapToActualizedOrNull(any(), any(), any())
        ) doReturn retailActualizedCart

        retailRepository.getRetailActualizedCart(eatsRetailCarts, "", "")
            .test()
            .assertNoErrors()
            .assertValue { value ->
                value.size == 1
                    && value.containsKey(SHOP_ID)
                    && value[SHOP_ID] is EatsRetailCart.ActualizedOnce
                    && value[SHOP_ID]?.items?.all { it is EatsRetailCartItem.Actualized } == true
            }

        verify(retailFapiClient, times(1)).resolveRetailActualizedCart(any())
        verify(eatsRetailCartMapper, times(1)).mapToActualizedOrNull(any(), any(), any())
        verify(eatsRetailCartsDataStore, times(1)).saveActualizedCarts(any())
        verify(eatsRetailHealthFacade, times(0)).actualizedCartItemsIsEmpty(any(), any())
        verify(eatsRetailHealthFacade, times(0)).actualizationError(any())
        verify(eatsRetailHealthFacade, times(0)).actualizedCartIsNull(any(), any())
    }

    @Test
    fun `actualization with one expired item`() {
        val cart = eatsRetailCartFromMarketCart.copy(
            items = listOf(
                eatsRetailCartItem_NotActualizedTestInstance(
                    cartItem = cartItemTestInstance(isExpired = true)
                ),
                eatsRetailCartItem_NotActualizedTestInstance(
                    cartItem = cartItemTestInstance(isExpired = false)
                )
            )
        )
        val eatsRetailCarts = mapOf(SHOP_ID to cart)
        val retailActualizedCart = eatsRetailCart_ActualizedOnce_ActualizedTestInstance(id = SHOP_ID)

        whenever(
            retailFapiClient.resolveRetailActualizedCart(any())
        ) doReturn Single.just(mapOf(SHOP_ID to retailActualizeCartDto))

        whenever(
            eatsRetailCartMapper.mapToActualizedOrNull(any(), any(), any())
        ) doReturn retailActualizedCart

        retailRepository.getRetailActualizedCart(eatsRetailCarts, "", "")
            .test()
            .assertNoErrors()
            .assertValue { value ->
                value.size == 1
                    && value.containsKey(SHOP_ID)
                    && value[SHOP_ID] is EatsRetailCart.ActualizedOnce
                    && value[SHOP_ID]?.items?.all { it is EatsRetailCartItem.Actualized } == true
            }

        verify(retailFapiClient, times(1)).resolveRetailActualizedCart(any())
        verify(eatsRetailCartMapper, times(1)).mapToActualizedOrNull(any(), any(), any())
        verify(eatsRetailCartsDataStore, times(1)).saveActualizedCarts(any())
        verify(eatsRetailHealthFacade, times(0)).actualizedCartItemsIsEmpty(any(), any())
        verify(eatsRetailHealthFacade, times(0)).actualizationError(any())
        verify(eatsRetailHealthFacade, times(0)).actualizedCartIsNull(any(), any())
    }

    @Test
    fun `actualization with one item with empty userBuyCount`() {
        val cart = eatsRetailCartFromMarketCart.copy(
            items = listOf(
                eatsRetailCartItem_NotActualizedTestInstance(
                    cartItem = cartItemTestInstance(isExpired = false, userBuyCount = 0)
                ),
                eatsRetailCartItem_NotActualizedTestInstance(
                    cartItem = cartItemTestInstance(isExpired = false)
                )
            )
        )
        val eatsRetailCarts = mapOf(SHOP_ID to cart)
        val retailActualizedCart = eatsRetailCart_ActualizedOnce_ActualizedTestInstance(id = SHOP_ID)

        whenever(
            retailFapiClient.resolveRetailActualizedCart(any())
        ) doReturn Single.just(mapOf(SHOP_ID to retailActualizeCartDto))

        whenever(
            eatsRetailCartMapper.mapToActualizedOrNull(any(), any(), any())
        ) doReturn retailActualizedCart

        retailRepository.getRetailActualizedCart(eatsRetailCarts, "", "")
            .test()
            .assertNoErrors()
            .assertValue { value ->
                value.size == 1
                    && value.containsKey(SHOP_ID)
                    && value[SHOP_ID] is EatsRetailCart.ActualizedOnce
                    && value[SHOP_ID]?.items?.all { it is EatsRetailCartItem.Actualized } == true
            }

        verify(retailFapiClient, times(1)).resolveRetailActualizedCart(any())
        verify(eatsRetailCartMapper, times(1)).mapToActualizedOrNull(any(), any(), any())
        verify(eatsRetailCartsDataStore, times(1)).saveActualizedCarts(any())
        verify(eatsRetailHealthFacade, times(0)).actualizedCartItemsIsEmpty(any(), any())
        verify(eatsRetailHealthFacade, times(0)).actualizationError(any())
        verify(eatsRetailHealthFacade, times(0)).actualizedCartIsNull(any(), any())
    }

    @Test
    fun `actualization with empty items`() {
        val cart = eatsRetailCartFromMarketCart.copy(
            items = emptyList()
        )
        val eatsRetailCarts = mapOf(SHOP_ID to cart)

        retailRepository.getRetailActualizedCart(eatsRetailCarts, "", "")
            .test()
            .assertNoErrors()
            .assertValue { value ->
                value.size == 1
                    && value.containsKey(SHOP_ID)
                    && value[SHOP_ID] is EatsRetailCart.ActualizedWithError
            }

        verify(retailFapiClient, times(0)).resolveRetailActualizedCart(any())
        verify(eatsRetailHealthFacade, times(1)).actualizedCartIsNull(any(), any())
        verify(eatsRetailHealthFacade, times(0)).actualizationError(any())
        verify(eatsRetailHealthFacade, times(0)).actualizedCartItemsIsEmpty(any(), any())
        verify(eatsRetailCartMapper, times(0)).mapToActualizedOrNull(any(), any(), any())
        verify(eatsRetailCartsDataStore, times(1)).saveActualizedCarts(any())
    }

    @Test
    fun `actualization with error on fapi call`() {
        val cart = eatsRetailCartFromMarketCart.copy(
            items = listOf(
                eatsRetailCartItem_NotActualizedTestInstance(
                    cartItem = cartItemTestInstance(isExpired = false)
                ),
                eatsRetailCartItem_NotActualizedTestInstance(
                    cartItem = cartItemTestInstance(isExpired = false)
                )
            )
        )
        val eatsRetailCarts = mapOf(SHOP_ID to cart)

        whenever(
            retailFapiClient.resolveRetailActualizedCart(any())
        ) doReturn Single.error(RuntimeException())

        retailRepository.getRetailActualizedCart(eatsRetailCarts, "", "")
            .test()
            .assertNoErrors()
            .assertValue { value ->
                value.size == 1
                    && value.containsKey(SHOP_ID)
                    && value[SHOP_ID] is EatsRetailCart.ActualizedWithError
            }

        verify(retailFapiClient, times(1)).resolveRetailActualizedCart(any())
        verify(eatsRetailHealthFacade, times(1)).actualizationError(any())
        verify(eatsRetailHealthFacade, times(0)).actualizedCartIsNull(any(), any())
        verify(eatsRetailHealthFacade, times(0)).actualizedCartItemsIsEmpty(any(), any())
        verify(eatsRetailCartMapper, times(0)).mapToActualizedOrNull(any(), any(), any())
        verify(eatsRetailCartsDataStore, times(1)).saveActualizedCarts(any())
    }

    @Test
    fun `actualization with returned null on mapping`() {
        val cart = eatsRetailCartFromMarketCart.copy(
            items = listOf(
                eatsRetailCartItem_NotActualizedTestInstance(
                    cartItem = cartItemTestInstance(isExpired = false)
                ),
                eatsRetailCartItem_NotActualizedTestInstance(
                    cartItem = cartItemTestInstance(isExpired = false)
                )
            )
        )
        val eatsRetailCarts = mapOf(SHOP_ID to cart)

        whenever(
            eatsRetailCartMapper.mapToActualizedOrNull(any(), any(), any())
        ) doReturn null

        whenever(
            retailFapiClient.resolveRetailActualizedCart(any())
        ) doReturn Single.just(mapOf(SHOP_ID to retailActualizeCartDto))

        retailRepository.getRetailActualizedCart(eatsRetailCarts, "", "")
            .test()
            .assertNoErrors()
            .assertValue { value ->
                value.size == 1
                    && value.containsKey(SHOP_ID)
                    && value[SHOP_ID] is EatsRetailCart.ActualizedWithError
            }

        verify(retailFapiClient, times(1)).resolveRetailActualizedCart(any())
        verify(eatsRetailHealthFacade, times(1)).actualizedCartItemsIsEmpty(any(), any())
        verify(eatsRetailHealthFacade, times(0)).actualizationError(any())
        verify(eatsRetailHealthFacade, times(0)).actualizedCartIsNull(any(), any())
        verify(eatsRetailCartMapper, times(1)).mapToActualizedOrNull(any(), any(), any())
        verify(eatsRetailCartsDataStore, times(1)).saveActualizedCarts(any())
    }

    private companion object {
        private const val SHOP_ID = "123456"
        private const val CART_IDENTIFIER_ID_1 = SHOP_ID
        private const val CART_IDENTIFIER_ID_2 = "654"
        private const val CART_IDENTIFIER_ID_3 = "321"
    }
}
