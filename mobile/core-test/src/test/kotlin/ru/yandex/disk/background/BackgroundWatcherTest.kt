package ru.yandex.disk.background

import android.content.SharedPreferences
import android.preference.PreferenceManager
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.robolectric.annotation.Config
import ru.yandex.disk.background.BackgroundWatcherContract.AUTOUPLOAD_ENABLED_TIME_KEY
import ru.yandex.disk.background.BackgroundWatcherContract.LAST_REPORTED_AUTOUPLOAD_TIME_KEY
import ru.yandex.disk.stats.AnalyticEventKeys
import ru.yandex.disk.test.AndroidTestCase2
import ru.yandex.disk.test.Assert2
import ru.yandex.disk.utils.FixedSystemClock
import ru.yandex.disk.util.error.ErrorKeys
import ru.yandex.disk.util.error.ErrorReporter
import java.util.concurrent.TimeUnit

@Config(manifest = Config.NONE)
class BackgroundWatcherTest : AndroidTestCase2() {

    private val MOCK_DAY_BEFORE_NOW = TimeUnit.DAYS.toMillis(1)
    private val MOCK_CURRENT_TIME = TimeUnit.DAYS.toMillis(3)
    private val MOCK_AUTOUPLOADED_FILE_CREATION_DATE = TimeUnit.DAYS.toMillis(2)

    private lateinit var clock: FixedSystemClock
    private lateinit var watcher: BackgroundWatcher
    private lateinit var prefs: SharedPreferences
    private lateinit var errorReporter: ErrorReporter

    @Before
    public override fun setUp() {
        super.setUp()
        clock = FixedSystemClock(MOCK_CURRENT_TIME)

        prefs = PreferenceManager.getDefaultSharedPreferences(mockContext)
        errorReporter = mock(ErrorReporter::class.java)

        watcher = BackgroundWatcher(prefs, clock, errorReporter)
    }


    @Test
    fun `should save last success upload time`() {
        watcher.notifySuccessAutoupload(MOCK_AUTOUPLOADED_FILE_CREATION_DATE)

        assertEquals(MOCK_CURRENT_TIME,
                prefs.getLong(LAST_REPORTED_AUTOUPLOAD_TIME_KEY, -1))
    }

    @Test
    fun `must save autoupload enable time`() {
        watcher.notifyAutouploadSettingEnabled()

        Assert2.assertEquals(MOCK_CURRENT_TIME, prefs.getLong(AUTOUPLOAD_ENABLED_TIME_KEY, -1))
    }

    @Test
    fun `must report backgroung work problems if last success upload was more that a day before`() {
        prefs.edit().putLong(LAST_REPORTED_AUTOUPLOAD_TIME_KEY, MOCK_AUTOUPLOADED_FILE_CREATION_DATE)
                .putLong(AUTOUPLOAD_ENABLED_TIME_KEY, MOCK_DAY_BEFORE_NOW)
                .apply()
        watcher.notifySuccessAutoupload(MOCK_AUTOUPLOADED_FILE_CREATION_DATE)

        verify(errorReporter).logError(eq(ErrorKeys.BACKGROUND_WORK_RESTRICTED_ERROR), any(),
                eq(AnalyticEventKeys.BACKGROUND_WORK_RESTRICTED))
    }

    @Test
    fun `must not report error if autoupload was enabled after photo was created`() {
        prefs.edit().putLong(AUTOUPLOAD_ENABLED_TIME_KEY, TimeUnit.DAYS.toMillis(10))
                .apply()
        watcher.notifySuccessAutoupload(TimeUnit.DAYS.toMillis(9))

        verifyNoMoreInteractions(errorReporter)
    }

    @Test
    fun `must debounce notifies about autoupload`() {
        prefs.edit().putLong(AUTOUPLOAD_ENABLED_TIME_KEY, 1L)
                .putLong(LAST_REPORTED_AUTOUPLOAD_TIME_KEY, 2L)
                .apply()

        watcher.notifySuccessAutoupload(MOCK_AUTOUPLOADED_FILE_CREATION_DATE)
        watcher.notifySuccessAutoupload(MOCK_AUTOUPLOADED_FILE_CREATION_DATE)
        watcher.notifySuccessAutoupload(MOCK_AUTOUPLOADED_FILE_CREATION_DATE)
        watcher.notifySuccessAutoupload(MOCK_AUTOUPLOADED_FILE_CREATION_DATE)

        verify(errorReporter, times(1))
                .logError(eq(ErrorKeys.BACKGROUND_WORK_RESTRICTED_ERROR), any(),
                        eq(AnalyticEventKeys.BACKGROUND_WORK_RESTRICTED))
    }

    @Test
    fun `must not report error if reported autoupload, but have no setting update time`() {
        watcher.notifySuccessAutoupload(MOCK_AUTOUPLOADED_FILE_CREATION_DATE)

        verifyNoMoreInteractions(errorReporter)
    }

    @Test
    fun `must save autoupload setting change time if upload reported, but enable time not saved`() {
        watcher.notifySuccessAutoupload(MOCK_AUTOUPLOADED_FILE_CREATION_DATE)

        assertEquals(MOCK_CURRENT_TIME, prefs.getLong(AUTOUPLOAD_ENABLED_TIME_KEY, 0L))
    }

    @Test
    fun `must not rewrite setting enabled time on autoupload report`() {
        prefs.edit().putLong(AUTOUPLOAD_ENABLED_TIME_KEY, MOCK_DAY_BEFORE_NOW)
                .apply()

        watcher.notifySuccessAutoupload(MOCK_AUTOUPLOADED_FILE_CREATION_DATE)

        assertEquals(MOCK_DAY_BEFORE_NOW, prefs.getLong(AUTOUPLOAD_ENABLED_TIME_KEY, 0L))
    }

    @Test
    fun `should not notify if uploaded photo taken in last 24 hours`() {
        prefs.edit().putLong(AUTOUPLOAD_ENABLED_TIME_KEY, MOCK_DAY_BEFORE_NOW)
            .putLong(LAST_REPORTED_AUTOUPLOAD_TIME_KEY, MOCK_DAY_BEFORE_NOW)
            .apply()

        watcher.notifySuccessAutoupload(MOCK_CURRENT_TIME)

        verify(errorReporter, never())
            .logError(eq(ErrorKeys.BACKGROUND_WORK_RESTRICTED_ERROR), any(),
                eq(AnalyticEventKeys.BACKGROUND_WORK_RESTRICTED))
    }
}
