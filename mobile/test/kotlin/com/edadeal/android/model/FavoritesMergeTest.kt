@file:Suppress("LongMethod")
package com.edadeal.android.model

import com.edadeal.android.data.datasync.DataSyncFavorites
import com.edadeal.android.model.favorites.FavoriteDiff
import com.edadeal.android.model.favorites.FavoriteDiffImpl
import com.edadeal.android.model.favorites.FavoritesMergeProcessor
import com.edadeal.android.model.favorites.FavoritesMergeProcessor.Action
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class FavoritesMergeTest(
    private val message: String,
    private val base: DataSyncFavorites,
    private val local: DataSyncFavorites,
    private val remote: DataSyncFavorites,
    private val mergeAction: Action
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<out Any>> = listOf(
            arrayOf(
                "should do nothing if revision is same and local diff is empty",
                DataSyncFavorites(1L, 1L, setOf("A"), mapOf("b" to "B")),
                DataSyncFavorites(1L, 2L, setOf("A"), mapOf("b" to "B")),
                DataSyncFavorites(1L, 1L, setOf("A"), mapOf("b" to "B")),
                Action.Noop
            ),
            arrayOf(
                "should update remote if remote diff is empty and local diff is not empty",
                DataSyncFavorites(0L, 0L, emptySet(), emptyMap()),
                DataSyncFavorites(0L, 0L, setOf("A"), mapOf("b" to "B")),
                DataSyncFavorites(0L, 0L, emptySet(), emptyMap()),
                Action.UpdateRemote(
                    remoteDiff = FavoriteDiffImpl(
                        addedRetailers = setOf("A"),
                        addedShops = mapOf("b" to "B")
                    )
                )
            ),
            arrayOf(
                "should update local if local revision is lower and remote diff is not empty",
                DataSyncFavorites(1L, 1L, setOf("A"), mapOf("b" to "B")),
                DataSyncFavorites(1L, 2L, setOf("A", "C"), mapOf("b" to "B")),
                DataSyncFavorites(2L, 3L, setOf("A", "C"), mapOf("b" to "B", "e" to "E")),
                Action.UpdateLocal(
                    localDiff = FavoriteDiffImpl(
                        addedShops = mapOf("e" to "E")
                    ),
                    revision = 2L,
                    timestamp = 3L
                )
            ),
            arrayOf(
                "should update both if no conflict",
                DataSyncFavorites(0L, 0L, emptySet(), emptyMap()),
                DataSyncFavorites(0L, 0L, setOf("A"), mapOf("ba" to "B")),
                DataSyncFavorites(1L, 1L, setOf("C"), mapOf("bb" to "B")),
                Action.UpdateBoth(
                    remoteDiff = FavoriteDiffImpl(
                        addedRetailers = setOf("A"),
                        addedShops = mapOf("ba" to "B")
                    ),
                    localDiff = FavoriteDiffImpl(
                        addedRetailers = setOf("C"),
                        addedShops = mapOf("bb" to "B")
                    )
                )
            ),
            arrayOf(
                "should update local if remote changes are subset of local",
                DataSyncFavorites(1L, 1L, setOf("A"), mapOf("ba" to "B")),
                DataSyncFavorites(1L, 2L, setOf("A"), mapOf("bb" to "B")),
                DataSyncFavorites(2L, 3L, setOf("A", "C"), mapOf("bb" to "B")),
                Action.UpdateLocal(
                    localDiff = FavoriteDiffImpl(
                        addedRetailers = setOf("C")
                    ),
                    revision = 2L,
                    timestamp = 3L
                )
            ),
            arrayOf(
                "should update local on conflict if local timestamp is old",
                DataSyncFavorites(1L, 1L, setOf("C"), emptyMap()),
                DataSyncFavorites(1L, 2L, setOf("A"), mapOf("b" to "B")),
                DataSyncFavorites(2L, 3L, setOf("B"), mapOf("a" to "A")),
                Action.UpdateLocal(
                    localDiff = FavoriteDiffImpl(
                        addedRetailers = setOf("B"),
                        addedShops = mapOf("a" to "A"),
                        removedRetailers = setOf("A"),
                        removedShops = mapOf("b" to "B")
                    ),
                    revision = 2L,
                    timestamp = 3L
                )
            ),
            arrayOf(
                "should update remote on conflict if remote timestamp is old",
                DataSyncFavorites(1L, 1L, setOf("C"), emptyMap()),
                DataSyncFavorites(1L, 3L, setOf("A"), mapOf("b" to "B")),
                DataSyncFavorites(2L, 2L, setOf("B"), mapOf("a" to "A")),
                Action.UpdateRemote(
                    remoteDiff = FavoriteDiffImpl(
                        addedRetailers = setOf("A"),
                        addedShops = mapOf("b" to "B"),
                        removedRetailers = setOf("B"),
                        removedShops = mapOf("a" to "A")
                    )
                )
            ),
            arrayOf(
                "should update both if no conflict with remote",
                DataSyncFavorites(1L, 1L, setOf("A"), mapOf("b" to "B")),
                DataSyncFavorites(1L, 2L, setOf("A", "C"), mapOf("b" to "B")),
                DataSyncFavorites(2L, 3L, setOf("A", "D"), mapOf("b" to "B", "e" to "E")),
                Action.UpdateBoth(
                    remoteDiff = FavoriteDiffImpl(
                        addedRetailers = setOf("C")
                    ),
                    localDiff = FavoriteDiffImpl(
                        addedRetailers = setOf("D"),
                        addedShops = mapOf("e" to "E")
                    )
                )
            ),
            arrayOf(
                "should update both on conflict if local timestamp is newer",
                DataSyncFavorites(0L, 0L, emptySet(), emptyMap()),
                DataSyncFavorites(0L, Long.MAX_VALUE, setOf("A"), mapOf("b" to "B")),
                DataSyncFavorites(3L, 3L, setOf("B", "C"), mapOf("a" to "A", "d" to "D")),
                Action.UpdateBoth(
                    localDiff = FavoriteDiffImpl(
                        addedRetailers = setOf("C"),
                        addedShops = mapOf("d" to "D")
                    ),
                    remoteDiff = FavoriteDiffImpl(
                        addedRetailers = setOf("A"),
                        addedShops = mapOf("b" to "B"),
                        removedRetailers = setOf("B"),
                        removedShops = mapOf("a" to "A")
                    )
                )
            ),
            arrayOf(
                "should update both on conflict if remote timestamp is newer",
                DataSyncFavorites(1L, 1L, setOf("E"), emptyMap()),
                DataSyncFavorites(1L, 2L, setOf("A"), mapOf("b" to "B")),
                DataSyncFavorites(3L, 3L, setOf("C"), mapOf("aa" to "A", "ab" to "A", "d" to "D")),
                Action.UpdateBoth(
                    localDiff = FavoriteDiffImpl(
                        addedRetailers = setOf("C"),
                        addedShops = mapOf("aa" to "A", "ab" to "A", "d" to "D"),
                        removedRetailers = setOf("A")
                    ),
                    remoteDiff = FavoriteDiffImpl(
                        addedShops = mapOf("b" to "B")
                    )
                )
            ),
            arrayOf(
                "should update local if local data is lost and remote data is not empty",
                DataSyncFavorites(1L, 1L, setOf("A"), mapOf("b" to "B")),
                DataSyncFavorites(0L, 1L, emptySet(), emptyMap()),
                DataSyncFavorites(1L, 1L, setOf("A"), mapOf("b" to "B")),
                Action.UpdateLocal(
                    localDiff = FavoriteDiffImpl(
                        addedRetailers = setOf("A"),
                        addedShops = mapOf("b" to "B")
                    ),
                    revision = 1L,
                    timestamp = 1L
                )
            ),
        )
    }

    @Test
    fun `merge should return correct action`() {
        val action = FavoritesMergeProcessor.merge(base = base, local = local, remote = remote)
        assertTrue(isEquals(action, mergeAction), message)
    }

    private fun isEquals(a: Action, b: Action): Boolean {
        return when {
            a is Action.Noop && b is Action.Noop ->
                true
            a is Action.UpdateRemote && b is Action.UpdateRemote ->
                isEquals(a.remoteDiff, b.remoteDiff)
            a is Action.UpdateBoth && b is Action.UpdateBoth ->
                isEquals(a.remoteDiff, b.remoteDiff) && isEquals(a.localDiff, b.localDiff)
            a is Action.UpdateLocal && b is Action.UpdateLocal ->
                a.revision == b.revision && a.timestamp == b.timestamp && isEquals(a.localDiff, b.localDiff)
            else -> false
        }
    }

    private fun isEquals(a: FavoriteDiff, b: FavoriteDiff): Boolean {
        if (a === b || (a.isEmpty && b.isEmpty)) {
            return true
        }
        return a.addedShops == b.addedShops && a.addedRetailers == b.addedRetailers &&
            a.removedShops == b.removedShops && a.removedRetailers == b.removedRetailers
    }
}
