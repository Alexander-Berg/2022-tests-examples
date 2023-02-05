package com.yandex.launcher.model

import android.content.Context
import androidx.collection.ArrayMap
import com.android.launcher3.LauncherSettings
import com.yandex.launcher.BaseRobolectricTest
import com.yandex.launcher.model.LauncherGlobalPreferencesProvider.TABLE_GLOBAL_PREFERENCES
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsNull.nullValue
import org.junit.Assert
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric


class LauncherSettingsTests: BaseRobolectricTest() {

    private val controller = Robolectric.buildContentProvider(LauncherGlobalPreferencesProvider::class.java)
    private lateinit var context: Context

    @Before
    override fun setUp() {
        super.setUp()
        context = appContext
    }

    @Test
    fun `setRawValue should add key-value pair to db`() {
        Assume.assumeThat(getEntityCount(), `is`(0))

        LauncherSettings.GlobalPreferences.setRawValue(context, "test_key", "test_value")

        val prefMap = getPreferencesAsMap()
        assertThat(prefMap.size, `is`(1))
        assertThat(prefMap.keyAt(0), `is`("test_key"))
        assertThat(prefMap.valueAt(0), `is`("test_value"))
    }

    @Test
    fun `setRawValue 2 times with same key should override value`() {
        Assume.assumeThat(getEntityCount(), `is`(0))

        LauncherSettings.GlobalPreferences.setRawValue(context, "test_key", "test_value")
        LauncherSettings.GlobalPreferences.setRawValue(context, "test_key", "new_test_value")

        val prefMap = getPreferencesAsMap()
        assertThat(prefMap.size, `is`(1))
        assertThat(prefMap.keyAt(0), `is`("test_key"))
        assertThat(prefMap.valueAt(0), `is`("new_test_value"))
    }

    @Test
    fun `setRawValue 2 times should add 2 key-value pairs to db`() {
        Assume.assumeThat(getEntityCount(), `is`(0))

        LauncherSettings.GlobalPreferences.setRawValue(context, "test_key_1", "test_value_1")
        LauncherSettings.GlobalPreferences.setRawValue(context, "test_key_2", "test_value_2")

        val prefMap = getPreferencesAsMap()
        assertThat(prefMap.size, `is`(2))
        assertThat(prefMap.keyAt(0), `is`("test_key_1"))
        assertThat(prefMap.valueAt(0), `is`("test_value_1"))
        assertThat(prefMap.keyAt(1), `is`("test_key_2"))
        assertThat(prefMap.valueAt(1), `is`("test_value_2"))
    }

    @Test
    fun `setRawValue with existed key should override only entry with target key`() {
        Assume.assumeThat(getEntityCount(), `is`(0))

        LauncherSettings.GlobalPreferences.setRawValue(context, "test_key_1", "test_value_1")
        LauncherSettings.GlobalPreferences.setRawValue(context, "test_key_2", "test_value_2")
        LauncherSettings.GlobalPreferences.setRawValue(context, "test_key_1", "new_test_value_1")

        val prefMap = getPreferencesAsMap()
        assertThat(prefMap.size, `is`(2))
        assertThat(prefMap.keyAt(0), `is`("test_key_1"))
        assertThat(prefMap.valueAt(0), `is`("new_test_value_1"))
        assertThat(prefMap.keyAt(1), `is`("test_key_2"))
        assertThat(prefMap.valueAt(1), `is`("test_value_2"))
    }

    @Test
    fun `getRawValue for non-existing key returns null`() {
        Assume.assumeThat(getEntityCount(), `is`(0))

        assertThat(LauncherSettings.GlobalPreferences.getRawValue(context, "non-existing key"), nullValue())
    }

    @Test
    fun `getRawValue for existing key returns it's value`() {
        Assume.assumeThat(getEntityCount(), `is`(0))

        LauncherSettings.GlobalPreferences.setRawValue(context, "test_key", "test_value")

        assertThat(LauncherSettings.GlobalPreferences.getRawValue(context, "test_key"), `is`("test_value"))
    }

    @Test
    fun `getRawValue for exising key returns it's value (2)`() {
        Assume.assumeThat(getEntityCount(), `is`(0))

        LauncherSettings.GlobalPreferences.setRawValue(context, "test_key_1", "test_value_1")
        LauncherSettings.GlobalPreferences.setRawValue(context, "test_key_2", "test_value_2")

        assertThat(LauncherSettings.GlobalPreferences.getRawValue(context, "test_key_1"), `is`("test_value_1"))
    }

    @Test
    fun `getRawValue for overridden value returns it's new value`() {
        Assume.assumeThat(getEntityCount(), `is`(0))

        LauncherSettings.GlobalPreferences.setRawValue(context, "test_key_1", "test_value_1")
        LauncherSettings.GlobalPreferences.setRawValue(context, "test_key_2", "test_value_2")
        LauncherSettings.GlobalPreferences.setRawValue(context, "test_key_1", "new_test_value_1")

        assertThat(LauncherSettings.GlobalPreferences.getRawValue(context, "test_key_1"), `is`("new_test_value_1"))
    }

    private fun getEntityCount(): Int {
        val dbHelper = AutoClosableDatabaseHelper(appContext)
        dbHelper.use {helper ->
            val cursor = helper.readableDatabase.query(TABLE_GLOBAL_PREFERENCES, null, null, null, null, null, null)
            cursor.use {
                return it.count
            }
        }
    }

    private fun getPreferencesAsMap(): ArrayMap<String, String> {
        val dbHelper = AutoClosableDatabaseHelper(appContext)
        dbHelper.use {helper ->
            val cursor = helper.readableDatabase.query(TABLE_GLOBAL_PREFERENCES, null, null, null, null, null, null)
            cursor.use {
                if (!it.moveToFirst()) {
                    return ArrayMap()
                }
                val result = ArrayMap<String, String>()
                do {
                    result[it.getString(it.getColumnIndex(LauncherSettings.GlobalPreferences.NAME))] = it.getString(it.getColumnIndex(LauncherSettings.GlobalPreferences.VALUE))
                } while (it.moveToNext())
                return result
            }
        }
    }
}
