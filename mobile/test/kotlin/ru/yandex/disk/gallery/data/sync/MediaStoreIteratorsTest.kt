package ru.yandex.disk.gallery.data.sync

import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.hamcrest.Matchers.equalTo
import org.junit.Test
import ru.yandex.disk.gallery.data.GalleryDataTestCase.Companion.consMediaItem
import ru.yandex.disk.gallery.data.database.*
import ru.yandex.disk.test.TestCase2

class MediaStoreIteratorsTest : TestCase2() {

    @Test
    fun `ascending iterator`() {
        val iterate: (List<Int>) -> List<Int> = {
            AscendingIterator(it.iterator(), Comparator(Int::compareTo)).asSequence().toList()
        }

        assertThat(iterate(listOf()), equalTo(listOf()))
        assertThat(iterate(listOf(1)), equalTo(listOf(1)))
        assertThat(iterate(listOf(1, 2)), equalTo(listOf(1, 2)))
        assertThat(iterate(listOf(1, 3, 2)), equalTo(listOf(1, 3)))
        assertThat(iterate(listOf(1, 1, 2, 1)), equalTo(listOf(1, 2)))
        assertThat(iterate(listOf(1, 2, 1, 2, 3)), equalTo(listOf(1, 2, 3)))
    }

    @Test
    fun `items iterator`() {
        val iterate: (List<Int>, Int) -> List<Int> = { times, batch ->
            val dao = mockDaoItemsIteration(times.map { MediaStoreKey(it.toLong(), 0) })

            val iterator = DatabaseItemsIterator(dao, batch,
                    MediaStoreKey((times.minOrNull() ?: 0).toLong(), 0),
                    MediaStoreKey((times.maxOrNull() ?: 0).toLong(), 0))

            iterator.asSequence().map { it -> it.eTime.toInt() }.toList().asReversed()
        }

        assertThat(iterate(listOf(), 2), equalTo(listOf()))
        assertThat(iterate(listOf(1), 2), equalTo(listOf(1)))
        assertThat(iterate(listOf(1, 2), 2), equalTo(listOf(1, 2)))
        assertThat(iterate(listOf(1, 2, 3), 2), equalTo(listOf(1, 2, 3)))
        assertThat(iterate(listOf(1, 2, 3, 4), 1), equalTo(listOf(1, 2, 3, 4)))
    }

    private fun mockDaoItemsIteration(keys: List<MediaStoreKey>): GalleryDao {
        val comparator = compareBy<MediaStoreKey> { it.eTime }.thenBy { it.mediaStoreId }

        return mock { _ ->
            on { queryItemsBetweenKeys(any(), any(), any()) } doAnswer { invocation ->
                val start: MediaStoreKey = invocation.getArgument(0)
                val end: MediaStoreKey = invocation.getArgument(1)
                val limit: Int = invocation.getArgument(2)

                keys.sortedWith(comparator.reversed())
                        .dropWhile { comparator.compare(end, it) < 0 }
                        .takeWhile { comparator.compare(start, it) <= 0 }
                        .take(limit)
                        .map { consMediaItem(mediaStoreId = it.mediaStoreId, eTime = it.eTime) }
            }
        }
    }
}
