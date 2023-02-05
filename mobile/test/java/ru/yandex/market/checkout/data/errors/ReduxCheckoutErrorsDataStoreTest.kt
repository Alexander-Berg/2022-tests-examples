package ru.yandex.market.checkout.data.errors

import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import ru.yandex.market.base.redux.store.LegacyCompatibleAppStateStore
import ru.yandex.market.base.redux.store.configureStore
import ru.yandex.market.checkout.domain.model.ErrorsPack
import ru.yandex.market.checkout.domain.model.ShopErrorsPack
import ru.yandex.market.redux.reducers.AppReducer
import ru.yandex.market.redux.states.AppState

class ReduxCheckoutErrorsDataStoreTest {

    private val reduxStore = LegacyCompatibleAppStateStore(
        appStateStore = configureStore(AppState()) { reducer = AppReducer() },
        reduxCommonHealthAnalytics = mock()
    )

    private lateinit var dataStore: CheckoutErrorsDataStore

    @Before
    fun setup() {
        dataStore = ReduxCheckoutErrorsDataStore(reduxStore)
    }

    @Test
    fun `Empty store has no error packs`() {
        val errorsObserver = dataStore.getErrorsStream().test()
        errorsObserver.assertEmpty()
    }

    @Test
    fun `Non empty store has packs`() {
        val emptyPack = ErrorsPack.empty()
        val nonEmptyPack = emptyPack.copy(
            shopErrorsPacks = listOf(ShopErrorsPack.testBuilder().build())
        )
        val errorsObserver = dataStore.getErrorsStream().test()
        dataStore.onNewErrorsPack(emptyPack)
        dataStore.onNewErrorsPack(nonEmptyPack)

        errorsObserver
            .assertNoErrors()

        errorsObserver
            .assertValueCount(2)
            .assertValueAt(0, emptyPack)
            .assertValueAt(1, nonEmptyPack)
    }

}