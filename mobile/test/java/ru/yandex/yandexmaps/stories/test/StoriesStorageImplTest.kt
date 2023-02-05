package ru.yandex.yandexmaps.stories.test

import android.app.Application
import android.content.Context.MODE_PRIVATE
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.yandex.yandexmaps.common.preferences.PreferencesFactory
import ru.yandex.yandexmaps.stories.StoriesStorageImpl

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
internal class StoriesStorageImplTest {

    private lateinit var application: Application
    private lateinit var storiesStorageImpl: StoriesStorageImpl

    @Before
    fun setUp() {
        @Suppress("DEPRECATION") // blocked by androidx migration
        application = RuntimeEnvironment.application
        storiesStorageImpl = StoriesStorageImpl(PreferencesFactory(application.getSharedPreferences("testPrefs", MODE_PRIVATE)))
    }

    @Test
    fun testRegionsParsing() {
        storiesStorageImpl.markAsViewed("1", 1)
        storiesStorageImpl.markAsViewed("1", 2)
        storiesStorageImpl.markAsViewed("2", 2)
        storiesStorageImpl.markAsViewed("3", 2)

        Assertions.assertThat(storiesStorageImpl.getViewedInfo().size).isEqualTo(3)
        Assertions.assertThat(storiesStorageImpl.getViewedInfo()["1"]).isEqualTo(2)

        storiesStorageImpl.clearAll()
        Assertions.assertThat(storiesStorageImpl.getViewedInfo().size).isEqualTo(0)
    }
}
