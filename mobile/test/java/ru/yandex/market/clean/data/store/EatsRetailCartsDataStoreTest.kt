package ru.yandex.market.clean.data.store

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.clean.domain.model.cartItemTestInstance
import ru.yandex.market.clean.domain.model.retail.EatsRetailCart.ActualizedOnce
import ru.yandex.market.clean.domain.model.retail.EatsRetailCart.ActualizedWithError
import ru.yandex.market.clean.domain.model.retail.EatsRetailCart.FromMarketCart
import ru.yandex.market.clean.domain.model.retail.eatsRetailCartItem_ActualizedTestInstance
import ru.yandex.market.clean.domain.model.retail.eatsRetailCartItem_ActualizedWithErrorTestInstance
import ru.yandex.market.clean.domain.model.retail.eatsRetailCart_ActualizedOnce_ActualizedTestInstance
import ru.yandex.market.clean.domain.model.retail.eatsRetailCart_ActualizedOnce_ActualizingTestInstance
import ru.yandex.market.clean.domain.model.retail.eatsRetailCart_ActualizedWithErrorTestInstance
import ru.yandex.market.clean.domain.model.retail.eatsRetailCart_FromMarketCartTestInstance
import ru.yandex.market.common.schedulers.EatsRetailCartsScheduler
import ru.yandex.market.optional.Optional
import ru.yandex.market.presentationSchedulersMock

class EatsRetailCartsDataStoreTest {

    private val presentationSchedulers = presentationSchedulersMock()

    private val dataStore = EatsRetailCartsDataStore(
        EatsRetailCartsScheduler(presentationSchedulers.localSingleThread)
    )

    private val marketCartItem = cartItemTestInstance()

    private val actualizedCartItem = eatsRetailCartItem_ActualizedTestInstance(
        cartItem = marketCartItem
    )

    private val actualizedWithErrorCartItem = eatsRetailCartItem_ActualizedWithErrorTestInstance(
        cartItem = marketCartItem
    )

    private val actualizedCart: ActualizedOnce.Actualized = eatsRetailCart_ActualizedOnce_ActualizedTestInstance(
        id = DUMMY_CART_ID
    )

    private val actualizingCart: ActualizedOnce.Actualizing = eatsRetailCart_ActualizedOnce_ActualizingTestInstance(
        id = DUMMY_CART_ID
    )

    private val actualizedWithErrorCart: ActualizedWithError = eatsRetailCart_ActualizedWithErrorTestInstance(
        id = DUMMY_CART_ID
    )

    private val cartFromMarket: FromMarketCart = eatsRetailCart_FromMarketCartTestInstance(
        id = DUMMY_CART_ID
    )

    @Test
    fun `should save actualized carts`() {
        val cartsFromMarket = mapOf(DUMMY_CART_ID to cartFromMarket)
        val actualizedCarts = mapOf(DUMMY_CART_ID to actualizedCart)
        dataStore.saveCarts(cartsFromMarket)
            .andThen(dataStore.saveActualizedCarts(actualizedCarts))
            .andThen(dataStore.getCartById(DUMMY_CART_ID))
            .test()
            .assertValue(Optional.of(actualizedCart))
    }

    @Test
    fun `should not save actualized carts if there is no carts before`() {
        dataStore.saveActualizedCarts(mapOf(DUMMY_CART_ID to actualizedCart))
            .andThen(dataStore.getCartById(DUMMY_CART_ID))
            .test()
            .assertValue(Optional.empty())
    }

    @Test
    fun `should not emit new same values if subscribed on distinct carts observable`() {
        val cartsFromMarket = mapOf(DUMMY_CART_ID to cartFromMarket)

        val cartsObservable = dataStore.observeCarts().test()

        dataStore.saveCarts(cartsFromMarket)
            .andThen(dataStore.saveActualizedCarts(cartsFromMarket))
            .repeat(5)
            .test()
            .assertComplete()

        cartsObservable.assertValueCount(2) //emptyMap -> saveCarts
    }

    @Test
    fun `should emit new same values if subscribed on without distinct carts observable`() {
        val cartsFromMarket = mapOf(DUMMY_CART_ID to cartFromMarket)

        val cartsObservable = dataStore.observeCartsWithoutDistinct().test()

        dataStore.saveCarts(cartsFromMarket)
            .andThen(dataStore.saveActualizedCarts(cartsFromMarket))
            .repeat(5)
            .test()
            .assertComplete()

        cartsObservable.assertValueCount(7) //emptyMap -> saveCarts -> saveActualizedCarts x5
    }

    @Test
    fun `should not invoke on next in cartsPublisher if new carts map is same`() {
        val cartsFromMarket = mapOf(DUMMY_CART_ID to cartFromMarket)

        val cartsObservable = dataStore.observeCartsWithoutDistinct().test()

        dataStore.saveCarts(cartsFromMarket)
            .andThen(dataStore.saveCarts(cartsFromMarket))
            .repeat(5)
            .test()
            .assertComplete()

        cartsObservable.assertValueCount(2) //emptyMap -> saveCarts
    }

