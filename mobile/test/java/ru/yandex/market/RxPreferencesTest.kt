package ru.yandex.market

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.annimon.stream.Optional
import com.annimon.stream.OptionalInt
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.common.preferences.CommonPreferences
import ru.yandex.market.internal.rx.preferences.RxSharedPreferences

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class RxPreferencesTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val sharedPreferences = context.getSharedPreferences(
        PREFERENCE_NAME, Context.MODE_PRIVATE
    )
    private val rxPreferences = RxSharedPreferences(CommonPreferences(sharedPreferences))

    @Test
    fun `Returns string value on subscription`() {
        val value = "value"
        sharedPreferences.edit().putString(KEY, value).commit()
        rxPreferences.getString(KEY)
            .asObservable()
            .test()
            .assertValue(Optional.of(value))
    }

    @Test
    fun `Returns empty optional when there is no actual value`() {
        rxPreferences.getString(KEY)
            .asObservable()
            .test()
            .assertValue(Optional.empty())
    }

    @Test
    fun `Distinct string values`() {
        val preference = rxPreferences.getString(KEY)
        val testObserver = preference.asObservable().test()
        sharedPreferences.edit().putString(KEY, "").commit()
        sharedPreferences.edit().putString(KEY, "").commit()
        testObserver.assertValues(Optional.empty(), Optional.of(""))
    }

    @Test
    fun `Converts null string to empty optional`() {
        val testObserver = rxPreferences.getString(KEY)
            .asObservable()
            .test()
        sharedPreferences.edit().putString(KEY, "A").commit()
        sharedPreferences.edit().putString(KEY, null).commit()
        testObserver.assertValues(Optional.empty(), Optional.of("A"), Optional.empty())
    }

    @Test
    fun `Distinct values until changed`() {
        val preference = rxPreferences.getString(KEY)
        val testObserver = preference.asObservable().test()
        sharedPreferences.edit().putString(KEY, "A").commit()
        sharedPreferences.edit().putString(KEY, "A").commit()
        testObserver.assertValues(Optional.empty(), Optional.of("A"))
    }

    @Test
    fun `Returns empty optional for absent int value`() {
        val preference = rxPreferences.getInteger(KEY)
        preference.asObservable()
            .test()
            .assertValue(OptionalInt.empty())
    }

    companion object {

        private const val KEY = "ExampleKey"
        private const val PREFERENCE_NAME = "testPreferences"
    }
}
