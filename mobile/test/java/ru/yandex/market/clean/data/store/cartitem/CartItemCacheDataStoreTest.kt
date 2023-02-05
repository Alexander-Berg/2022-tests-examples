package ru.yandex.market.clean.data.store.cartitem

import com.yandex.alicekit.core.utils.Assert.assertNull
import com.yandex.alicekit.core.utils.Assert.setEnabled
import org.assertj.core.api.Assertions
import org.junit.Test
import ru.yandex.market.clean.domain.model.cartAffectingDataTestInstance
import ru.yandex.market.clean.domain.model.cartItemTestInstance
import ru.yandex.market.utils.Duration

class CartItemCacheDataStoreTest {

    private val configuration = CartItemCacheDataStore.Configuration(Duration(CACHE_DURATION))
    private val cartItemCacheDataStore = CartItemCacheDataStore(configuration)

    @Test
    fun `Check item in cache`() {
        val item = cartItemTestInstance()
        val cartAffectingData = cartAffectingDataTestInstance()
        cartItemCacheDataStore.setCartItems(cartAffectingData, listOf(item))
        val cartItems = cartItemCacheDataStore.getCartItemsCache()
        Assertions.assertThat(item).isEqualTo(cartItems?.firstOrNull())
    }

    @Test
    fun `Check item not in cache`() {
        val item = cartItemTestInstance()
        val cartAffectingData = cartAffectingDataTestInstance()
        cartItemCacheDataStore.setCartItems(cartAffectingData, listOf(item))
        cartItemCacheDataStore.clearCache().blockingGet()
        val cartItems = cartItemCacheDataStore.getCartItemsCache()
        setEnabled(true)
        assertNull(cartItems)
    }

    @Test
    fun `Check item not in cache by cart affecting data`() {
        val item = cartItemTestInstance()
        val cartAffectingData = cartAffectingDataTestInstance()
        cartItemCacheDataStore.setCartItems(cartAffectingData, listOf(item))
        val cartItems = cartItemCacheDataStore.getCartItemsCache(cartAffectingData.copy(personalPromoId = "newId"))
        setEnabled(true)
        assertNull(cartItems)
    }

    @Test
    fun `Check item in cache by cart affecting data`() {
        val item = cartItemTestInstance()
        val cartAffectingData = cartAffectingDataTestInstance()
        cartItemCacheDataStore.setCartItems(cartAffectingData, listOf(item))
        val cartItems = cartItemCacheDataStore.getCartItemsCache(cartAffectingData)
        Assertions.assertThat(item).isEqualTo(cartItems?.firstOrNull())
    }

    companion object {
        private const val CACHE_DURATION = 10.0
    }
}