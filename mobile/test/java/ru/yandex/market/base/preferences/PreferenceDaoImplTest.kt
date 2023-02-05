package ru.yandex.market.base.preferences

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.base.preferences.adapter.GsonPreferenceAdapter
import ru.yandex.market.base.preferences.adapter.StringPreferenceAdapter
import ru.yandex.market.base.preferences.dao.PreferenceDaoImpl
import ru.yandex.market.optional.Optional

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class PreferenceDaoImplTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    @Before
    fun clearPrefs() {
        sharedPreferences.edit().clear().commit()
    }

    @Test
    fun `save and get string pref`() {
        val prefDao = PreferenceDaoImpl(
            sharedPreferences = sharedPreferences,
            preferenceKey = PREF_KEY,
            preferenceAdapter = StringPreferenceAdapter()
        )

        assertThat(prefDao.get()).isNull()
        prefDao.set(PREF_VALUE_STRING)
        assertThat(prefDao.get()).isEqualTo(PREF_VALUE_STRING)
    }

    @Test
    fun `save and remove string pref`() {
        val prefDao = PreferenceDaoImpl(
            sharedPreferences = sharedPreferences,
            preferenceKey = PREF_KEY,
            preferenceAdapter = StringPreferenceAdapter()
        )

        prefDao.set(PREF_VALUE_STRING)
        assertThat(prefDao.get()).isEqualTo(PREF_VALUE_STRING)
        prefDao.delete()
        assertThat(prefDao.get()).isNull()
    }

    @Test
    fun `observe pref values stream`() {
        val prefDao = PreferenceDaoImpl(
            sharedPreferences = sharedPreferences,
            preferenceKey = PREF_KEY,
            preferenceAdapter = StringPreferenceAdapter()
        )

        val testObserver = prefDao.observeValues().test()

        PREF_VALUES_STRING.forEach(prefDao::set)

        testObserver
            .assertNoErrors()
            .assertNotComplete()
            .assertValueCount(PREF_VALUES_STRING.size + 1)
            .assertValueAt(0, Optional.empty())
            .apply {
                PREF_VALUES_STRING.forEachIndexed { index, expected ->
                    assertValueAt(index + 1, Optional.of(expected))
                }
            }
    }

    @Test
    fun `observe pref changes stream`() {
        val prefDao = PreferenceDaoImpl(
            sharedPreferences = sharedPreferences,
            preferenceKey = PREF_KEY,
            preferenceAdapter = StringPreferenceAdapter()
        )

        val testObserver = prefDao.observeChanges().test()

        PREF_VALUES_STRING.forEach(prefDao::set)

        testObserver
            .assertNoErrors()
            .assertNotComplete()
            .assertValueCount(PREF_VALUES_STRING.size)
            .apply {
                PREF_VALUES_STRING.forEachIndexed { index, expected ->
                    assertValueAt(index, Optional.of(expected))
                }
            }
    }

    @Test
    fun `save and get json pref`() {
        val prefDao = PreferenceDaoImpl(
            sharedPreferences = sharedPreferences,
            preferenceKey = PREF_KEY,
            preferenceAdapter = GsonPreferenceAdapter(gson, TypeToken.get(CompoundPreferenceData::class.java).type)
        )

        prefDao.set(PREF_VALUE_COMPOUND)
        assertThat(prefDao.get()).isEqualTo(PREF_VALUE_COMPOUND)
    }

    companion object {
        private const val PREFS_NAME = "testPrefs"
        private const val PREF_KEY = "key"
        private const val PREF_VALUE_STRING = "value"
        private const val PREF_VALUE_INT = 42

        private val PREF_VALUE_COMPOUND = CompoundPreferenceData(
            stringValue = PREF_VALUE_STRING,
            intValue = PREF_VALUE_INT,
            booleanValue = true
        )

        private val PREF_VALUES_STRING = listOf("value1", "value2", "value3")
    }
}
