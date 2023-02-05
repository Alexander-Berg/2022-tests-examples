package com.yandex.launcher.common.history.base

import android.os.Build
import androidx.collection.SimpleArrayMap
import androidx.test.core.app.ApplicationProvider
import com.yandex.launcher.common.history.UserHistoryEntry
import com.yandex.launcher.common.history.UserHistoryEntryModifier
import com.yandex.launcher.app.TestApplication
import com.yandex.launcher.common.history.base.FIELD_MAP
import com.yandex.launcher.common.history.base.FIELD_SORTED
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import java.text.SimpleDateFormat

private const val HISTORY_ID = "some-id"

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1], application = TestApplication::class)
class UserHistoryCompatibilityTest1 {

    private val testKey = "some key"

    private lateinit var oldImplementation: UserHistory
    private lateinit var newImplementation: com.yandex.launcher.common.history.UserHistory

    @Before
    fun setUp() {
        oldImplementation = UserHistory(ApplicationProvider.getApplicationContext(), HISTORY_ID, 14, 10)
        newImplementation = com.yandex.launcher.common.history.UserHistory(ApplicationProvider.getApplicationContext(), HISTORY_ID, 10, 14)
    }

    @Test
    fun `test 1`() {
        oldImplementation.add(testKey, testKey)
        newImplementation.launch(testKey, UserHistoryEntryModifier(testKey, testKey, true))

        Assert.assertEquals(oldImplementation.contains(testKey), newImplementation.contains(testKey))
    }

    @Test
    fun `test 2`() {
        val time = System.currentTimeMillis()
        oldImplementation.add(testKey, time, 1f)
        newImplementation.launch(testKey, UserHistoryEntryModifier(testKey, time, 1f))

        Assert.assertEquals(oldImplementation.contains(testKey), newImplementation.contains(testKey))
    }

    @Test
    fun `test 3`() {
        val time = System.currentTimeMillis()
        oldImplementation.add(testKey, time, 1f)
        newImplementation.launch(testKey, UserHistoryEntryModifier(testKey, time, 1f))

        Assert.assertTrue(assertRatingEquals(oldImplementation.getRating(testKey), newImplementation.getRating(testKey)))
    }

    @Test
    fun `test 4`() {
        oldImplementation.add(testKey, testKey)
        newImplementation.launch(testKey, UserHistoryEntryModifier(testKey, testKey, true))

        Assert.assertTrue(assertRatingEquals(oldImplementation.getRating(testKey), newImplementation.getRating(testKey)))
    }

    @Test
    fun `test 5`() {
        val keyArray = arrayOf(
            "some key 1", "some key 1", "some key 1", "some key 1", "some key 1", "some key 1",
            "some key 2", "some key 2", "some key 3", "some key 4", "some key 5", "some key 6",
            "some key 7", "some key 8", "some key 9", "some key 10", "some key 1"
        )

        keyArray.forEach {
            oldImplementation.add(it, it)
            newImplementation.launch(it, UserHistoryEntryModifier(it, it, true))
        }

        val oldImplementationMap: SimpleArrayMap<String, UserHistory.HistoryEntry> = ReflectionHelpers.getField<SimpleArrayMap<String, UserHistory.HistoryEntry>>(oldImplementation, FIELD_MAP)
        val oldImplementationSorted: ArrayList<UserHistory.HistoryEntry> = ReflectionHelpers.getField<ArrayList<UserHistory.HistoryEntry>>(oldImplementation, FIELD_SORTED)

        val newImplementationMap: SimpleArrayMap<String, UserHistoryEntry> = ReflectionHelpers.getField<SimpleArrayMap<String, UserHistoryEntry>>(newImplementation, FIELD_MAP)
        val newImplementationSorted: ArrayList<UserHistoryEntry> = ReflectionHelpers.getField<ArrayList<UserHistoryEntry>>(newImplementation, FIELD_SORTED)

        Assert.assertEquals(oldImplementationMap.size(), newImplementationMap.size())
        Assert.assertEquals(oldImplementationSorted.size, newImplementationSorted.size)

        for (i in 0 until oldImplementationSorted.size) {
            val oldHistoryEntry = oldImplementationSorted[i]
            val newHistoryEntry = newImplementationSorted[i]
            Assert.assertEquals(oldHistoryEntry.key, newHistoryEntry.key)
            Assert.assertTrue(assertRatingEquals(oldHistoryEntry.rating, newHistoryEntry.rating))
        }
    }