    @Test
    fun `should change cart to actualizing if previously cart was actualized and items count changed`() {
        val newCartFromMarket = cartFromMarket.copyWithNewItems(
            items = listOf(
                actualizedCartItem.copy(
                    cartItem = marketCartItem.copy(userBuyCount = 111)
                )
            )
        )
        dataStore.saveCarts(mapOf(DUMMY_CART_ID to cartFromMarket))
            .andThen(dataStore.saveActualizedCarts(mapOf(DUMMY_CART_ID to actualizedCart)))
            .andThen(dataStore.saveCarts(mapOf(DUMMY_CART_ID to newCartFromMarket as FromMarketCart)))
            .andThen(dataStore.getCartById(DUMMY_CART_ID))
            .test()
            .assertValue(Optional.ofNullable(actualizedCart.toActualizing(newCartFromMarket.items)))
    }

    @Test
    fun `should change cart to actualizing if previous cart was actualized and isSelected changed`() {
        val newCartFromMarket = cartFromMarket.copyWithNewItems(
            items = listOf(
                actualizedCartItem.copy(
                    isSelected = false
                )
            )
        )
        dataStore.saveCarts(mapOf(DUMMY_CART_ID to cartFromMarket))
            .andThen(dataStore.saveActualizedCarts(mapOf(DUMMY_CART_ID to actualizedCart)))
            .andThen(dataStore.saveCarts(mapOf(DUMMY_CART_ID to newCartFromMarket as FromMarketCart)))
            .andThen(dataStore.getCartById(DUMMY_CART_ID))
            .test()
            .assertValue(Optional.ofNullable(actualizedCart.toActualizing(newCartFromMarket.items)))
    }

    @Test
    fun `should not change cart if changed item count and it was actualized with error before`() {
        val actualizedCart = actualizedCart.copyWithNewItems(
            items = listOf(
                actualizedWithErrorCartItem.copy(
                    cartItem = marketCartItem.copy(userBuyCount = 0)
                )
            )
        )
        val newCartFromMarket = cartFromMarket.copyWithNewItems(
            items = listOf(
                actualizedCartItem.copy(
                    cartItem = marketCartItem.copy(userBuyCount = 111)
                )
            )
        )
        dataStore.saveCarts(mapOf(DUMMY_CART_ID to cartFromMarket))
            .andThen(dataStore.saveActualizedCarts(mapOf(DUMMY_CART_ID to actualizedCart)))
            .andThen(dataStore.saveCarts(mapOf(DUMMY_CART_ID to newCartFromMarket as FromMarketCart)))
            .andThen(dataStore.getCartById(DUMMY_CART_ID))
            .test()
            .assertValue(Optional.ofNullable(actualizedCart))
    }

    @Test
    fun `should return actualized cart if new cart from market is same and count of items not changed`() {
        dataStore.saveCarts(mapOf(DUMMY_CART_ID to cartFromMarket))
            .andThen(dataStore.saveActualizedCarts(mapOf(DUMMY_CART_ID to actualizedCart)))
            .andThen(dataStore.saveCarts(mapOf(DUMMY_CART_ID to cartFromMarket)))
            .andThen(dataStore.getCartById(DUMMY_CART_ID))
            .test()
            .assertValue(Optional.ofNullable(actualizedCart))
    }

    @Test
    fun `carts are same`() {
        assertThat(dataStore.isCartsEquals(actualizedCart, actualizedCart)).isTrue
    }

    @Test
    fun `should return that carts differs when new cart Actualized`() {
        assertThat(dataStore.isCartsEquals(cartFromMarket, actualizedCart)).isFalse
    }

    @Test
    fun `should return that carts differs when new cart ActualizedWithError`() {
        assertThat(dataStore.isCartsEquals(actualizedCart, actualizedWithErrorCart)).isFalse
    }

    @Test
    fun `should return that carts differs when new cart has different cart items`() {
        assertThat(
            dataStore.isCartsEquals(
                actualizedCart, cartFromMarket.copyWithNewItems(listOf(actualizedCartItem, actualizedCartItem))
            )
        ).isFalse
    }

    @Test
    fun `should return new cart if cached is null`() {
        assertThat(dataStore.mergeCarts(null, cartFromMarket)).isEqualTo(cartFromMarket)
    }


    @Test
    fun `should return new cart if it is actualized`() {
        assertThat(dataStore.mergeCarts(cartFromMarket, actualizedCart)).isEqualTo(actualizedCart)
    }

    @Test
    fun `should return new cart if it is Actualized`() {
        assertThat(dataStore.mergeCarts(actualizedCart, actualizedWithErrorCart)).isEqualTo(actualizedWithErrorCart)
    }

    @Test
    fun `should return cached cart if it's equals to new cart`() {
        assertThat(dataStore.mergeCarts(actualizedCart, actualizedCart)).isEqualTo(actualizedCart)
    }

    @Test
    fun `should return actualizing cart if new cart is not equals to cached`() {
        val newCartFromMarket = cartFromMarket.copyWithNewItems(
            items = listOf(actualizedCartItem, actualizedCartItem, actualizedCartItem)
        )
        val actualizingCart = actualizingCart.copyWithNewItems(
            items = listOf(actualizedCartItem, actualizedCartItem, actualizedCartItem)
        )
        assertThat(
            dataStore.mergeCarts(actualizedCart, newCartFromMarket)
        ).isEqualTo(actualizingCart)
    }

    private companion object {
        const val DUMMY_CART_ID = "DUMMY_CART_ID"
    }
}
