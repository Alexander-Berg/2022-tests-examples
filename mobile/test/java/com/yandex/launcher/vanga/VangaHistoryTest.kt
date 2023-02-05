package com.yandex.launcher.vanga

import android.app.Application
import android.os.Build
import androidx.collection.SimpleArrayMap
import com.yandex.vanga.NO_LIMIT
import com.yandex.vanga.RatingManager
import com.yandex.vanga.VangaRatingEntry
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsNull.nullValue
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import java.util.ArrayList

private const val RANDOME_KEY = "5"
private const val RATING_FOR_RANDOM_KEY = 5.0
private const val NON_EXISTING_KEY = "non existing key"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1], application = Application::class)
class VangaHistoryTest {

    private val testEntryList = Array(10) {
        VangaRatingEntry(it.toString(), it.toDouble())
    }.toList()
    private val ratingManager = mock<RatingManager> {
        on { updateVisitsAndRating(any(), any(), any()) } doReturn ArrayList<VangaRatingEntry>()
    }
    private lateinit var history: VangaHistoryForTest

    @Before
    fun setUp() {
        AuxThreadInternal.restart()
        history = VangaHistoryForTest(RuntimeEnvironment.application, ratingManager, StubVangaHistoryDelegate())
    }

    @Test
    fun `load history, the result of RatingManager is saved`() {
        whenever(ratingManager.getSortedEntries(any(), any())).thenReturn(testEntryList)
        assumeHistoryIsEmpty()

        history.load()

        assertHistoryHasCorrectEntities()
    }

    @Test
    fun `getTop returns top N elements in history`() {
        whenever(ratingManager.getSortedEntries(any(), any())).thenReturn(testEntryList)
        history.load()

        val top3 = history.getTop(3)

        assertRallytop3(top3)
    }

    @Test
    fun `getTop for not initialized history returns empty list`() {
        val top3 = history.getTop(3)

        assertThat(top3.size, equalTo(0))
    }

    @Test
    fun `getTop for less history, returns all the history`() {
        whenever(ratingManager.getSortedEntries(any(), any())).thenReturn(testEntryList)
        history.load()

        val top3 = history.getTop(3)

        assertRallytop3(top3)
    }

    @Test
    fun `contains for existing key returns true`() {
        whenever(ratingManager.getSortedEntries(any(), any())).thenReturn(testEntryList)
        history.load()

        assertThat(history.contains(RANDOME_KEY), equalTo(true))
    }

    @Test
    fun `contains for non-existing key returns false`() {
        whenever(ratingManager.getSortedEntries(any(), any())).thenReturn(testEntryList)
        history.load()

        assertThat(history.contains(NON_EXISTING_KEY), equalTo(false))
    }

    @Test
    fun `contains for not initialized history returns false`() {
        assertThat(history.contains(RANDOME_KEY), equalTo(false))
    }

    @Test
    fun `after removing existing key, the key returned`() {
        whenever(ratingManager.getSortedEntries(any(), any())).thenReturn(testEntryList)
        history.load()
        Assume.assumeThat(history.contains(RANDOME_KEY), equalTo(true))

        val result = history.remove(RANDOME_KEY)

        assertThat(result, equalTo(RANDOME_KEY))
    }

    @Test
    fun `after removing existing key, key is not in the history`() {
        whenever(ratingManager.getSortedEntries(any(), any())).thenReturn(testEntryList)
        history.load()
        Assume.assumeThat(history.contains(RANDOME_KEY), equalTo(true))

        history.remove(RANDOME_KEY)

        assertThat(history.contains(RANDOME_KEY), equalTo(false))
    }

    @Test
    fun `get for existing key returns corresponding entry`() {
        whenever(ratingManager.getSortedEntries(any(), any())).thenReturn(testEntryList)
        history.load()
        Assume.assumeThat(history.contains(RANDOME_KEY), equalTo(true))

        val result = history[RANDOME_KEY]

        assertThat(result!!.key, equalTo(RANDOME_KEY))
    }

    @Test
    fun `get for non-existing key returns null`() {
        whenever(ratingManager.getSortedEntries(any(), any())).thenReturn(testEntryList)
        history.load()
        Assume.assumeThat(history.contains(NON_EXISTING_KEY), equalTo(false))

        val result = history[NON_EXISTING_KEY]

        assertThat(result, nullValue())
    }

    @Test
    fun `getRating for existing key, returns it's rating`() {
        whenever(ratingManager.getSortedEntries(any(), any())).thenReturn(testEntryList)
        history.load()
        Assume.assumeThat(history.contains(RANDOME_KEY), equalTo(true))

        val result = history.getRating(RANDOME_KEY)

        assertThat(result, equalTo(RATING_FOR_RANDOM_KEY))
    }

    @Test
    fun `getRating for non-existing key, returns zero`() {
        whenever(ratingManager.getSortedEntries(any(), any())).thenReturn(testEntryList)
        history.load()
        Assume.assumeThat(history.contains(NON_EXISTING_KEY), equalTo(false))

        val result = history.getRating(NON_EXISTING_KEY)

        assertThat(result, equalTo(0.0))
    }

    @Test
    fun `launch called updateVisitsAndRating with passed key`() {
        whenever(ratingManager.updateVisitsAndRating(any(), any(), any())).thenReturn(ArrayList())

        history.launch(RANDOME_KEY)

        verify(ratingManager).updateVisitsAndRating(any(), eq(RANDOME_KEY), eq(NO_LIMIT))
    }

    private fun assumeHistoryIsEmpty() {
        Assume.assumeThat(ReflectionHelpers.getField<ArrayList<VangaRatingEntry>>(history, "sorted").size, equalTo(0))
        Assume.assumeThat(ReflectionHelpers.getField<SimpleArrayMap<String, VangaRatingEntry>>(history, "map").size(), equalTo(0))
    }

    private fun assertHistoryHasCorrectEntities() {
        val sorted = ReflectionHelpers.getField<ArrayList<LauncherVangaRatingEntry>>(history, "sorted")
        val map = ReflectionHelpers.getField<SimpleArrayMap<String, LauncherVangaRatingEntry>>(history, "map")

        assertThat(sorted.size, equalTo(testEntryList.size))
        assertThat(map.size(), equalTo(testEntryList.size))
        for (i in 0 until testEntryList.size) {
            val expected = testEntryList[i]
            assertThat(sorted[i].entry, equalTo(expected))
            assertThat(map[expected.key]!!.entry, equalTo(expected))
        }
    }

    private fun assertRallytop3(actualTop3: List<String>) {
        assertThat(actualTop3.size, equalTo(3))
        for (i in 0..2) {
            assertThat(actualTop3[i], equalTo(testEntryList[i].key))
        }
    }
}