    @Test
    fun `test 6`() {
        val keyArray = arrayOf(
            "some key 1", "some key 1", "some key 1", "some key 1", "some key 1", "some key 1",
            "some key 2", "some key 2", "some key 3", "some key 4", "some key 5", "some key 6",
            "some key 7", "some key 8", "some key 9", "some key 10", "some key 1"
        )

        keyArray.forEach {
            oldImplementation.add(it, System.currentTimeMillis(), 1f)
            newImplementation.launch(it, UserHistoryEntryModifier(it, System.currentTimeMillis(), 1f))
        }

        val oldImplementationMap: SimpleArrayMap<String, UserHistory.HistoryEntry> = ReflectionHelpers.getField<SimpleArrayMap<String, UserHistory.HistoryEntry>>(oldImplementation, FIELD_MAP)
        val oldImplementationSorted: ArrayList<UserHistory.HistoryEntry> = ReflectionHelpers.getField<ArrayList<UserHistory.HistoryEntry>>(oldImplementation, FIELD_SORTED)

        val newImplementationMap: SimpleArrayMap<String, UserHistoryEntry> = ReflectionHelpers.getField<SimpleArrayMap<String, UserHistoryEntry>>(newImplementation, FIELD_MAP)
        val newImplementationSorted: ArrayList<UserHistoryEntry> = ReflectionHelpers.getField<ArrayList<UserHistoryEntry>>(newImplementation, FIELD_SORTED)

        Assert.assertEquals(oldImplementationMap.size(), newImplementationMap.size())
        Assert.assertEquals(oldImplementationSorted.size, newImplementationSorted.size)

        for (i in 0 until oldImplementationSorted.size) {
            val oldHistoryEntry = oldImplementationSorted[i]
            val newHistoryEntry = newImplementationSorted[i]
            Assert.assertEquals(oldHistoryEntry.key, newHistoryEntry.key)
            Assert.assertTrue(assertRatingEquals(oldHistoryEntry.rating, newHistoryEntry.rating))
        }
    }

    @Test
    fun `test 7`() {
        val keyArray = arrayOf(
            "some key 1", "some key 1", "some key 1", "some key 1", "some key 1", "some key 1",
            "some key 2", "some key 2", "some key 3", "some key 4", "some key 5", "some key 6",
            "some key 7", "some key 8", "some key 9", "some key 10", "some key 1"
        )

        val timeArray = arrayOf(
            getTime("28-10-2018"),
            getTime("28-10-2018"),
            getTime("29-10-2018"),
            getTime("29-10-2018"),
            getTime("29-10-2018"),
            getTime("30-10-2018"),

            getTime("30-10-2018"),
            getTime("30-10-2018"),
            getTime("30-10-2018"),
            getTime("30-10-2018"),
            getTime("30-10-2018"),
            getTime("30-10-2018"),

            getTime("01-11-2018"),
            getTime("01-11-2018"),
            getTime("02-11-2018"),
            getTime("02-11-2018"),
            getTime("02-11-2018")
        )

        for (i in 0 until keyArray.size) {
            oldImplementation.add(keyArray[i], timeArray[i], 1f)
            newImplementation.launch(keyArray[i], UserHistoryEntryModifier(keyArray[i], timeArray[i], 1f))
        }

        val oldImplementationMap: SimpleArrayMap<String, UserHistory.HistoryEntry> = ReflectionHelpers.getField<SimpleArrayMap<String, UserHistory.HistoryEntry>>(oldImplementation, FIELD_MAP)
        val oldImplementationSorted: ArrayList<UserHistory.HistoryEntry> = ReflectionHelpers.getField<ArrayList<UserHistory.HistoryEntry>>(oldImplementation, FIELD_SORTED)

        val newImplementationMap: SimpleArrayMap<String, UserHistoryEntry> = ReflectionHelpers.getField<SimpleArrayMap<String, UserHistoryEntry>>(newImplementation, FIELD_MAP)
        val newImplementationSorted: ArrayList<UserHistoryEntry> = ReflectionHelpers.getField<ArrayList<UserHistoryEntry>>(newImplementation, FIELD_SORTED)

        Assert.assertEquals(oldImplementationMap.size(), newImplementationMap.size())
        Assert.assertEquals(oldImplementationSorted.size, newImplementationSorted.size)

        for (i in 0 until oldImplementationSorted.size) {
            val oldHistoryEntry = oldImplementationSorted[i]
            val newHistoryEntry = newImplementationSorted[i]
            Assert.assertEquals(oldHistoryEntry.key, newHistoryEntry.key)
            Assert.assertTrue(assertRatingEquals(oldHistoryEntry.rating, newHistoryEntry.rating))
        }
    }

    private fun assertRatingEquals(rating1: Float, rating2: Double): Boolean {
        return Math.abs(rating1 - rating2) < 0.0000001
    }

    private fun getTime(date: String): Long {
        val sdf = SimpleDateFormat("dd-MM-YYYY")
        return sdf.parse(date).time
    }
}