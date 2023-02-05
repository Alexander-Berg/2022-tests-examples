package com.yandex.launcher.common.history.base

import androidx.collection.SimpleArrayMap
import androidx.test.core.app.ApplicationProvider
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.mockito.kotlin.*
import com.yandex.launcher.BaseRobolectricTest
import com.yandex.launcher.common.history.UserHistory
import com.yandex.launcher.common.history.UserHistoryEntry
import com.yandex.launcher.common.history.UserHistoryEntryModifier
import com.yandex.launcher.common.history.UserHistoryStrategy
import com.yandex.launcher.common.util.JsonDiskStorage
import org.hamcrest.CoreMatchers
import org.junit.Assume
import org.junit.Test
import org.robolectric.util.ReflectionHelpers

class UserHistoryGeneralTest : BaseRobolectricTest() {

    private val testKey = "test_key"
    private val leastPopularKey = "leastPopularKey"
    private val testRating = 500.0
    private val testEntry = mock<UserHistoryEntry> {
        on { rating } doReturn testRating
    }
    private val leastPopularEntry = mock<UserHistoryEntry> {
        on { key } doReturn leastPopularKey
        on { rating } doReturn 0.0
    }

    private val mockedStrategy = mock<UserHistoryStrategy> {
        on { newEntry(any()) } doReturn testEntry
        on { newEntry(leastPopularKey) } doReturn testEntry
    }
    private val mockedStorage = mock<JsonDiskStorage<UserHistory.SavedData>> { }

    private lateinit var history: com.yandex.launcher.common.history.UserHistory

    private val oneItemMap: SimpleArrayMap<String, UserHistoryEntry> =
        SimpleArrayMap()
    private val emptyMap: SimpleArrayMap<String, UserHistoryEntry> =
        SimpleArrayMap()
    private val oneItemList: ArrayList<UserHistoryEntry> = ArrayList()
    private val manyItemsList = arrayListOf(
        mock {
            on { key } doReturn "key 1"
            on { rating } doReturn 0.0
        },
        mock {
            on { key } doReturn "key 2"
            on { rating } doReturn 0.0
        },
        leastPopularEntry
    )
    private val manyItemsMap = SimpleArrayMap<String, UserHistoryEntry>()

    init {
        oneItemMap.put(testKey, testEntry)
        oneItemList.add(testEntry)
        manyItemsMap.put(manyItemsList[0].key, manyItemsList[0])
        manyItemsMap.put(manyItemsList[1].key, manyItemsList[1])
        manyItemsMap.put(manyItemsList[2].key, manyItemsList[2])
    }

    override fun setUp() {
        super.setUp()
        history = createHistoryWithMaxNumberOfObjects(10)
    }

    @Test
    fun `on clear, cleared persistent storage too`() {
        history.clear()

        verify(mockedStorage).postSave()
    }

    @Test
    fun `on flush, persistent storage flushed`() {
        history.flush()

        verify(mockedStorage).flush()
    }


    @Test
    fun `on adding new entry, entry created through the strategy`() {
        history.launch("test", mock { })

        verify(mockedStrategy).newEntry("test")
    }

    @Test
    fun `on adding new entry, entry added to sorted list`() {
        val sorted = ReflectionHelpers.getField<java.util.ArrayList<UserHistoryEntry>>(history, FIELD_SORTED)
        Assume.assumeThat(sorted.isEmpty(), CoreMatchers.equalTo(true))

        history.launch(testKey, mock { })

        assertThat(sorted[0], equalTo(testEntry))
    }

    @Test
    fun `on adding new entry, entry added to map`() {
        val map =
            ReflectionHelpers.getField<SimpleArrayMap<String, UserHistoryEntry>>(history, FIELD_MAP)
        Assume.assumeThat(map.isEmpty, CoreMatchers.equalTo(true))

        history.launch(testKey, mock { })

        assertThat(map.get(testKey), equalTo(testEntry))
    }

    @Test
    fun `on adding new entry, modification applied`() {
        val modifier: UserHistoryEntryModifier = mock { }

        history.launch(testKey, modifier)

        verify(mockedStrategy).applyModification(testEntry, modifier)
    }

    @Test
    fun `on adding new entry, amount of entries exceed the limit, the last object in sorted list removed`() {
        val history = createHistoryWithMaxNumberOfObjects(3)
        ReflectionHelpers.setField(history, FIELD_SORTED, manyItemsList)
        ReflectionHelpers.setField(history, FIELD_MAP, manyItemsMap)
        Assume.assumeThat(manyItemsList.contains(leastPopularEntry), CoreMatchers.equalTo(true))

        history.launch(testKey, mock { })

        assertThat(manyItemsList.contains(leastPopularEntry), equalTo(false))
    }

    @Test
    fun `on adding new entry, amount of entries exceed the limit, the last object of sorted list removed from map`() {
        val history = createHistoryWithMaxNumberOfObjects(3)
        ReflectionHelpers.setField(history, FIELD_SORTED, manyItemsList)
        ReflectionHelpers.setField(history, FIELD_MAP, manyItemsMap)
        Assume.assumeThat(manyItemsMap.containsKey(leastPopularKey), CoreMatchers.equalTo(true))

        history.launch(testKey, mock { })

        assertThat(manyItemsMap.containsKey(leastPopularKey), equalTo(false))
    }

    private fun createHistoryWithMaxNumberOfObjects(maxNumberOfObjects: Int) = TestUserHistoryImplementation(
        context = ApplicationProvider.getApplicationContext(),
        maxNumberOfObjects = maxNumberOfObjects,
        maxDays = 30,
        strategy = mockedStrategy,
        storage = mockedStorage
    )

}
