package com.yandex.launcher.common.history.base

import android.os.Build
import androidx.collection.SimpleArrayMap
import androidx.test.core.app.ApplicationProvider
import com.yandex.launcher.app.TestApplication
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

const val FIELD_MAP = "map"
const val FIELD_SORTED = "sorted"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1], application = TestApplication::class)
class HistoryTest {

    private val testKey = "test_key"
    private val leastPopularKey = "leastPopularKey"
    private val testRating = 500.0
    private val testEntry = mock<TestHistoryEntry> {
        on { rating } doReturn testRating
    }
    private val leastPopularEntry = TestHistoryEntry(0.0, leastPopularKey)

    private val oneItemMap: SimpleArrayMap<String, TestHistoryEntry> = SimpleArrayMap()
    private val emptyMap: SimpleArrayMap<String, TestHistoryEntry> = SimpleArrayMap()
    private val oneItemList: ArrayList<TestHistoryEntry> = ArrayList()

    private val history = createHistoryWithMaxNumberOfObjects(3)

    private val manyItemsList = arrayListOf(
        TestHistoryEntry(0.0, "key 1"),
        TestHistoryEntry(0.0, "key 2"),
        leastPopularEntry
    )

    private val manyItemsMap = SimpleArrayMap<String, TestHistoryEntry>()

    init {
        oneItemMap.put(testKey, testEntry)
        oneItemList.add(testEntry)
        manyItemsMap.put(manyItemsList[0].key, manyItemsList[0])
        manyItemsMap.put(manyItemsList[1].key, manyItemsList[1])
        manyItemsMap.put(manyItemsList[2].key, manyItemsList[2])
    }

    @Test
    fun `history contains item if map contains the item`() {
        ReflectionHelpers.setField(history, FIELD_MAP, oneItemMap)

        assertThat(history.contains(testKey), equalTo(true))
    }

    @Test
    fun `history does not contains item if map does not contains the item`() {
        ReflectionHelpers.setField(history, FIELD_MAP, emptyMap)

        assertThat(history.contains(testKey), equalTo(false))
    }

    @Test
    fun `history returns entry if map contains entry`() {
        ReflectionHelpers.setField(history, FIELD_MAP, oneItemMap)

        assertThat(history[testKey], equalTo(testEntry))
    }

    @Test
    fun `history returns null if map does not contains entry`() {
        ReflectionHelpers.setField(history, FIELD_MAP, emptyMap)

        assertThat(history[testKey], nullValue())
    }

    @Test
    fun `history returns rating for entry if map has entry with such key`() {
        ReflectionHelpers.setField(history, FIELD_MAP, oneItemMap)

        assertThat(history.getRating(testKey), equalTo(testRating))
    }

    @Test
    fun `history returns zero rating if map has no such entry`() {
        ReflectionHelpers.setField(history, FIELD_MAP, emptyMap)

        assertThat(history.getRating(testKey), equalTo(0.0))
    }

    @Test
    fun `map cleared when history clear method called`() {
        ReflectionHelpers.setField(history, FIELD_MAP, oneItemMap)
        Assume.assumeThat(oneItemMap.isEmpty, not(true))

        history.clear()

        assertThat(oneItemMap.isEmpty, equalTo(true))
    }

    @Test
    fun `list cleared when history clear method called`() {
        ReflectionHelpers.setField(history, FIELD_SORTED, oneItemList)
        Assume.assumeThat(oneItemList.isEmpty(), not(true))

        history.clear()

        assertThat(oneItemList.isEmpty(), equalTo(true))
    }

    @Test
    fun `on adding new entry with null key, no entry added to map`() {
        val map = ReflectionHelpers.getField<SimpleArrayMap<String, TestHistoryEntry>>(history, FIELD_MAP)
        Assume.assumeThat(map.isEmpty, equalTo(true))

        history.launch(null, mock { })

        assertThat(map.isEmpty, equalTo(true))
    }

    @Test
    fun `on adding new entry with null key, no entry added to sorted list`() {
        val sorted = ReflectionHelpers.getField<java.util.ArrayList<TestHistoryEntry>>(history, FIELD_SORTED)
        Assume.assumeThat(sorted.isEmpty(), equalTo(true))

        history.launch(null, mock { })

        assertThat(sorted.isEmpty(), equalTo(true))
    }

    private fun createHistoryWithMaxNumberOfObjects(maxNumberOfObjects: Int): TestHistoryImplementation =
        TestHistoryImplementation(
            context = ApplicationProvider.getApplicationContext(),
            maxNumberOfObjects = maxNumberOfObjects
        )
}
