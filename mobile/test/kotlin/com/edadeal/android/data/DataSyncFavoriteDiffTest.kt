package com.edadeal.android.data

import com.edadeal.android.data.datasync.DataSyncFavorites
import com.edadeal.android.model.favorites.FavoriteDiff
import com.edadeal.android.model.favorites.FavoriteDiffImpl
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class DataSyncFavoriteDiffTest(
    private val original: DataSyncFavorites,
    private val diff: FavoriteDiff,
    private val patched: DataSyncFavorites
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(
                getFavorites(
                    setOf("C"),
                    "a0" to "A", "a1" to "A"
                ),
                FavoriteDiffImpl(
                    addedRetailers = setOf("B"),
                    addedShops = mapOf("a0" to "A", "a2" to "A"),
                    removedRetailers = setOf("C")
                ),
                getFavorites(
                    setOf("B"),
                    "a0" to "A", "a1" to "A", "a2" to "A"
                )
            ),
            arrayOf(
                getFavorites(
                    setOf("B"),
                    "a0" to "A", "a1" to "A"
                ),
                FavoriteDiffImpl(),
                getFavorites(
                    setOf("B"),
                    "a0" to "A", "a1" to "A"
                )
            ),
            arrayOf(
                getFavorites(
                    emptySet()
                ),
                FavoriteDiffImpl(
                    addedRetailers = setOf("B"),
                    addedShops = mapOf("a0" to "A", "a1" to "A")
                ),
                getFavorites(
                    setOf("B"),
                    "a0" to "A", "a1" to "A"
                )
            )
        )

        private fun getFavorites(
            retailers: Set<String>,
            vararg shops: Pair<String, String>
        ) = DataSyncFavorites(
            revision = 0L,
            timestamp = 0L,
            retailers = retailers,
            shops = shops.toMap()
        )
    }

    @Test
    fun `patch should return correct data`() {
        val actual = original.patch(diff)

        assertEquals(patched.revision, actual.revision)
        assertEquals(patched.timestamp, actual.timestamp)
        assertEquals(patched.shops, actual.shops)
        assertEquals(patched.retailers, actual.retailers)
    }
}
