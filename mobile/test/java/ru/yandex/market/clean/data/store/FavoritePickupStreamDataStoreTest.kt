package ru.yandex.market.clean.data.store

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.market.clean.domain.model.pickup.FavoritePickup

class FavoritePickupStreamDataStoreTest {
    private val store = FavoritePickupStreamDataStore()

    @Test
    fun `Duplicate pickups not added on empty store`() {
        val pickup = FavoritePickup(
            id = "PICKUP_ID",
            regionId = 0L,
            lastOrderDate = null
        )
        val authToken = null

        store.onAddFavoritePickup(pickup.id, pickup.regionId, authToken)
        assertThat(store.getStream(pickup.regionId, authToken).blockingFirst()).isEqualTo(listOf(pickup))

        store.onAddFavoritePickup(pickup.id, pickup.regionId, authToken)
        assertThat(store.getStream(pickup.regionId, authToken).blockingFirst()).isEqualTo(listOf(pickup))
    }

    @Test
    fun `Duplicate pickups not added on non-empty store`() {
        val pickup = FavoritePickup(
            id = "PICKUP_ID",
            regionId = 0L,
            lastOrderDate = null
        )
        val authToken = null

        store.setFavoritePickups(pickup.regionId, authToken, listOf(pickup))
        assertThat(store.getStream(pickup.regionId, authToken).blockingFirst()).isEqualTo(listOf(pickup))

        store.onAddFavoritePickup(pickup.id, pickup.regionId, authToken)
        assertThat(store.getStream(pickup.regionId, authToken).blockingFirst()).isEqualTo(listOf(pickup))
    }
}